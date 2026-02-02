package com.moneymanager.api.service;

import com.moneymanager.api.model.FearGreed;
import com.moneymanager.api.model.MarketOverviewResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MarketDataService {
    public MarketOverviewResponse getOverview() {
        return new MarketOverviewResponse(Instant.now(), new FearGreed(50, "Neutral"), List.of());
    }
}
