package com.stockmanager.api.model;

public class WatchlistItem {
    private final String symbol;
    private final String name;
    private final double price;
    private final double change;
    private final double changePercent;
    private final IndicatorSet indicators;
    private final Signal signal;
    private final String rationale;

    public WatchlistItem(String symbol, String name, double price, double change, double changePercent,
                         IndicatorSet indicators, Signal signal, String rationale) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
        this.indicators = indicators;
        this.signal = signal;
        this.rationale = rationale;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public double getChange() {
        return change;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public IndicatorSet getIndicators() {
        return indicators;
    }

    public Signal getSignal() {
        return signal;
    }

    public String getRationale() {
        return rationale;
    }
}

