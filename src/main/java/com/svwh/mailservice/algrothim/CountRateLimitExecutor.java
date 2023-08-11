package com.svwh.mailservice.algrothim;

import com.svwh.mailservice.conf.MailProperties;
import com.svwh.mailservice.conf.RateLimitRankConf;
import com.svwh.mailservice.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @description 将具体的限流执行器抽离，一方面有利于邮件服务的使用，另一方面有利于限流策略的随时改进！
 * @Author cxk
 * @Date 2023/8/8 21:07
 */
public class CountRateLimitExecutor implements RateLimitExecutor{

    private static final Logger LOGGER = LoggerFactory.getLogger(CountRateLimitExecutor.class);


    /**
     * 定时升级邮箱账号等级线程
     */
    private Thread upGradeThread;

    /**
     * 限流策略列表
     */
    protected   RateLimitRankConf rateLimitRankLists;

    /**
     * 邮箱账号集
     */
    protected List<MailSender> mailSenders;

    /**
     * 限流配置策略
     */
    protected   ConcurrentHashMap<MailSender, RateLimit> mailSenderRateLimitMap;


    public CountRateLimitExecutor(MailProperties mailProperties,
                                  RateLimitRankConf rateLimitRankLists) {
        new ConcurrentHashMap<>();
        this.mailSenders = mailProperties.getMailInfos();
        this.rateLimitRankLists = rateLimitRankLists;
        this.mailSenderRateLimitMap = new ConcurrentHashMap<>();
    }

    public void close(){
        upGradeThread.interrupt();
    }

    @Override
    public void start() {
        initRateLimitInfo();
        adjustMailSenderGrade();
    }

    /**
     * 获取可用的邮件发送账号
     * @return n返回可用的邮件发送账号
     */
    public MailSender availableAccount(){
        // 随机从一个下标开始。
        int size = mailSenders.size();
        int index = ThreadLocalRandom.current().nextInt(size);
        int pointNum = 0;
        while (pointNum != size){
            MailSender mailSender = mailSenders.get(index);
            // 如果邮箱账号被锁定则尝试解封
            if (mailSender.isLimited()) {
                tryRemoveLimit(mailSender);
                continue;
            }
            RateLimit rateLimit = mailSenderRateLimitMap.get(mailSender);
            // 尝试获取执行权限（在限制频率范围内）
            if (rateLimit.tryAccess(mailSender)) {
                return mailSender;
            }else{
                adjustmentMailSender(mailSender,false);
            }
            if (++index >= size){
                index = 0;
            }
            if (++pointNum >= size){
                break;
            }
        }
        return null;
    }

    @Override
    public void adjustmentMailSender(MailSender mailSender) {
        adjustmentMailSender(mailSender,true);
    }


    /**
     * 尝试解封邮箱账号 （每个账号默认封10分钟）
     *
     * @param mailSender 发送邮件账号
     */
    public void tryRemoveLimit(MailSender mailSender) {
        long now = System.currentTimeMillis();
        if (now > mailSender.getStartLimitTime() + TimeUnit.MINUTES.toMillis(10)) {
            mailSender.setLimited(false);
        }
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
                if (senderRank < rateLimitRankLists.limitListSize()) {
                    mailSender.setSenderRank(senderRank + 1);
                    RateLimit newRateLimit = rateLimitRankLists.rankRateLimit(mailSender.getSenderRank() - 1);
                    mailSenderRateLimitMap.put(mailSender, newRateLimit);
                }
            }
        }
    }

    /**
     * 邮件账号限流对应的初始化操作 分配不同的限流策略和等级
     */
    protected void initRateLimitInfo() {
        // 如果没有配置限流等级，自动进行默认配置
        if (this.rateLimitRankLists == null){
            throw new RuntimeException("自定义的邮箱限流等级不允许为NULL！");
        }
        int rateLimitsSize = rateLimitRankLists.limitListSize();
        // 根据发送邮箱信息配置不同的限流等级
        for (MailSender mailSender : mailSenders){
            int rateLimitRank = mailSender.getSenderRank();
            if (rateLimitRank <= 0 || rateLimitRank > rateLimitsSize){
                throw new RuntimeException("初始限流等级不能 < 0 或者 > 总的限流等级数");
            }
            mailSenderRateLimitMap.put(mailSender,rateLimitRankLists.rankRateLimit(rateLimitRank));
        }
    }

    /**
     * 判断所有的邮箱是否不可用
     *
     * @return 是否所有的邮箱已经都不可用了
     */
    public boolean isAllLimited() {
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
                            LOGGER.info("邮箱账号: {} 升级成功,当前邮箱账号等级为: {}", mailSender.getFromSender(), mailSender.getSenderRank());
                            RateLimit rateLimit = rateLimitRankLists.rankRateLimit(mailSender.getSenderRank() - 1);
                            mailSenderRateLimitMap.put(mailSender, rateLimit);
                        }
                    }
                    if (upGradeThread.isInterrupted()) {
                        break;
                    }
                    TimeUnit.DAYS.sleep(1);
                }
            } catch (InterruptedException e) {
                LOGGER.warn("睡眠线程已终止！");
            }
        });
        upGradeThread.start();
    }

}
