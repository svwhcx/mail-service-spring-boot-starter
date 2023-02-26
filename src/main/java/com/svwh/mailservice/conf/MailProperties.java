package com.svwh.mailservice.conf;


import com.svwh.mailservice.mail.MailSender;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @description
 * @Author cxk
 */
@ConfigurationProperties(prefix = "mail-service")
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
