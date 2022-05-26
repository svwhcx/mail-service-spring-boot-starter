package com.study.mailsender.core;

import com.study.mailsender.mail.Mail;
/**
 * @description
 * @Author cxk
 * @Date 2022/5/11 22:35
 */
public interface MailService {

    boolean sendMail(Mail mail);

}

