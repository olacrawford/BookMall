package com.bookmall.aftersale.client;

public record LogisticsQueryResult(boolean available, boolean timeout, String reason) {

    public static LogisticsQueryResult ok() {
        return new LogisticsQueryResult(true, false, "ok");
    }

    public static LogisticsQueryResult timedOut() {
        return new LogisticsQueryResult(false, true, "logistics query timeout");
    }

    public static LogisticsQueryResult unavailable() {
        return new LogisticsQueryResult(false, false, "logistics service unavailable");
    }
}
