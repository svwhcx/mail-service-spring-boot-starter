package com.svwh.mailservice.mail;


/**
 * @description
 * @Author cxk
 */
public class MailSender {

    /**
     * 邮件服务器的SMTP地址
     */
    private String hostName;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 发送邮件端口号
     */
    private int port = 465;

    /**
     * 邮件发件人
     */
    private String fromSender;

    /**
     * 使用STARTTLS安全连接
     */
    private Boolean starttlsEnable = true;

    /**
     * 开启ssl 安全连接（默认为true）
     */
    private Boolean sslEnable = true;

    /**
     * 默认的编码方式
     */
    private String defaultEncoding = "UTF-8";

    /**
     * 规定的连接的超时时间 (毫秒）
     */
    private Integer timeout = 3000;

    /**
     * 邮箱号可发送等级（1 - 14）默认低级
     */
    private Integer senderRank = 9;

    /**
     * 对该账号进行发送限制（不必配置此选项）,(调整可见性）
     */
    private volatile boolean isLimited;

    /**
     * 开始禁止时间(不必配置此选项)
     */
    private long startLimitTime;

    public long getStartLimitTime() {
        return startLimitTime;
    }

    public void setStartLimitTime(long startLimitTime) {
        this.startLimitTime = startLimitTime;
    }

    public boolean isLimited() {
        return isLimited;
    }

    public void setLimited(boolean limited) {
        isLimited = limited;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFromSender() {
        return fromSender;
    }

    public void setFromSender(String fromSender) {
        this.fromSender = fromSender;
    }

    public Boolean getStarttlsEnable() {
        return starttlsEnable;
    }

    public void setStarttlsEnable(Boolean starttlsEnable) {
        this.starttlsEnable = starttlsEnable;
    }

    public Boolean getSslEnable() {
        return sslEnable;
    }

    public void setSslEnable(Boolean sslEnable) {
        this.sslEnable = sslEnable;
    }

    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getSenderRank() {
        return senderRank;
    }

    public void setSenderRank(Integer senderRank) {
        this.senderRank = senderRank;
    }

    @Override
    public String toString() {
        return "MailSender{" +
                "hostName='" + hostName + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", port=" + port +
                ", fromSender='" + fromSender + '\'' +
                ", starttlsEnable=" + starttlsEnable +
                ", sslEnable=" + sslEnable +
                ", defaultEncoding='" + defaultEncoding + '\'' +
                ", timeout=" + timeout +
                ", senderRank=" + senderRank +
                ", isLimited=" + isLimited +
                ", startLimitTime=" + startLimitTime +
                '}';
    }
}
