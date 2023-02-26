package com.svwh.mailservice.threadpool;

import com.svwh.mailservice.core.StandAloneMailService;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description 自定义线程池工厂
 * @Author cxk
 */
public class MailThreadPoolFactory implements ThreadFactory {

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(StandAloneMailService.class);

    /**
     * 线程组
     */
    private final ThreadGroup mailThreadGroup;

    /**
     * 线程组个数
     */
    private static final AtomicInteger poolNum = new AtomicInteger(1);
    /**
     * 线程数量
     */
    private final AtomicInteger threadNum = new AtomicInteger(1);

    /**
     * 线程前缀名
     */
    private final String threadPrefix;

    public MailThreadPoolFactory() {
        mailThreadGroup = Thread.currentThread().getThreadGroup();
        threadPrefix = "mail-pool-" + poolNum.getAndIncrement() + "-mail-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(mailThreadGroup, r, threadPrefix + threadNum.getAndIncrement(), 0);
        if (thread.isDaemon())
            thread.setDaemon(false);
        if (thread.getPriority() != Thread.NORM_PRIORITY)
            thread.setPriority(Thread.NORM_PRIORITY);
        // 设置异常拦截处理
        thread.setUncaughtExceptionHandler((thread1, throwable) -> {
            Logger.error("===================发送邮件服务出现异常==================");
            Logger.error("发生异常的线程：  {}", thread1.getName());
            Logger.error("发送邮件失败原因： {}", throwable.getMessage());
            Logger.error("具体的原因为    ", throwable);
            Logger.error("=====================================================");
            // 线程崩溃，保证线程数量和命名一致。
            threadNum.decrementAndGet();
        });
        return thread;
    }

}
