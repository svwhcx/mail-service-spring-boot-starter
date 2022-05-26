package com.study.mailsender.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description 用户可自定义线程池的配置（发挥最大性能）
 * @Author cxk
 * @Date 2022/5/11 22:03
 */
@ConfigurationProperties(prefix = "mailpool")
public class MailThreadPoolProperties {


    /**
     * 发送邮件线程池核心线程数
     */
    private int corePoolSize = 5;

    /**
     * 发送邮件线程池最大线程数
     */
    private int maxPoolSize = 10;

    /**
     * 发送邮件线程池线程最大存货时间
     */
    private int keepAliveTime = 3;

    /**
     * 最大发送邮件任务个数
     */
    private int maxWorkCount = Integer.MAX_VALUE;


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
}
