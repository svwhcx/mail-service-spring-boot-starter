package com.svwh.mailservice.core;

import com.svwh.mailservice.algrothim.RateLimitExecutor;
import com.svwh.mailservice.conf.MailProperties;
import com.svwh.mailservice.conf.MailServiceProperties;
import com.svwh.mailservice.mail.Mail;

import com.svwh.mailservice.mail.MailSender;
import com.svwh.mailservice.util.ParamAssert;

import org.apache.commons.mail.EmailException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @description 本策略只适合单体情况下的服务
 *      当邮件发送过快的时候会导致邮箱账号发送频率被限制而导致邮件服务不可用
 *      为此，故要实现一个降级、限流保证邮件服务能正常发送邮件
 *      将邮件先保存，待服务正常后再发送。
 *      利用生产者消费者模式来重发重要的邮件
 *      本服务实现了两个维度的限流模式
 *          1、如果所有邮件不可用采取不同的限流和对应的降级策略并并报警
 *          2、如果邮件数目已经达到了最大数目采取对应的降级策略
 * @Author cxk
 */
public class StandAloneMailService extends BaseMailService {

    private static final Logger Logger = LoggerFactory.getLogger(StandAloneMailService.class);

    /**
     * 默认的邮件队列大小
     */
    private static final int  MAIL_QUEUE_SIZE = 5000;

    /**
     * 最大可发送邮件任务的数量（可被严格模式打破）
     */
    private final AtomicInteger maxMailTaskNum;

    /**
     * 邮件严格到达队列消费者线程
     */
    private Thread consumerMailThread;

    /**
     * 当所有邮箱账号不可用时消费者线程需要睡眠的时间
     */
    private final long threadSleepTime;

    /**
     * 限流执行器
     */
    private RateLimitExecutor rateLimitExecutor;

    /**
     * 线程池
     */
    private final ExecutorService threadPoolExecutor;

    /**
     * 存储必达消息的队列，如果队列太大会造成服务OOM
     */
    private final BlockingQueue<Mail> mailQueue = new LinkedBlockingQueue<>(MAIL_QUEUE_SIZE);


    public StandAloneMailService(MailProperties mailProperties,
                                 MailServiceProperties mailServiceProperties,
                                 RateLimitExecutor rateLimitExecutor,
                                 ThreadPoolExecutor threadPoolExecutor){
        this.threadSleepTime = mailServiceProperties.getSleepTime();
        this.rateLimitExecutor = rateLimitExecutor;
        maxMailTaskNum = new AtomicInteger(mailServiceProperties.getMaxTaskNum());
        // 配置线程池
        this.threadPoolExecutor = threadPoolExecutor;
        // 开启邮件发送服务
        start();
    }


    @Override
    public void start() {
        // 自启动缓冲区消费者
        consumer();
        // 启动限流执行器
        rateLimitExecutor.start();
        Logger.info("邮件发送服务启动成功！...");
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
        if (rateLimitExecutor.isAllLimited()) {
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
            if (rateLimitExecutor.isAllLimited()) {
                errorTrigger();
                if (mail.getStrictArrive()) {
                    producer(mail);
                }
                return;
            }
            MailSender availableMailSender = rateLimitExecutor.availableAccount();
            // 没有可用的邮箱账号（虽然前面做了判断，但是防止线程安全问题再次判断）。
            if (availableMailSender == null){
                retrySendEmail(mail);
                return;
            }
            try{
                doSendMail(mail, availableMailSender);
                maxMailTaskNum.incrementAndGet();
            }catch (AddressException e){
                // 当发送邮件的目的地址发生错误的时候不需要对服务进行降级
                Logger.warn("非法的目的邮箱地址：{}!",mail.getToMail());
            }catch (EmailException e){
                maxMailTaskNum.incrementAndGet();
                rateLimitExecutor.adjustmentMailSender(availableMailSender);
                retrySendEmail(mail);
                Logger.warn("邮箱账号:{} 不可用！", availableMailSender.getUsername());
            }catch (Exception e){
                Logger.error("=============发送邮件发生了错误！===========");
                Logger.error(e.getMessage());
                maxMailTaskNum.incrementAndGet();
                retrySendEmail(mail);
            }
        });
    }


    @Override
    public void closeService() {
        threadPoolExecutor.shutdown();
        consumerMailThread.interrupt();
        rateLimitExecutor.close();
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
                if (rateLimitExecutor.isAllLimited()) {
                    consumerLock();
                }
                try {
                    Mail mail = mailQueue.take();
                    Logger.info("待发送邮件数为: {}", mailQueue.size());
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
