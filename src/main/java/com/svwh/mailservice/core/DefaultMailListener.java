package com.svwh.mailservice.core;

import com.svwh.mailservice.listener.MailServiceListener;
import com.svwh.mailservice.mail.Mail;
import com.svwh.mailservice.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

/**
 * @description 默认的邮件服务监听器
 * @Author cxk
 */
public class DefaultMailListener implements MailServiceListener {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultMailListener.class);

    /**
     * 用于记录成功发送了多少封邮件
     */
    private final LongAdder successCountAdder = new LongAdder();


    @Override
    public void errorListener() {
        LOGGER.warn("邮箱资源即将耗尽！");
    }

    @Override
    public void successListener(Mail mail, MailSender mailSender) {
        successCountAdder.increment();
        LOGGER.debug("邮件发送成功，账号：{}，目的账号：{}；当前已成功发送邮件数量：{}",mailSender.getFromSender()
                ,mail.getToMail(),successCountAdder.sum());
    }

    /**
     * 统计已经成功发送邮件的个数。
     * @param isClean 是否清除状态
     * @return 成功数
     */
    public long successCount(boolean isClean) {
        if (isClean){
            return successCountAdder.sumThenReset();
        }
        return successCountAdder.sum();
    }
}
