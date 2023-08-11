package com.svwh.mailservice.listener;


import com.svwh.mailservice.mail.Mail;
import com.svwh.mailservice.mail.MailSender;

/**
 * @description 邮件服务监听器
 * @Author cxk
 */
public interface MailServiceListener {

    /**
     * 错误事件监听器（命名暂时容易产生误解）
     */
    void errorListener();

    /**
     * 发送邮件成功的监听。
     * @param mail 邮件数据
     * @param mailSender 邮件发送账号。
     */
    void successListener(Mail mail,MailSender mailSender);
}
