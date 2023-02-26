package com.svwh.mailservice.core;

import com.svwh.mailservice.listener.MailServiceListener;
import com.svwh.mailservice.mail.HtmlMail;
import com.svwh.mailservice.mail.Mail;
import com.svwh.mailservice.mail.MailSender;
import com.svwh.mailservice.mail.TextMail;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;

import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @description
 * @Author cxk
 */
public abstract class BaseMailService implements MailService{

    private final org.slf4j.Logger Logger = LoggerFactory.getLogger(BaseMailService.class);

    /**
     * 发出错误预警的时间戳，防止多线程情况下短时间内频繁报警
     */
    private long errorTriggerSleepTime = System.currentTimeMillis();

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 邮件服务监听器
     */
    private MailServiceListener mailServiceListener;

    @Override
    public abstract boolean send(Mail mail);


    protected void doSendMail(Mail mail,MailSender mailSender) throws AddressException,EmailException {
        if (mail instanceof HtmlMail){
            sendHtmlMail(mailSender,mail);
        } else if (mail instanceof TextMail) {
            sendTextMail(mailSender,mail);
        }
    }

    private void sendHtmlMail(MailSender mailSender,Mail mail) throws EmailException {
            HtmlEmail email = new HtmlEmail();
            email.setHostName(mailSender.getHostName());
            email.setAuthenticator(new DefaultAuthenticator(mailSender.getUsername(), mailSender.getPassword()));
            email.setSSLOnConnect(mailSender.getSslEnable());
            email.setCharset(mailSender.getDefaultEncoding());
            email.setFrom(mailSender.getFromSender());
            email.setSubject(mail.getSubject());
            email.setHtmlMsg(mail.getContent());
            for (String address : mail.getToMail()) {
                email.addTo(address);
            }
            email.send();
    }

    private void sendTextMail(MailSender mailSender,Mail mail) throws EmailException {
            SimpleEmail email = new SimpleEmail();
            email.setHostName(mailSender.getHostName());
            email.setAuthenticator(new DefaultAuthenticator(mailSender.getUsername(), mailSender.getPassword()));
            email.setSSLOnConnect(mailSender.getSslEnable());
            email.setFrom(mailSender.getUsername());
            email.setContent(mail.getContent(), "text/plain;charset=UTF-8");
            email.setSubject(mail.getSubject());
            for (String address : mail.getToMail()) {
                email.addTo(address);
            }
            email.send();
    }

    @Override
    public void setErrorListener(MailServiceListener mailServiceListener) {
        this.mailServiceListener = mailServiceListener;
    }

    /**
     * 邮箱服务预警，当服务不可用时所触发的动作（包括所有邮箱账号不可用）
     */
    protected void errorTrigger(){
        long currentTime = System.currentTimeMillis();
         // 在判断当前时间是否满足的时候加锁防止多线程情况下
         // 导致同时进行判断从而短时间内发出多次预警
        lock.lock();
        try {
            if (currentTime - errorTriggerSleepTime > 30000){
                errorTriggerSleepTime = currentTime;
                Logger.warn("邮箱服务资源即将耗尽!");
                if (mailServiceListener != null){
                    mailServiceListener.errorListener();
                }
            }
        }finally {
            lock.unlock();
        }

    }
}
