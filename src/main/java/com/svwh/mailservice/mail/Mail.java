package com.svwh.mailservice.mail;

import java.util.List;

/**
 * @description
 * @Author cxk
 */
public class Mail {

    private List<String> toMail;

    private String subject;

    private String content;

    /**
     * 是否严格到达（可丢弃：验证码）,默认丢弃
     */
    private Boolean strictArrive = false;

    public List<String> getToMail() {
        return toMail;
    }

    public void setToMail(List<String> toMail) {
        this.toMail = toMail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public Boolean getStrictArrive() {
        return strictArrive;
    }

    public void setStrictArrive(Boolean strictArrive) {
        this.strictArrive = strictArrive;
    }

    @Override
    public String toString() {
        return "Mail{" +
                "toMail=" + toMail +
                ", subject='" + subject + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
