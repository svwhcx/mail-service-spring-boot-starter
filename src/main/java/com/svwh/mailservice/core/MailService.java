package com.svwh.mailservice.core;

import com.svwh.mailservice.listener.MailServiceListener;
import com.svwh.mailservice.mail.Mail;
/**
 * @description
 * @Author cxk
 */
public interface MailService {

    /**
     * 开启服务
     */
    void start();

    /**
     * 发送邮件服务
     * @param mail 邮件任务
     * @return 发送邮件是否成功
     */
    boolean send(Mail mail);

    /**
     * 关闭整个发送邮件的任务
     */
    void closeService();


    /**
     * 当前服务等待发送邮件个数
     * @return 待发送邮件数目
     */
    long awaitSendNum();

    /**
     * 设置邮箱服务监听器
     * @param mailServiceListener 邮件服务监听器
     */
    void setMailListener(MailServiceListener mailServiceListener);

}

