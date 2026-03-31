<template>
  <div class="page">
    <header class="header">
      <h1>MoneyManager Overview</h1>
      <p>Actionable snapshots for global markets and your watchlist.</p>
      <p class="helper" v-if="lastUpdated">Last updated: {{ lastUpdated }}</p>
        <div class="view-tabs">
      <button
        class="tab"
        :class="activeView === 'market' ? 'active' : ''"
        @click="activeView = 'market'"
      >
        Market
      </button>
      <button
        class="tab"
        :class="activeView === 'portfolio' ? 'active' : ''"
        @click="activeView = 'portfolio'"
      >
        Portfolio
      </button>
    </div>
    </header>

    <div class="sections" v-if="activeView === 'market'">
      <section class="section">
        <div class="section-header">
          <h2>Market Overview</h2>
          <button class="button secondary" @click="refresh" :disabled="loading">
            Refresh
          </button>
        </div>
        <div v-if="error" class="helper">{{ error }}</div>
        <div v-else class="overview-grid">
          <div v-for="quote in market?.quotes" :key="quote.symbol" class="card">
            <div class="symbol">{{ quote.market }} - {{ quote.symbol }}</div>
            <h3>{{ quote.name }}</h3>
            <div class="metric">{{ formatNumber(quote.price) }}</div>
            <div
              class="delta"
              :class="quote.changePercent >= 0 ? 'positive' : 'negative'"
            >
              {{ formatSigned(quote.change) }} ({{ formatSigned(quote.changePercent) }}%)
            </div>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2>Fear & Greed Index</h2>
          <span class="helper">{{ market?.fearGreed.classification }}</span>
        </div>
        <div class="fear-meter">
          <div class="metric">{{ market?.fearGreed.value }}</div>
          <div class="fear-bar">
            <div class="fear-dot" :style="{ left: `${fearPosition}%` }"></div>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2>Watchlist Signals</h2>
        </div>
        <div class="actions">
          <input class="input" v-model="newSymbol" placeholder="Symbol (e.g. AMD)" />
          <input class="input" v-model="newName" placeholder="Name (optional)" />
          <button class="button" @click="addToWatchlist" :disabled="adding">
            Add
          </button>
        </div>
        <p class="helper">Signals are generated from RSI/MACD/MA/Bollinger score.</p>
        <table class="watchlist-table">
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Name</th>
              <th>Price</th>
              <th>RSI</th>
              <th>MACD</th>
              <th>MA50 / MA200</th>
              <th>Signal</th>
              <th>Notes</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in watchlist" :key="item.symbol">
              <td>{{ item.symbol }}</td>
              <td>{{ item.name }}</td>
              <td>{{ formatNumber(item.price) }}</td>
              <td>{{ item.indicators.rsi }}</td>
              <td>{{ item.indicators.macd }}</td>
              <td>{{ formatNumber(item.indicators.ma50) }} / {{ formatNumber(item.indicators.ma200) }}</td>
              <td><span :class="signalClass(item.signal)">{{ item.signal }}</span></td>
              <td>{{ item.rationale }}</td>
              <td>
                <button class="button secondary" @click="removeFromWatchlist(item.symbol)">
                  Remove
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
    <PortfolioDashboard v-else />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import PortfolioDashboard from './components/PortfolioDashboard.vue';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const STORAGE_KEY = 'money-manager-watchlist';
const market = ref(null);
const watchlist = ref([]);
const loading = ref(false);
const adding = ref(false);
const error = ref('');
const newSymbol = ref('');
const newName = ref('');
const activeView = ref('market');

const lastUpdated = computed(() => {
  if (!market.value?.asOf) return '';
  return new Date(market.value.asOf).toLocaleString();
});

const fearPosition = computed(() => {
  if (!market.value?.fearGreed?.value) return 0;
  return Math.min(100, Math.max(0, market.value.fearGreed.value));
});

const refresh = async () => {
  loading.value = true;
  error.value = '';
  try {
    const apiKey = import.meta.env.VITE_FINNHUB_API_KEY;
    const [overview, watchlistEntries] = await Promise.all([
      fetchMarketOverview(apiKey),
      getWatchlistEntries()
    ]);
    market.value = overview;
    watchlist.value = await buildWatchlist(watchlistEntries, apiKey);
  } catch (err) {
    error.value = err.message || 'Failed to load data.';
  } finally {
    loading.value = false;
  }
};

const addToWatchlist = async () => {
  if (!newSymbol.value.trim()) return;
  adding.value = true;
  try {
    await addWatchlistEntry({
      symbol: newSymbol.value.trim(),
      name: newName.value.trim()
    });
    newSymbol.value = '';
    newName.value = '';
    await refresh();
  } catch (err) {
    error.value = err.message || 'Failed to add symbol.';
  } finally {
    adding.value = false;
  }
};

const removeFromWatchlist = async (symbol) => {
  try {
    await removeWatchlistEntry(symbol);
    await refresh();
  } catch (err) {
    error.value = err.message || 'Failed to remove symbol.';
  }
};

const fetchJson = async (url, options) => {
  const response = await fetch(withApiBase(url), options);
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
};

const withApiBase = (url) => {
  if (/^https?:\/\//.test(url)) return url;
  if (!API_BASE_URL) return url;
  return `${API_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`;
};

const FINNHUB_BASE = 'https://finnhub.io/api/v1';
const marketSeeds = [
  { symbol: 'SPY', name: 'S&P 500', market: 'US Index' },
  { symbol: 'QQQ', name: 'NASDAQ 100', market: 'US Index' },
  { symbol: 'DIA', name: 'Dow Jones', market: 'US Index' },
  { symbol: 'AAPL', name: 'Apple', market: 'US' },
  { symbol: 'MSFT', name: 'Microsoft', market: 'US' },
  { symbol: 'NVDA', name: 'NVIDIA', market: 'US' },
  { symbol: 'TSLA', name: 'Tesla', market: 'US' }
];

const fetchQuote = async (symbol, apiKey) => {
  if (!apiKey) {
    return mockQuote(symbol);
  }
  const response = await fetch(
    `${FINNHUB_BASE}/quote?symbol=${encodeURIComponent(symbol)}&token=${apiKey}`
  );
  if (!response.ok) {
    return mockQuote(symbol);
  }
  const data = await response.json();
  if (data?.c === undefined || data?.c === null || data.c === 0) {
    return mockQuote(symbol);
  }
  return data;
};

const fetchMarketOverview = async (apiKey) => {
  const quotes = await Promise.all(
    marketSeeds.map(async (seed) => {
      const quote = await fetchQuote(seed.symbol, apiKey);
      return {
        symbol: seed.symbol,
        name: seed.name,
        market: seed.market,
        price: round(quote.c),
        change: round(quote.d),
        changePercent: round(quote.dp)
      };
    })
  );
  const fearGreed = deriveFearGreedFromChange(quotes);

  return {
    asOf: new Date().toISOString(),
    fearGreed,
    quotes
  };
};

const buildWatchlist = async (entries, apiKey) => {
  const items = await Promise.all(
    entries.map(async (entry) => {
      const quote = await fetchQuote(entry.symbol, apiKey);
      const mapped = {
        symbol: entry.symbol,
        name: entry.name,
        price: round(quote.c),
        change: round(quote.d),
        changePercent: round(quote.dp)
      };
      const indicators = deriveIndicators(mapped);
      const score = scoreIndicators(indicators, mapped.price);
      const signal = decideSignal(score.total);
      return {
        ...mapped,
        indicators,
        signal,
        rationale: buildRationale(score)
      };
    })
  );
  return items;
};

const getWatchlistEntries = async () => {
  if (API_BASE_URL) {
    try {
      return await fetchJson('/api/watchlist');
    } catch (err) {
      console.warn('Falling back to local watchlist storage.', err);
    }
  }
  return readLocalWatchlist();
};

const addWatchlistEntry = async (entry) => {
  const normalized = normalizeEntry(entry);
  if (API_BASE_URL) {
    try {
      await fetchJson('/api/watchlist', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(normalized)
      });
      return;
    } catch (err) {
      console.warn('Falling back to local watchlist storage.', err);
    }
  }
  const items = readLocalWatchlist().filter((item) => item.symbol !== normalized.symbol);
  items.unshift(normalized);
  writeLocalWatchlist(items);
};

const removeWatchlistEntry = async (symbol) => {
  if (API_BASE_URL) {
    try {
      await fetchJson(`/api/watchlist/${symbol}`, { method: 'DELETE' });
      return;
    } catch (err) {
      console.warn('Falling back to local watchlist storage.', err);
    }
  }
  const items = readLocalWatchlist().filter((item) => item.symbol !== symbol.toUpperCase());
  writeLocalWatchlist(items);
};

const normalizeEntry = (entry) => ({
  symbol: entry.symbol.trim().toUpperCase(),
  name: entry.name?.trim() || entry.symbol.trim().toUpperCase(),
  market: 'Custom'
});

const readLocalWatchlist = () => {
  const fallback = [
    normalizeEntry({ symbol: 'MSFT', name: 'Microsoft' }),
    normalizeEntry({ symbol: 'NVDA', name: 'NVIDIA' }),
    normalizeEntry({ symbol: 'AAPL', name: 'Apple' }),
    normalizeEntry({ symbol: 'TSLA', name: 'Tesla' })
  ];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return fallback;
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed) || parsed.length === 0) return fallback;
    return parsed.map(normalizeEntry);
  } catch {
    return fallback;
  }
};

const writeLocalWatchlist = (items) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
};

const deriveIndicators = (quote) => {
  const rsi = clamp(50 + quote.changePercent * 4, 0, 100);
  const macd = round(quote.changePercent / 2);
  const ma50 = round(quote.price * (1 - quote.changePercent / 200));
  const ma200 = round(quote.price * (1 - quote.changePercent / 150));
  const bbLower = round(quote.price * (1 - 0.02));
  const bbUpper = round(quote.price * (1 + 0.02));
  const trend = ma50 >= ma200 ? 'Uptrend' : 'Downtrend';
  return {
    rsi: round(rsi),
    macd,
    ma50,
    ma200,
    trend,
    bbLower,
    bbUpper,
    lastClose: quote.price,
    prevLower: bbLower,
    prevClose: quote.price
  };
};

const scoreIndicators = (indicators, price) => {
  let total = 0;
  const flags = [];

  if (indicators.rsi !== null) {
    if (indicators.rsi < 30) {
      total += 1;
      flags.push('RSI<30');
    } else if (indicators.rsi > 70) {
      total -= 1;
      flags.push('RSI>70');
    }
  }

  if (indicators.macd !== null) {
    if (indicators.macd > 0.4) {
      total += 1;
      flags.push('MACD positive');
    } else if (indicators.macd < -0.4) {
      total -= 1;
      flags.push('MACD negative');
    }
  }

  if (indicators.ma200 !== null && price > indicators.ma200) {
    total += 1;
    flags.push('Price>MA200');
  }
  if (indicators.ma50 !== null && price < indicators.ma50) {
    total -= 1;
    flags.push('Price<MA50');
  }

  if (indicators.bbLower !== null && indicators.bbUpper !== null) {
    if (price <= indicators.bbLower) {
      total += 1;
      flags.push('BB lower');
    } else if (price >= indicators.bbUpper) {
      total -= 1;
      flags.push('BB upper');
    }
  }

  return { total, flags };
};

const decideSignal = (totalScore) => {
  if (totalScore >= 2) return 'BUY';
  if (totalScore <= -2) return 'SELL';
  return 'HOLD';
};

const buildRationale = (score) => {
  const detail = score.flags.length ? score.flags.join(', ') : 'No strong signals';
  return `Score ${score.total}: ${detail}`;
};

const deriveFearGreedFromChange = (quotes) => {
  const spy = quotes.find((q) => q.symbol === 'SPY');
  let value = 50;
  if (spy) {
    const shifted = 50 + spy.changePercent * 2;
    value = Math.round(Math.max(0, Math.min(100, shifted)));
  }
  return classifyFearGreed(value);
};

const classifyFearGreed = (value) => {
  if (value <= 25) return { value, classification: 'Extreme Fear' };
  if (value <= 45) return { value, classification: 'Fear' };
  if (value <= 55) return { value, classification: 'Neutral' };
  if (value <= 75) return { value, classification: 'Greed' };
  return { value, classification: 'Extreme Greed' };
};

const formatNumber = (value) => {
  if (value === null || value === undefined) return '-';
  return new Intl.NumberFormat('en-US', {
    maximumFractionDigits: 2
  }).format(value);
};

const formatSigned = (value) => {
  if (value === null || value === undefined) return '-';
  const sign = value >= 0 ? '+' : '';
  return `${sign}${value.toFixed(2)}`;
};

const signalClass = (signal) => `signal ${signal.toLowerCase()}`;

const round = (value) => Math.round(value * 100) / 100;
const clamp = (value, min, max) => Math.max(min, Math.min(max, value));

const mockQuote = (symbol) => {
  const seed = Array.from(symbol).reduce((sum, ch) => sum + ch.charCodeAt(0), 0);
  const base = 80 + (seed % 220);
  const change = ((seed % 19) - 9) * 0.73;
  const price = round(base + change * 2.8);
  return {
    c: price,
    d: round(change),
    dp: round((change / base) * 100)
  };
};

onMounted(refresh);
</script>



























