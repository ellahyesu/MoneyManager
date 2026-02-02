package com.moneymanager.api.model;

public class WatchlistEntry {
    private final String symbol;
    private final String name;
    private final String market;

    public WatchlistEntry(String symbol, String name, String market) {
        this.symbol = symbol;
        this.name = name;
        this.market = market;
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
}
