package com.study.mailsender.conf;

import com.study.mailsender.core.DefaultMailService;
import com.study.mailsender.core.MailService;
import com.study.mailsender.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description 自动注入配置启动器
 * @Author cxk
 * @Date 2022/5/11 22:30
 */
@Configuration
@EnableConfigurationProperties(value = {MailProperties.class,MailThreadPoolProperties.class})
public class MailSenderAutoConfiguration {

    private final Logger LOGGER = LoggerFactory.getLogger(MailSenderAutoConfiguration.class);

    /**
     * 肯定是返回一个默认的邮件发送器
     * @param mailProperties
     * @return
     */
    @Bean
    @ConditionalOnClass(value = {MailProperties.class,MailThreadPoolProperties.class})
    public MailService mailService(MailProperties mailProperties,MailThreadPoolProperties mailThreadPoolProperties){
        // 默认为BaseMailService
        for (MailSender mailInfo : mailProperties.getMailInfos()) {
            if (mailInfo.getStartLimitTime() != 0L){
                LOGGER.error("The mail service start failed ! the startLimitTime shouldn't set");
            }
        }
        return new DefaultMailService(mailProperties,mailThreadPoolProperties);
    }

}
