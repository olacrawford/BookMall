package com.bookmall.book.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bookmall.sentinel")
public class SentinelRuleProperties {

    private double listQps = 6;
    private double detailQps = 12;
    private double searchQps = 4;

    public double getListQps() {
        return listQps;
    }

    public void setListQps(double listQps) {
        this.listQps = listQps;
    }

    public double getDetailQps() {
        return detailQps;
    }

    public void setDetailQps(double detailQps) {
        this.detailQps = detailQps;
    }

    public double getSearchQps() {
        return searchQps;
    }

    public void setSearchQps(double searchQps) {
        this.searchQps = searchQps;
    }
}
