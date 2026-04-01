package com.stockmanager.api.controller;

import com.stockmanager.api.model.WatchlistEntry;
import com.stockmanager.api.model.WatchlistRequest;
import com.stockmanager.api.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {
    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<WatchlistEntry> getAll() {
        return watchlistService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistEntry add(@RequestBody WatchlistRequest request) {
        return watchlistService.add(request);
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String symbol) {
        watchlistService.remove(symbol);
    }
}

