package com.svwh.mailservice.algrothim;


import com.svwh.mailservice.enums.CountRateLimitEnum;
import com.svwh.mailservice.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description 计数器限流算法进行限流。
 * @Author cxk
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

    public CountRateLimit(CountRateLimitEnum countRateLimitEnum){
        this.accessTimeUnit = countRateLimitEnum.timeUnit;
        this.timeLimit = countRateLimitEnum.timeLimit;
        this.accessCountLimit = countRateLimitEnum.accessCountLimit;
    }

    /**
     * 尝试访问，并发情况下使用CAS + 自旋的方式尝试获取发送邮件所需的条件
     * 直接使用自旋锁而不用互斥锁为了节约资源（获取锁的时间很短）
     * @return 是否成功获得发送邮件的访问权
     */
    @Override
    public boolean tryAccess(MailSender mailSender){
        long now = System.currentTimeMillis();
        // 判断是否在单位时间内
        if (now < accessTimeUnit.toMillis(timeLimit) + startTime.get()){
            if (accessCount.get() < accessCountLimit){
                int aCount;
                do {
                    aCount = accessCount.get();
                    if (aCount > accessCountLimit){
                        rateLimitOccur(mailSender);
                        return false;
                    }
                }while (aCount<accessCountLimit&&
                        !accessCount.compareAndSet(accessCount.get(),accessCount.get()+1));
                return true;
            }
            rateLimitOccur(mailSender);
            return false;
        }
        // 尝试重置时间窗口
        return resetTimeWindow(now);
    }

    /**
     * 重置时间窗口
     * @return 重置是否成功
     */
    private boolean resetTimeWindow(long now){
        if (startTime.compareAndSet(startTime.get(), now)){
            accessCount = ZERO;
            accessCount.incrementAndGet();
            return true;
        }
        return false;
    }

    private void rateLimitOccur(MailSender mailSender){
        LOGGER.warn("账号：{}发送频率达到限制！",mailSender.getFromSender());
    }




}
