package com.svwh.mailservice.algrothim;

import com.svwh.mailservice.mail.MailSender;

/**
 *@Description: 限流策略执行器
 *  用于邮件服务的具体执行，包括邮件资源处理、邮件资源预警
 * @Author cxk
 */
public interface RateLimitExecutor {

    /**
     * 关闭限流器
     */
    void close();

    /**
     * 开启限流器
     */
    void start();

    /**
     * 检查系统邮箱账号可用性
     * @return 是否所有的邮箱账号都不可用
     */
    boolean isAllLimited();

    /**
     * 获取可用的邮件发送账号
     * @return 可用的邮箱账号资源
     */
    MailSender availableAccount();

    /**
     * 调整某个邮箱账号的使用性（即限制发送）。
     */
    void adjustmentMailSender(MailSender mailSender);

    /**
     * 尝试解封某个邮箱账号
     *
     */
    void tryRemoveLimit(MailSender mailSender);
}
