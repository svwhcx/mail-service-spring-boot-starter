package com.svwh.mailservice.threadpool;

import com.svwh.mailservice.core.StandAloneMailService;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description 定义默认的拒绝策略
 *  * 可打印出当前线程池中的邮件任务数量
 *  * 同时丢弃未处理的任务。
 * @Author cxk
 */
public class MailTooManyRejectStrategy implements RejectedExecutionHandler {

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(StandAloneMailService.class);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        Logger.error("=================邮件系统发生了故障================");
        Logger.error("=================邮件个数超过设定限制==============");
        Logger.error("邮件系统中待发送的邮件个数为:   {}", executor.getQueue().size());
        Logger.error("=================邮箱故障抓取结束==================");
    }

}
