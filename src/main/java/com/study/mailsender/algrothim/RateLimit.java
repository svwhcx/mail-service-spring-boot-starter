package com.study.mailsender.algrothim;

import com.study.mailsender.mail.MailSender;

/**
 * @description 顶级限流接口(当前已有的限流实现：计数器限流，后期可能会增加更多的限流算法。）
 * @Author cxk
 * @Date 2022/5/26 12:45
 */
public interface RateLimit {


    /**
     * 尝试进行限流访问，并记录哪个邮箱不能访问
     * @param mailSender 待访问邮箱
     * @return 是否能访问。
     */
    boolean tryAccess(MailSender mailSender);

}
