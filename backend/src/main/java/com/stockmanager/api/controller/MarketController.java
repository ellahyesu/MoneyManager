package com.stockmanager.api.controller;

import com.stockmanager.api.model.MarketOverviewResponse;
import com.stockmanager.api.service.MarketDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final MarketDataService marketDataService;

    public MarketController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/overview")
    public MarketOverviewResponse getOverview() {
        return marketDataService.getOverview();
    }
}

