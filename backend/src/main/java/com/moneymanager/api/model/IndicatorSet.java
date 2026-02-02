package com.moneymanager.api.model;

public class IndicatorSet {
    private final double rsi;
    private final double macd;
    private final double ma50;
    private final double ma200;
    private final String trend;

    public IndicatorSet(double rsi, double macd, double ma50, double ma200, String trend) {
        this.rsi = rsi;
        this.macd = macd;
        this.ma50 = ma50;
        this.ma200 = ma200;
        this.trend = trend;
    }

    public double getRsi() {
        return rsi;
    }

    public double getMacd() {
        return macd;
    }

    public double getMa50() {
        return ma50;
    }

    public double getMa200() {
        return ma200;
    }

    public String getTrend() {
        return trend;
    }
}
