package com.study.mailsender.conf;


import com.study.mailsender.mail.MailSender;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @description
 * @Author cxk
 * @Date 2022/5/11 22:00
 */
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /**
     * 发送邮件地址配置
     */
    private List<MailSender> mailInfos;

    public List<MailSender> getMailInfos() {
        return mailInfos;
    }

    public void setMailInfos(List<MailSender> mailInfos) {
        this.mailInfos = mailInfos;
    }
}
