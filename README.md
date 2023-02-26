# 邮件发送服务框架MailService

## 1. 出现背景

很多情况下会用邮箱发送一些通知，比如验证码、文件之类的邮件，这个时候很多邮件服务会限制不同账号的发送频率，如果发送频率过高就会将账号锁定一段时间，本发送框架旨在解决高频率下的自动调整邮件的发送频率和邮件服务的降级，防止邮件服务因过多的发送请求而崩溃

## 2. 目前支持

- 配置多账号邮件发送并且配置限流策略
- 账号被限制时发送频率自动降级
- 所有账号不可用时进行服务预警
- 当邮件缓存数量达到指定阈值后进行服务预警

## 3. 待支持

- 动态刷新服务的配置
- 分布式环境下多个邮件发送服务的降级和限流

## 4. 使用

### 1. 导入依赖

当前框架已做成springboot-starter因此直接导入对应的pom依赖即可，由于没有发布到maven中央仓库因此直接使用了jitpack的地址

```xml
<!-- 设置仓库地址 -->
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<!-- 导入依赖 -->
<dependency>
<groupId>com.github.cx2002</groupId>
<artifactId>mail-service-spring-boot-starter</artifactId>
<version>v1.0.0</version>
</dependency>
```

### 2. 使用配置

在配置文件中配置发件账号以及邮件服务的参数信息

- 发件账号的配置前缀 `mail-service.infos`；可配置项

  - `hostName` ：邮件服务器的SMTP地址
  - `username`：用户名
  - `password`：密码
  - `port`：发送邮件端口号
  - `fromSender`：邮件发件人
  - `starttlsEnable`：是否使用STARTTLS安全连接（默认为true）
  - `sslEnable`：开启ssl 安全连接（默认为true）
  - `defaultEncoding`：默认的编码方式
  - `timeout`：规定的连接的超时时间 (毫秒）
  - `senderRank`：邮箱号可发送等级（默认配置了1-14级并且默认为低级）数值越高等级越低发送频率也越低

- 邮件服务的配置前缀 `mail-service-pool`；可配置项有

  - `corePoolSize`：发送邮件线程池核心线程数（默认为3）
  - `maxPoolSize`：发送邮件线程池最大线程数（默认为5）
  - `keepAliveTime`：发送邮件线程池线程最大存活时间
  - `maxWorkCount`：最大发送邮件任务个数（用于线程池的队列中）
  - `sleepTime`：当所有发件账号不可用时整个发送服务需要挂起的时间
  - `maxTaskNum`：邮件服务可发送的最大任务数，该值也叫阈值，服务中缓存的发件数量总数达到该值的时候会挂起整个邮件服务（默认为5000）

- 使用示例：

- ```yaml
  mail-service:
    mail-infos:
      - host-name: smtp.qq.com
        username: username
        password: password
        from-sender: from-sender
        sender-rank: 9
      - host-name: smtp.163.com
        username: username
        password: password
        from-sender: from-sender
        sender-rank: 9
  mail-service-pool:
    core-pool-size: 5
    keep-alive-time: 3000
    sleep-time: 3000
  ```

### 3. 自定义注入配置

1、可以参考一下部分进行动态注入

```java
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
```

其中，StandAloneMailService的构造参数有

```java
public StandAloneMailService(MailProperties mailProperties,
                                 MailServiceProperties mailServiceProperties,
                                 List<RateLimit> rateLimits,
                                 ThreadFactory threadFactory,
                                 RejectedExecutionHandler rejectedExecutionHandler)
```

2、`rateLimits` 是限流等级的list，使用者可定义一系列的限流等级；目前只支持计数器限流算法但使用者可以实现RateLimit接口来扩展自己的限流算法，不过需要注意多个线程竞争一个账号资源时候的线程安全问题，其中默认的包含RateLimit策略包含的参数有

```java
public CountRateLimit(TimeUnit accessTimeUnit, Long timeLimit, int accessCountLimit) {
        this.accessTimeUnit = accessTimeUnit; // 访问时间单元（用于最终转换为毫秒）
        this.timeLimit = timeLimit; // 限制单元数，和访问时间单元结合
        this.accessCountLimit = accessCountLimit; // 访问数量，在一定的单元时间内可发送邮件的数量（用于发送邮件限流）
}
```

3、如果需要自定义服务预警的触发事件可以传入一个实现了MailServiceListener接口的对象，当发生服务预警时则会执行相应的方法

## 5. 使用建议

- 该框架目前只支持在单体环境下使用
- 在使用时如果邮件在严格到达模式下服务存留邮件发送任务过多时需要注意整个服务的OOM问题 