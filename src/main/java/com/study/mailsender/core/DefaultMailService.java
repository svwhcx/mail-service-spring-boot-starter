package com.study.mailsender.core;

import com.study.mailsender.algrothim.CountRateLimit;
import com.study.mailsender.algrothim.RateLimit;
import com.study.mailsender.conf.MailProperties;
import com.study.mailsender.conf.MailThreadPoolProperties;
import com.study.mailsender.mail.Mail;

import com.study.mailsender.mail.MailSender;
import com.study.mailsender.util.ParamAssert;

import org.apache.commons.mail.EmailException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * @description 当邮件发送迅速的时候会导致邮件服务不可用，为此，故要实现一个限流保证邮件服务能正常发送邮件，将邮件先保存，待服务正常后再发送。
 * 利用生产者消费者模式来重发重要的邮件
 * TODO 适配动态配置刷新更新发送邮件账号集
 * @Author cxk
 * @Date 2022/5/16 18:51
 */
public class DefaultMailService extends BaseMailService {

    private static final Logger Logger = LoggerFactory.getLogger(DefaultMailService.class);

    /**
     * 为每个邮箱设置不同的限流算法。
     */
    private final HashMap<Integer, RateLimit> rateLimitMap = new HashMap<>(16);

    /**
     * 邮箱账号集
     */
    private static List<MailSender> MAIL_SENDERS;

    private final ConcurrentHashMap<MailSender, RateLimit> mailSenderRateLimitMap;

    private final ExecutorService threadPoolExecutor;

    LinkedBlockingQueue<Mail> mailQueue = new LinkedBlockingQueue<>();

    /**
     * 用于测试成功发送了多少封邮件
     */
    private final LongAdder longAdder = new LongAdder();

    public DefaultMailService(MailProperties mailProperties, MailThreadPoolProperties mailThreadPoolProperties) {
        init();
        // 默认拒绝策略
        RejectedExecutionHandler DEFAULT_REJECT_HANDLER = new MailTooManyRejectStrategy();
        // 默认线程工厂
        ThreadFactory DEFAULT_THREAD_FACTORY = new MailThreadPoolFactory();
        // 配置线程池
        this.threadPoolExecutor = new ThreadPoolExecutor(mailThreadPoolProperties.getCorePoolSize(),
                mailThreadPoolProperties.getMaxPoolSize(), mailThreadPoolProperties.getKeepAliveTime(),
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(mailThreadPoolProperties.getMaxWorkCount()),
                DEFAULT_THREAD_FACTORY, DEFAULT_REJECT_HANDLER);
        MAIL_SENDERS = mailProperties.getMailInfos();
        // 根据发送邮箱信息配置不同的限流等级
        mailSenderRateLimitMap = new ConcurrentHashMap<>(MAIL_SENDERS.size());
        MAIL_SENDERS.forEach(mailSender -> mailSenderRateLimitMap.put(mailSender, rateLimitMap
                .get(mailSender.getSenderRank())));
        // 自启动缓冲区消费者
        consumer();
        // 调整邮箱账号等级
        adjustMailSenderGrade();
    }


    /**
     * 初始化操作 分配不同的限流等级
     */
    private void init() {
        rateLimitMap.put(1, CountRateLimit.CountRateLimitEnum.one.getRateLimit());
        rateLimitMap.put(2, CountRateLimit.CountRateLimitEnum.two.getRateLimit());
        rateLimitMap.put(3, CountRateLimit.CountRateLimitEnum.three.getRateLimit());
        rateLimitMap.put(4, CountRateLimit.CountRateLimitEnum.four.getRateLimit());
        rateLimitMap.put(5, CountRateLimit.CountRateLimitEnum.five.getRateLimit());
        rateLimitMap.put(6, CountRateLimit.CountRateLimitEnum.six.getRateLimit());
        rateLimitMap.put(7, CountRateLimit.CountRateLimitEnum.seven.getRateLimit());
        rateLimitMap.put(8, CountRateLimit.CountRateLimitEnum.eight.getRateLimit());
        rateLimitMap.put(9, CountRateLimit.CountRateLimitEnum.nine.getRateLimit());
        rateLimitMap.put(10, CountRateLimit.CountRateLimitEnum.ten.getRateLimit());
        rateLimitMap.put(11, CountRateLimit.CountRateLimitEnum.eleven.getRateLimit());
    }

    @Override
    public boolean sendMail(Mail mail) {
        checkParameter(mail);
        // 处理服务限流，如果有必须到的任务则必须发送到而不能被限流给抛弃,而是服务正常后继续进行发送
        threadPoolExecutor.execute(() -> {
            for (MailSender mailSender : MAIL_SENDERS) {
                if (mailSender.isLimited()) {
                    // 尝试对邮箱账号解封
                    tryRemoveLimit(mailSender);
                    continue;
                }
                RateLimit rateLimit = mailSenderRateLimitMap.get(mailSender);
                if (rateLimit.tryAccess(mailSender)) {
                    try {
                        doSendMail(mail, mailSender);
                        longAdder.increment();
                        Logger.info("成功发送邮件    {}    封", longAdder.sum());
                        return;
                    } catch (EmailException e) {
                        adjustmentMailSender(mailSender);
                        retrySendEmail(mail);
                        Logger.error("=============发送邮件账号不可用=============={}", mailSender);
                    }
                    break;
                }
            }
            retrySendEmail(mail);
        });
        return true;
    }

    /**
     * 对发送邮件账号等级进行调整
     *
     * @param mailSender 发送邮件账号
     */
    private void adjustmentMailSender(MailSender mailSender) {
        // 对邮箱账号进行限流,被限制了不必再次限制和调整等级
        if (!mailSender.isLimited()) {
            mailSender.setLimited(true);
            mailSender.setStartLimitTime(System.currentTimeMillis());
            // 账号等级降级
            Integer senderRank = mailSender.getSenderRank();
            if (senderRank > 0) {
                mailSender.setSenderRank(senderRank);
                RateLimit rateLimit1 = rateLimitMap.get(mailSender.getSenderRank());
                mailSenderRateLimitMap.put(mailSender, rateLimit1);
            }
        }
    }

    /**
     * 尝试解封邮箱账号 （每个账号默认封10分钟）
     *
     * @param mailSender 发送邮件账号
     */
    private void tryRemoveLimit(MailSender mailSender) {
        long now = System.currentTimeMillis();
        if (now > mailSender.getStartLimitTime() + TimeUnit.MINUTES.toMillis(10)) {
            mailSender.setLimited(false);
        }
    }

    /**
     * 尝试重发邮件
     *
     * @param mail 待发送邮件
     */
    private void retrySendEmail(Mail mail) {
        // 如果邮件必须送达则加入待发送邮件缓冲池等待消费。
        if (mail.getStrictArrive()) {
            producer(mail);
        }
    }

    /**
     * 校验待发送邮件参数 TODO 如果是发送附件，类型不一样校验不一致
     *
     * @param mail 待发送邮件
     */
    private void checkParameter(Mail mail) {
        ParamAssert.notNull(mail, "the mail  is  null!");
        ParamAssert.stringNotEmpty(mail.getSubject(), "the subject of Mail is Null");
        ParamAssert.stringNotEmpty(mail.getContent(), "the content of Mail if Null");
        ParamAssert.notNull(mail.getToMail(), "the of toMail is Null");
        // 注： 目的邮件判空不在检查范围内，调用方自行判定
    }

    /**
     * 生产者，向队列投递消息 TODO 直接线程池的处理不是很好,可能会阻塞发送邮件的线程，线程池中的线程被全部拿来处理入队操作
     */
    private void producer(Mail mail) {
        threadPoolExecutor.execute(() -> {
            try {
                mailQueue.put(mail);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * 每隔一天对邮箱账号发邮件等级进行升级(每次升级一个等级）
     */
    private void adjustMailSenderGrade() {
        new Thread(() -> {
            try {
                // 调整时间为0:00唤醒
                long now = System.currentTimeMillis();
                long sleep = 86400 - now % 86400;
                TimeUnit.MILLISECONDS.sleep(sleep);
                // 每天任务
                while (true) {
                    for (MailSender mailSender : MAIL_SENDERS) {
                        Integer senderRank = mailSender.getSenderRank();
                        if (senderRank < 11) {
                            mailSender.setSenderRank(senderRank + 1);
                            RateLimit rateLimit = rateLimitMap.get(mailSender.getSenderRank());
                            mailSenderRateLimitMap.put(mailSender, rateLimit);
                        }
                    }
                    TimeUnit.DAYS.sleep(1);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }).start();
    }

    /**
     * 消费者，从队列拿消息进行消费，限流就是在这里进行处理
     */
    private void consumer() {
        new Thread(() -> {
            while (true) {
                try {
                    Mail mail = mailQueue.take();
                    Logger.info("开始尝试发送邮件> {}", mail);
                    Logger.info("当前待发送邮件个数为  >> {}", mailQueue.size());
                    sendMail(mail);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();

    }


    /**
     * 定义默认的拒绝策略
     * 可打印出当前线程池中的邮件任务数量
     * 同时丢弃未处理的任务。
     */
    static class MailTooManyRejectStrategy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Logger.error("=================邮件系统发生了故障================");
            Logger.error("=================邮件个数超过设定限制==================");
            Logger.error("邮件系统中待发送的邮件个数为:   {}", executor.getQueue().size());
            Logger.error("===============================================");
        }

    }

    /**
     * 自定义线程池工厂
     */
    static class MailThreadPoolFactory implements ThreadFactory {

        /**
         * 线程组
         */
        private final ThreadGroup mailThreadGroup;

        /**
         * 线程组个数
         */
        private static final AtomicInteger poolNum = new AtomicInteger(1);
        /**
         * 线程数量
         */
        private final AtomicInteger threadNum = new AtomicInteger(1);

        /**
         * 线程前缀名
         */
        private final String threadPrefix;

        MailThreadPoolFactory() {
            mailThreadGroup = Thread.currentThread().getThreadGroup();
            threadPrefix = "mail-pool-" + poolNum.getAndIncrement() + "-mail-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(mailThreadGroup, r, threadPrefix + threadNum.getAndIncrement(), 0);
            if (thread.isDaemon())
                thread.setDaemon(false);
            if (thread.getPriority() != Thread.NORM_PRIORITY)
                thread.setPriority(Thread.NORM_PRIORITY);
            // 设置异常拦截处理
            thread.setUncaughtExceptionHandler((thread1, throwable) -> {
                Logger.error("===================发送邮件服务出现异常==================");
                Logger.error("发生异常的线程：  {}", thread1.getName());
                Logger.error("发送邮件失败原因： {}", throwable.getMessage());
                Logger.error("具体的原因为    ", throwable);
                Logger.error("=====================================================");
                // 线程崩溃，保证线程数量和命名一致。
                threadNum.decrementAndGet();
            });
            return thread;
        }

    }

}
