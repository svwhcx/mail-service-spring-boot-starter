package com.svwh.mailservice.conf;

import com.svwh.mailservice.algrothim.CountRateLimit;
import com.svwh.mailservice.algrothim.CountRateLimitExecutor;
import com.svwh.mailservice.algrothim.RateLimitExecutor;
import com.svwh.mailservice.algrothim.RefreshableRateLimitExecutor;
import com.svwh.mailservice.core.DefaultMailListener;
import com.svwh.mailservice.core.StandAloneMailService;
import com.svwh.mailservice.core.MailService;
import com.svwh.mailservice.enums.CountRateLimitEnum;
import com.svwh.mailservice.listener.MailServiceListener;
import com.svwh.mailservice.mail.MailSender;
import com.svwh.mailservice.threadpool.DefaultThreadPoolExecutor;
import com.svwh.mailservice.threadpool.MailThreadPoolFactory;
import com.svwh.mailservice.threadpool.MailTooManyRejectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @description 自动注入配置启动器
 * @Author cxk
 */
@Configuration
@EnableConfigurationProperties(value = {MailProperties.class, MailServiceProperties.class})
public class MailSenderAutoConfiguration {

    private final Logger LOGGER = LoggerFactory.getLogger(MailSenderAutoConfiguration.class);

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new MailThreadPoolFactory();
    private static final RejectedExecutionHandler DEFAULT_REJECT_HANDLER = new MailTooManyRejectStrategy();



    /**
     * 默认的限流等级集合。
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitRankConf rateLimitRankConf(){
        RateLimitRankConf rateLimitRankConf = new RateLimitRankConf();
        for (CountRateLimitEnum value : CountRateLimitEnum.values()) {
            rateLimitRankConf.getRateLimitList().add(new CountRateLimit(value));
        }
        LOGGER.debug("默认的限流等级已生效！");
        return rateLimitRankConf;
    }


    /**
     * 配置默认的限流执行器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(value = {RateLimitRankConf.class})
    @ConditionalOnClass(value = {MailProperties.class})
    public RateLimitExecutor rateLimitExecutor(MailProperties mailProperties,
                                               RateLimitRankConf rateLimitRankConf){
        // 配置默认的限流器
        RateLimitExecutor rateLimitExecutor;
        if (mailProperties.isEnableRefresh()){
            rateLimitExecutor = new RefreshableRateLimitExecutor(mailProperties,rateLimitRankConf);
            LOGGER.debug("动态刷新限流器已生效！");
        }else{
            rateLimitExecutor = new CountRateLimitExecutor(mailProperties,rateLimitRankConf);
        }
        LOGGER.debug("默认的限流器已生效！");
        return  rateLimitExecutor;
    }

    /**
     * 配置默认的线程池操作，使用Bean注入。
     * 防止和其他的线程池出现冲突，这里采用自定义的注入类
     * 使用时再获取即可。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(value = {MailProperties.class, MailServiceProperties.class})
    public DefaultThreadPoolExecutor defaultThreadPoolExecutor(MailServiceProperties mailServiceProperties){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(mailServiceProperties.getCorePoolSize(),
                mailServiceProperties.getMaxPoolSize(), mailServiceProperties.getKeepAliveTime(),
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(mailServiceProperties.getMaxWorkCount()),
                DEFAULT_THREAD_FACTORY, DEFAULT_REJECT_HANDLER);
        return new DefaultThreadPoolExecutor(threadPoolExecutor);
    }

    /**
     * 默认的邮箱服务监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public MailServiceListener mailServiceListener(){
        return new DefaultMailListener();
    }

    /**
     * 返回一个默认的邮件发送器
     */
    @Bean
    @ConditionalOnClass(value = {MailProperties.class, MailServiceProperties.class})
    @ConditionalOnBean(value = {MailServiceListener.class, RateLimitExecutor.class,
            DefaultThreadPoolExecutor.class, MailServiceListener.class})
    @ConditionalOnMissingBean
    public MailService mailService(MailProperties mailProperties,
                                   MailServiceProperties mailServiceProperties,
                                   RateLimitExecutor rateLimitExecutor,
                                   DefaultThreadPoolExecutor defaultThreadPoolExecutor,
                                   MailServiceListener mailServiceListener){
        // 默认为BaseMailService
        for (MailSender mailInfo : mailProperties.getMailInfos()) {
            if (mailInfo.getStartLimitTime() != 0L){
                LOGGER.error("The mail service start failed ! the startLimitTime shouldn't  be set");
            }
        }
        MailService mailService = new StandAloneMailService(mailProperties, mailServiceProperties,rateLimitExecutor,
                defaultThreadPoolExecutor.getThreadPoolExecutor());
        mailService.setMailListener(mailServiceListener);
        return mailService;
    }

}
