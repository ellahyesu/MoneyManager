package com.stockmanager.api.model;

import java.time.Instant;
import java.util.List;

public class MarketOverviewResponse {
    private final Instant asOf;
    private final FearGreed fearGreed;
    private final List<Quote> quotes;

    public MarketOverviewResponse(Instant asOf, FearGreed fearGreed, List<Quote> quotes) {
        this.asOf = asOf;
        this.fearGreed = fearGreed;
        this.quotes = quotes;
    }

    public Instant getAsOf() {
        return asOf;
    }

    public FearGreed getFearGreed() {
        return fearGreed;
    }

    public List<Quote> getQuotes() {
        return quotes;
    }
}

