package com.svwh.mailservice.core;

import com.svwh.mailservice.algrothim.CountRateLimit;
import com.svwh.mailservice.algrothim.RateLimit;
import com.svwh.mailservice.conf.MailProperties;
import com.svwh.mailservice.conf.MailServiceProperties;
import com.svwh.mailservice.mail.Mail;

import com.svwh.mailservice.mail.MailSender;
import com.svwh.mailservice.threadpool.MailThreadPoolFactory;
import com.svwh.mailservice.threadpool.MailTooManyRejectStrategy;
import com.svwh.mailservice.util.ParamAssert;

import org.apache.commons.mail.EmailException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * @description
 *      本策略只适合单体情况下的服务
 *
 *      当邮件发送过快的时候会导致邮箱账号发送频率被限制而导致邮件服务不可用
 *      为此，故要实现一个降级、限流保证邮件服务能正常发送邮件
 *      将邮件先保存，待服务正常后再发送。
 *      利用生产者消费者模式来重发重要的邮件
 *      本服务实现了两个维度的限流模式
 *          1、如果所有邮件不可用采取不同的限流和对应的降级策略并并报警
 *          2、如果邮件数目已经达到了最大数目采取对应的降级策略
 * TODO:
 *  1、适配动态配置刷新更新发送邮件账号集
 *  2、合理采用设计模式来增强可扩展性
 * @Author cxk
 */
public class StandAloneMailService extends BaseMailService {

    private static final Logger Logger = LoggerFactory.getLogger(StandAloneMailService.class);

    /**
     * 用于记录成功发送了多少封邮件
     */
    private final LongAdder successCountAdder = new LongAdder();

    /**
     * 最大可发送邮件任务的数量（可被严格模式打破）
     */
    private final AtomicInteger maxMailTaskNum;

    /**
     * 邮箱账号集
     */
    private static List<MailSender> mailSenders;

    /**
     * 邮件严格到达队列消费者线程
     */
    private Thread consumerMailThread;

    /**
     * 定时升级邮箱账号等级线程
     */
    private Thread upGradeThread;

    /**
     * 当所有邮箱账号不可用时消费者线程需要睡眠的时间
     */
    private final long threadSleepTime;

    /**
     * 限流配置策略
     */
    private final ConcurrentHashMap<MailSender, RateLimit> mailSenderRateLimitMap;

    /**
     * 线程池
     */
    private final ExecutorService threadPoolExecutor;

    /**
     * 存储必达消息的队列，如果队列太大会造成服务OOM
     */
    private final BlockingQueue<Mail> mailQueue = new LinkedBlockingQueue<>(5000);

    /**
     * 限流策略列表
     */
    private List<RateLimit> rateLimits;


    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new MailThreadPoolFactory();
    private static final RejectedExecutionHandler DEFAULT_REJECT_HANDLER = new MailTooManyRejectStrategy();



    public StandAloneMailService(MailProperties mailProperties,
                                 MailServiceProperties mailServiceProperties) {
        this(mailProperties, mailServiceProperties,null,
                DEFAULT_THREAD_FACTORY,DEFAULT_REJECT_HANDLER);
    }

    public StandAloneMailService(MailProperties mailProperties,
                                 MailServiceProperties mailServiceProperties,
                                 List<RateLimit> rateLimits) {
        this(mailProperties, mailServiceProperties,rateLimits,
                DEFAULT_THREAD_FACTORY,DEFAULT_REJECT_HANDLER);
    }

    public StandAloneMailService(MailProperties mailProperties,
                                 MailServiceProperties mailServiceProperties,
                                 List<RateLimit> rateLimits,
                                 ThreadFactory threadFactory){
        this(mailProperties, mailServiceProperties,rateLimits,
                threadFactory,DEFAULT_REJECT_HANDLER);
    }

    public StandAloneMailService(MailProperties mailProperties,
                                 MailServiceProperties mailServiceProperties,
                                 RejectedExecutionHandler rejectedExecutionHandler){
        this (mailProperties, mailServiceProperties,null,null,rejectedExecutionHandler);
    }

    public StandAloneMailService(MailProperties mailProperties,
                                 MailServiceProperties mailServiceProperties,
                                 List<RateLimit> rateLimits,
                                 ThreadFactory threadFactory,
                                 RejectedExecutionHandler rejectedExecutionHandler){
        mailSenders = mailProperties.getMailInfos();
        mailSenderRateLimitMap = new ConcurrentHashMap<>();
        this.threadSleepTime = mailServiceProperties.getSleepTime();
        this.rateLimits = rateLimits;
        maxMailTaskNum = new AtomicInteger(mailServiceProperties.getMaxTaskNum());
        // 配置线程池
        this.threadPoolExecutor =
                new ThreadPoolExecutor(mailServiceProperties.getCorePoolSize(),
                        mailServiceProperties.getMaxPoolSize(), mailServiceProperties.getKeepAliveTime(),
                        TimeUnit.SECONDS, new LinkedBlockingQueue<>(mailServiceProperties.getMaxWorkCount()),
                        threadFactory, rejectedExecutionHandler);
        initRateLimitInfo();
        // 自启动缓冲区消费者
        consumer();
        // 调整邮箱账号等级
        adjustMailSenderGrade();
    }


    /**
     * 邮件账号限流对应的初始化操作 分配不同的限流策略和等级
     */
    private void initRateLimitInfo() {
        // 如果没有配置限流等级，自动进行默认配置
        if (this.rateLimits == null){
            configRateLimits();
        }
        int rateLimitsSize = rateLimits.size();
        // 根据发送邮箱信息配置不同的限流等级
        for (MailSender mailSender : mailSenders){
            int rateLimitRank = mailSender.getSenderRank();
            if (rateLimitRank <= 0 || rateLimitRank > rateLimitsSize){
                throw new RuntimeException();
            }
            mailSenderRateLimitMap.put(mailSender,rateLimits.get(rateLimitRank));
        }
    }

    /**
     * 配置默认的限流策略
     */
    private void configRateLimits(){
        this.rateLimits = new ArrayList<>();
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,600));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,400));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,200));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,100));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,70));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,60));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,50));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,30));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,20));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,1L,10));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,5L,40));
        rateLimits.add(new CountRateLimit(TimeUnit.SECONDS,30L,10));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,3L,10));
        rateLimits.add(new CountRateLimit(TimeUnit.MINUTES,10L,20));
    }


    @Override
    public boolean send(Mail mail) {
        // 在外层调用可方便调用端捕捉异常
        checkParameter(mail);
        // 处理邮箱限流，如果有必须到的任务则必须发送到而不能被限流给抛弃,而是服务正常后继续进行发送
        // 如果需要处理服务限流，则添加一个限流策略即可，达到阈值后直接进行限流
        // 如果邮件任务达到了最大数量但邮件必须可达则依然要执行入队
        if (isMaxTaskNum()) {
            // 这里不进行报警触发防止因报警阻塞而造成的业务线程阻塞
            if (mail.getStrictArrive()) {
                sendMail(mail);
                return true;
            }
            return false;
        }

        // 当前邮件任务数量没有到达阈值但所有账号都不可用
        if (isAllLimited()) {
            if (mail.getStrictArrive()) {
                sendMail(mail);
                return true;
            }
            return false;
        }
        sendMail(mail);
        return true;
    }


    private void sendMail(Mail mail) {
        threadPoolExecutor.execute(() -> {
            maxMailTaskNum.decrementAndGet();
            if (isMaxTaskNum()){
                errorTrigger();
                if (mail.getStrictArrive()){
                    producer(mail);
                }
            }
            // 所有的邮箱账号都不可用并且判断邮件是否严格到达
            // 如果所有账号都不可用那么当前线程只负责接收新的请求
            // 而旧的请求是由consumer的一个单独线程来负责的
            if (isAllLimited()) {
                errorTrigger();
                if (mail.getStrictArrive()) {
                    producer(mail);
                }
                return;
            }
            for (MailSender mailSender : mailSenders) {
                // 如果邮箱账号被锁定则尝试解封
                if (mailSender.isLimited()) {
                    tryRemoveLimit(mailSender);
                    continue;
                }
                RateLimit rateLimit = mailSenderRateLimitMap.get(mailSender);
                // 尝试获取执行权限（在限制频率范围内）
                if (rateLimit.tryAccess(mailSender)) {
                    try {
                        doSendMail(mail, mailSender);
                        successCountAdder.increment();
                        maxMailTaskNum.incrementAndGet();
                        return;

                    }catch (AddressException e){
                      // 当发送邮件的目的地址发生错误的时候不需要对服务进行降级
                      Logger.warn("some destination email address {} are illegal !",mail.getToMail());
                    } catch (EmailException e) {
                        maxMailTaskNum.incrementAndGet();
                        adjustmentMailSender(mailSender,true);
                        retrySendEmail(mail);
                        Logger.warn("=============邮箱账号{}不可用==============", mailSender);
                    } catch (Exception e) {
                        Logger.error("=============发送邮件发生了错误！");
                        Logger.error(e.getMessage());
                        maxMailTaskNum.incrementAndGet();
                        retrySendEmail(mail);
                    }
                }else{
                    adjustmentMailSender(mailSender,false);
                }
            }
        });
    }



    /**
     * 判断所有的邮箱是否不可用
     *
     * @return 是否所有的邮箱已经都不可用了
     */
    private boolean isAllLimited() {
        // 为了防止并发问题，直接循环判断邮箱账号列表而不直接用原子类来记录
        // 邮箱账号列表一般不会太多因此不会造成性能损伤
        int limitedCount = 0;
        for (MailSender mailSender : mailSenders){
            if (mailSender.isLimited()){
                limitedCount++;
            }
        }
        return limitedCount == mailSenders.size();
    }

    @Override
    public void closeService() {
        threadPoolExecutor.shutdown();
        consumerMailThread.interrupt();
        upGradeThread.interrupt();
    }

    @Override
    public long successCount(boolean isClean) {
        if (isClean){
            return successCountAdder.sumThenReset();
        }
        return successCountAdder.sum();
    }

    @Override
    public long awaitSendNum() {
        return mailQueue.size();
    }

    /**
     * 校验待发送邮件参数
     * 注： 目的邮件判空不在检查范围内，调用方自行判定
     *
     * @param mail 待发送邮件
     */
    private void checkParameter(Mail mail) {
        ParamAssert.notNull(mail, "the mail  is  null!");
        ParamAssert.stringNotEmpty(mail.getSubject(), "the subject of Mail is Null");
        ParamAssert.stringNotEmpty(mail.getContent(), "the content of Mail if Null");
        ParamAssert.notNull(mail.getToMail(), "the of toMail is Null");
    }

    /**
     * 对发送邮件账号等级进行调整
     *
     * @param mailSender 发送邮件账号
     * @param isDowngrade 是否执行降级操作
     */
    private void adjustmentMailSender(MailSender mailSender,boolean isDowngrade) {
        // 对邮箱账号进行限流,被限制了不必再次限制和调整等级
        if (!mailSender.isLimited()) {
            mailSender.setLimited(true);
            if (isDowngrade){
                mailSender.setStartLimitTime(System.currentTimeMillis());
                // 账号等级降级
                Integer senderRank = mailSender.getSenderRank();
                if (senderRank < rateLimits.size()) {
                    mailSender.setSenderRank(senderRank + 1);
                    RateLimit newRateLimit = rateLimits.get(mailSender.getSenderRank()-1);
                    mailSenderRateLimitMap.put(mailSender, newRateLimit);
                }
            }
        }
    }


    /**
     * 尝试解封邮箱账号 （每个账号默认封10分钟）
     *
     * @param mailSender 发送邮件账号
     */
    private void tryRemoveLimit(MailSender mailSender) {
        long now = System.currentTimeMillis();
        if (now > mailSender.getStartLimitTime() + TimeUnit.MINUTES.toMillis(10)) {
            mailSender.setLimited(false);
        }
    }


    /**
     * 尝试重发邮件
     *
     * @param mail 待发送邮件
     */
    private void retrySendEmail(Mail mail) {
        // 如果邮件必须送达则加入待发送邮件队列中等待消费。
        if (mail.getStrictArrive()) {
            producer(mail);
        }
    }

    /**
     * 定时任务
     * 每隔一天对邮箱账号发邮件等级进行升级(每次升级一个等级）
     */
    private void adjustMailSenderGrade() {
        this.upGradeThread = new Thread(() -> {
            try {
                // 调整时间为凌晨1点唤醒
                long now = System.currentTimeMillis();
                long sleep = 90000000 - now % 86400000;
                TimeUnit.MILLISECONDS.sleep(sleep);
                while (true) {
                    for (MailSender mailSender : mailSenders) {
                        Integer senderRank = mailSender.getSenderRank();
                        if (senderRank > 1) {
                            mailSender.setSenderRank(senderRank - 1);
                            Logger.info("邮箱: {} 升级成功,当前邮箱账号等级为: {}", mailSender.getFromSender(), mailSender.getSenderRank());
                            RateLimit rateLimit = rateLimits.get(mailSender.getSenderRank() - 1);
                            mailSenderRateLimitMap.put(mailSender, rateLimit);
                        }
                    }
                    if (consumerMailThread.isInterrupted()) {
                        break;
                    }
                    TimeUnit.DAYS.sleep(1);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        upGradeThread.start();
    }


    /**
     * 生产者，向队列投递消息
     * 注意：如果严格到达邮件过多可能会导致系统内存不足导致的OOM
     */
    private void producer(Mail mail) {
        try {
            mailQueue.put(mail);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 消费者，消费邮件严格到达队列的消息并在所有账号不可用时进行降级处理
     * 防止服务继续消耗CPU
     */
    private void consumer() {
        // 之所以不直接调用线程池的线程的原因是如果有需要重发的邮件那么该线程会一直阻塞
        this.consumerMailThread = new Thread(() -> {
            while (true) {
                // 当消费者线程得知所有的邮箱不可用时，会强制休眠一段时间
                // 这段时间内不会再次响应严格到达邮件队列中的邮件发送任务
                if (isAllLimited()) {
                    consumerLock();
                }
                try {
                    Mail mail = mailQueue.take();
                    Logger.info("当前待发送邮件个数为  >> {}", mailQueue.size());
                    sendMail(mail);
                    if (consumerMailThread.isInterrupted()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        consumerMailThread.start();
    }

    /**
     * 当所有的邮箱都不可用时触发
     */
    private void consumerLock() {
        errorTrigger();
        try {
            Thread.sleep(threadSleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断是否到达了最大任务数量
     *
     * @return 任务数量是否超过阈值
     */
    private boolean isMaxTaskNum() {
        return maxMailTaskNum.get() < 0;
    }

}
