package com.svwh.mailservice.algrothim;

import com.svwh.mailservice.conf.MailProperties;
import com.svwh.mailservice.conf.RateLimitRankConf;
import com.svwh.mailservice.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @description  限流器在服务时,实现配置的自动刷新.
 * @Author cxk
 */
public class RefreshableRateLimitExecutor extends CountRateLimitExecutor implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(RefreshableRateLimitExecutor.class);

    // 采用读写锁提高性能
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private ApplicationContext applicationContext;



    public RefreshableRateLimitExecutor(MailProperties mailProperties, RateLimitRankConf rateLimitRankLists) {
        super(mailProperties, rateLimitRankLists);
    }

    @Override
    public MailSender availableAccount() {
        readWriteLock.readLock().lock();
        MailSender mailSender =  super.availableAccount();
        readWriteLock.readLock().unlock();
        return mailSender;
    }


    /**
     * @Description: 配置环境发生变化触发
     * 这里采用注解而不是接口的方式是因为
     * 动态配置在刷新后执行款的刷新操作也是通过实现接口的方式
     * 那么监听器的顺序就不可控，因此这里采用注解的方式进行
     */
    @EventListener
    public void refresh(EnvironmentChangeEvent event){
        for (String key : event.getKeys()) {
            if (key.contains("mail-service")){
                readWriteLock.writeLock().lock();
                updateProperty();
                readWriteLock.writeLock().unlock();
                break;
            }
        }
    }

    /**
     * 动态更新邮件账号的数据
     * 思路：找到已有的账号并保存等级，然后将新德吉账号集合直接对老的账号集合进行替换
     *      然后将限流映射重置后恢复之前账号的限流等级
     * 之所以这样处理的方式是因为限流等级已经经过了多次发送的调整，改了后有可能打破之前的平衡状态。
     */
    private void updateProperty(){
        MailProperties mailProperties = applicationContext.getBean(MailProperties.class);
        List<MailSender> mailInfos = mailProperties.getMailInfos();
        // 将之前的邮件发送账号等级事先保存起来。
        List<MailSender> mailSenders = new ArrayList<>();
        List<RateLimit> cRateLimits = new ArrayList<>();
        for (MailSender mailInfo : mailInfos) {
            for (MailSender mailSender : this.mailSenders) {
                if (mailInfo.getUsername().equals(mailSender.getUsername())){
                    if (mailInfo.getSenderRank().equals(mailSender.getSenderRank())){
                        mailSenders.add(mailSender);
                        cRateLimits.add(mailSenderRateLimitMap.get(mailSender));
                    }
                    break;
                }
            }
        }
        this.mailSenders.clear();
        this.mailSenders.addAll(mailInfos);
        reloadRateLimitMap(mailSenders,cRateLimits);
        logger.debug("邮件账号信息自动更新成功！");
    }

    /**
     * 重新加载限流策略映射
     */
    private void reloadRateLimitMap(List<MailSender> mailSenders, List<RateLimit> cRateLimits){
        mailSenderRateLimitMap.clear();
        initRateLimitInfo();
        for (int i = 0; i < mailSenders.size(); i++) {
            mailSenderRateLimitMap.put(mailSenders.get(i),cRateLimits.get(i));
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


}
