package com.svwh.mailservice.conf;


import com.svwh.mailservice.mail.MailSender;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.List;

/**
 * @description
 * @Author cxk
 */
@ConfigurationProperties(prefix = "mail-service.accounts")
@RefreshScope
public class MailProperties {

    /**
     * 发送邮件地址配置
     */
    private List<MailSender> mailInfos;

    public List<MailSender> getMailInfos() {
        return mailInfos;
    }

    private boolean enableRefresh = true;

    public boolean isEnableRefresh() {
        return enableRefresh;
    }

    public void setEnableRefresh(boolean enableRefresh) {
        this.enableRefresh = enableRefresh;
    }

    public void setMailInfos(List<MailSender> mailInfos) {
        this.mailInfos = mailInfos;
    }
}
