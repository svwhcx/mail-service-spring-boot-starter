package com.svwh.mailservice.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description 用户可自定义线程池的配置（发挥最大性能）
 * @Author cxk
 */
@ConfigurationProperties(prefix = "mail-service-pool")
public class MailServiceProperties {


    /**
     * 发送邮件线程池核心线程数
     */
    private int corePoolSize = 3;

    /**
     * 发送邮件线程池最大线程数
     */
    private int maxPoolSize = 5;

    /**
     * 发送邮件线程池线程最大存货时间
     */
    private int keepAliveTime = 3;

    /**
     * 最大发送邮件任务个数
     */
    private int maxWorkCount = Integer.MAX_VALUE;

    /**
     * 当所有发件账号不可用时整个发送服务需要挂起的时间
     */
    private long sleepTime = 30000;

    private int maxTaskNum = 5000;


    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getMaxWorkCount() {
        return maxWorkCount;
    }

    public void setMaxWorkCount(int maxWorkCount) {
        this.maxWorkCount = maxWorkCount;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public int getMaxTaskNum() {
        return maxTaskNum;
    }

    public void setMaxTaskNum(int maxTaskNum) {
        this.maxTaskNum = maxTaskNum;
    }
}
