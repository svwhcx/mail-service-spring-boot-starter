package com.study.mailsender.mail;

import java.util.List;

/**
 * @description
 * @Author cxk
 * @Date 2022/5/22 22:50
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

    /*public static class Builder{

        private List<String> toMail;

        private String content;

        private String subject;

        public Builder toMail(List<String> toMail){
            this.toMail = toMail;
            return this;
        }

        public Builder content(String content){
            this.content = content;
            return this;
        }

        public Builder subject(String subject){
            this.subject = subject;
            return this;
        }

        public Mail build(){
            return null;
        }

    }*/


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
