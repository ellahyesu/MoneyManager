package com.stockmanager.api.model;

public class Quote {
    private final String symbol;
    private final String name;
    private final String market;
    private final double price;
    private final double change;
    private final double changePercent;

    public Quote(String symbol, String name, String market, double price, double change, double changePercent) {
        this.symbol = symbol;
        this.name = name;
        this.market = market;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getMarket() {
        return market;
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
}

