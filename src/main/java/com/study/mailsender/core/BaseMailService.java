package com.study.mailsender.core;

import com.study.mailsender.mail.HtmlMail;
import com.study.mailsender.mail.Mail;
import com.study.mailsender.mail.MailSender;
import com.study.mailsender.mail.TextMail;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;

import org.slf4j.LoggerFactory;

/**
 * @description
 * @Author cxk
 * @Date 2022/5/22 23:02
 */
public abstract class BaseMailService implements MailService{

    private final org.slf4j.Logger Logger = LoggerFactory.getLogger(BaseMailService.class);

    @Override
    public abstract boolean sendMail(Mail mail);


    protected void doSendMail(Mail mail,MailSender mailSender) throws EmailException {
        Logger.info("正在发送 。。。 {}",mail);
        Logger.info("mail  sender 》》》 {}",mailSender);
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

}
