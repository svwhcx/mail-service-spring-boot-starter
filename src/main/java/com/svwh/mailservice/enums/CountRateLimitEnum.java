package com.svwh.mailservice.enums;

import java.util.concurrent.TimeUnit;

/**
 * @description 默认的技术限流配置
 * @Author cxk
 */
public enum CountRateLimitEnum {

    Rank1(TimeUnit.MINUTES,1L,600),
    Rank2(TimeUnit.MINUTES,1L,600),
    Rank3(TimeUnit.MINUTES,1L,400),
    Rank4(TimeUnit.MINUTES,1L,200),
    Rank5(TimeUnit.MINUTES,1L,100),
    Rank6(TimeUnit.MINUTES,1L,70),
    Rank7(TimeUnit.MINUTES,1L,60),
    Rank8(TimeUnit.MINUTES,1L,50),
    Rank9(TimeUnit.MINUTES,1L,30),
    Rank10(TimeUnit.MINUTES,1L,20),
    Rank11(TimeUnit.MINUTES,1L,10),
    Rank12(TimeUnit.MINUTES,5L,40),
    Rank13(TimeUnit.SECONDS,30L,10),
    Rank14(TimeUnit.MINUTES,3L,10),
    Rank15(TimeUnit.MINUTES,10L,20),
    ;


    public final TimeUnit timeUnit;


    public final Long timeLimit;

    public final Integer accessCountLimit;

    CountRateLimitEnum(TimeUnit timeUnit, long timeLimit, int accessCountLimit) {
        this.timeLimit = timeLimit;
        this.accessCountLimit = accessCountLimit;
        this.timeUnit  = timeUnit;
    }
}
