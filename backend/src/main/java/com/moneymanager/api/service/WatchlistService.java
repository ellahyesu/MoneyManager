package com.moneymanager.api.service;

import com.moneymanager.api.model.WatchlistEntry;
import com.moneymanager.api.model.WatchlistRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WatchlistService {
    private final Map<String, WatchlistSeed> seeds = new ConcurrentHashMap<>();

    public WatchlistService() {
        seedDefaults();
    }

    public List<WatchlistEntry> getAll() {
        List<WatchlistEntry> items = new ArrayList<>();
        for (WatchlistSeed seed : seeds.values()) {
            items.add(new WatchlistEntry(seed.getSymbol(), seed.getName(), seed.getMarket()));
        }
        return items;
    }

    public WatchlistEntry add(WatchlistRequest request) {
        String symbol = normalizeSymbol(request.getSymbol());
        String name = request.getName() == null || request.getName().isBlank() ? symbol : request.getName().trim();
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        WatchlistSeed seed = new WatchlistSeed(symbol, name, "Custom");
        seeds.put(symbol, seed);
        return new WatchlistEntry(seed.getSymbol(), seed.getName(), seed.getMarket());
    }

    public void remove(String symbol) {
        seeds.remove(normalizeSymbol(symbol));
    }

    private void seedDefaults() {
        putSeed(new WatchlistSeed("AAPL", "Apple", "US"));
        putSeed(new WatchlistSeed("MSFT", "Microsoft", "US"));
        putSeed(new WatchlistSeed("NVDA", "NVIDIA", "US"));
        putSeed(new WatchlistSeed("TSLA", "Tesla", "US"));
        putSeed(new WatchlistSeed("AMZN", "Amazon", "US"));
        putSeed(new WatchlistSeed("GOOGL", "Alphabet", "US"));
    }

    private void putSeed(WatchlistSeed seed) {
        seeds.put(seed.getSymbol(), seed);
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    private static class WatchlistSeed {
        private final String symbol;
        private final String name;
        private final String market;

        private WatchlistSeed(String symbol, String name, String market) {
            this.symbol = symbol;
            this.name = name;
            this.market = market;
        }

        private String getSymbol() {
            return symbol;
        }

        private String getName() {
            return name;
        }

        private String getMarket() {
            return market;
        }
    }
}

