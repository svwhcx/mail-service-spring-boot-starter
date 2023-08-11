package com.svwh.mailservice.threadpool;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description 邮件服务默认的线程池
 * @Author cxk
 */
public class DefaultThreadPoolExecutor {

    // 默认的线程池执行器
    private final ThreadPoolExecutor threadPoolExecutor;

    public DefaultThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public ThreadPoolExecutor getThreadPoolExecutor(){
        return this.threadPoolExecutor;
    }
}
