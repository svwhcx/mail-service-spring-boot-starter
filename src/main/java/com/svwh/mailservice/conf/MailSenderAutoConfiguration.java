package com.svwh.mailservice.conf;

import com.svwh.mailservice.core.StandAloneMailService;
import com.svwh.mailservice.core.MailService;
import com.svwh.mailservice.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description 自动注入配置启动器
 * @Author cxk
 */
@Configuration
@EnableConfigurationProperties(value = {MailProperties.class, MailServiceProperties.class})
public class MailSenderAutoConfiguration {

    private final Logger LOGGER = LoggerFactory.getLogger(MailSenderAutoConfiguration.class);

    /**
     * 返回一个默认的邮件发送器
     */
    @Bean
    @ConditionalOnClass(value = {MailProperties.class, MailServiceProperties.class})
    public MailService mailService(MailProperties mailProperties, MailServiceProperties mailServiceProperties){
        // 默认为BaseMailService
        for (MailSender mailInfo : mailProperties.getMailInfos()) {
            if (mailInfo.getStartLimitTime() != 0L){
                LOGGER.error("The mail service start failed ! the startLimitTime shouldn't  be set");
            }
        }
        return new StandAloneMailService(mailProperties, mailServiceProperties);
    }

}
