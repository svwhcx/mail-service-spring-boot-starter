package com.svwh.mailservice.conf;

import com.svwh.mailservice.algrothim.RateLimit;

import java.util.ArrayList;
import java.util.List;

/**
 * @description 限流策略列表配置
 * @Author cxk
 */
public class RateLimitRankConf {

    private final List<RateLimit> rateLimitList;

    public RateLimitRankConf() {
        this.rateLimitList = new ArrayList<>();
    }

    public List<RateLimit> getRateLimitList() {
        return rateLimitList;
    }

    public Integer limitListSize(){
        return this.rateLimitList.size();
    }

    public RateLimit rankRateLimit(Integer rank){
        return this.rateLimitList.get(rank);
    }
}
