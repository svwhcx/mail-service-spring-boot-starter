package com.study.mailsender.algrothim;


import com.study.mailsender.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description 计数器限流算法进行限流。
 * @Author cxk
 * @Date 2022/5/26 10:55
 */
public class CountRateLimit implements RateLimit{

    private static final Logger LOGGER = LoggerFactory.getLogger(CountRateLimit.class);

    private static final AtomicInteger ZERO = new AtomicInteger(0);

    /**
     * 访问次数默认值
     */
    private AtomicInteger accessCount = ZERO;

    /**
     * 访问时间单元
     */
    private final TimeUnit accessTimeUnit;

    /**
     * 时间限制，配置时间单元
     */
    private final Long timeLimit;

    /**
     * 访问次数限制
     */
    private final int accessCountLimit;

    /**
     * 开始时间
     */
    private  final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

    public CountRateLimit(TimeUnit accessTimeUnit, Long timeLimit, int accessCountLimit) {
        this.accessTimeUnit = accessTimeUnit;
        this.timeLimit = timeLimit;
        this.accessCountLimit = accessCountLimit;
    }

    /**
     * 尝试访问，并发情况下使用CAS + 自旋的方式尝试获取发送邮件条件
     * @return 是否成功获得发送邮件的访问权
     */
    @Override
    public boolean tryAccess(MailSender mailSender){
        long now = System.currentTimeMillis();
        // 判断是否在单位时间内
        if (now < accessTimeUnit.toMillis(timeLimit) + startTime.get()){
            if (accessCount.get() < accessCountLimit){
                int temp;
                do {
                    temp = accessCount.get();
                    if (temp > accessCountLimit){
                        LOGGER.error("===============当前邮箱发送频率超过了限制！===========");
                        LOGGER.error("===============账号为：{}",mailSender.getFromSender());
                        return false;
                    }
                }while (temp<accessCountLimit&&
                        !accessCount.compareAndSet(accessCount.get(),accessCount.get()+1));
                return true;
            }
            LOGGER.error("===============当前邮箱发送频率超过了限制！===========");
            return false;
        }
        // 重置时间窗口
        if (startTime.compareAndSet(startTime.get(), now)){
            accessCount = ZERO;
            accessCount.incrementAndGet();
            return true;
        }
        return false;
    }



    /**
     * 限流模式，级别越高限流范围越广(6级可视为中档）
     */
    public enum CountRateLimitEnum{
        one(TimeUnit.MINUTES,10L,20),
        two(TimeUnit.MINUTES,3L,10),
        three(TimeUnit.SECONDS,30L,10),
        four(TimeUnit.MINUTES,5L,40),
        five(TimeUnit.MINUTES,1L,10),
        six(TimeUnit.MINUTES,1L,20),
        seven(TimeUnit.MINUTES,1L,30),
        eight(TimeUnit.MINUTES,1L,50),
        nine(TimeUnit.MINUTES,1L,60),
        ten(TimeUnit.MINUTES,1L,70),
        eleven(TimeUnit.MINUTES,1L,100);
        private final CountRateLimit countRateLimit;

        CountRateLimitEnum(TimeUnit timeUnit,Long time,int account){
            this.countRateLimit = new CountRateLimit(timeUnit,time,account);
        }

        public CountRateLimit getRateLimit(){
            return this.countRateLimit;
        }
    }
}
