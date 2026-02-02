<template>
  <div class="page">
    <header class="header">
      <h1>MoneyManager Overview</h1>
      <p>Actionable snapshots for global markets and your watchlist.</p>
      <p class="helper" v-if="lastUpdated">Last updated: {{ lastUpdated }}</p>
    </header>

    <div class="sections">
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
            <div class="symbol">{{ quote.market }} ¡¤ {{ quote.symbol }}</div>
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
        <p class="helper">Signals are generated from price-based proxy indicators.</p>
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';

const market = ref(null);
const watchlist = ref([]);
const loading = ref(false);
const adding = ref(false);
const error = ref('');
const newSymbol = ref('');
const newName = ref('');

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
    if (!apiKey) {
      throw new Error('VITE_FINNHUB_API_KEY is not set.');
    }
    const [overview, watchlistEntries] = await Promise.all([
      fetchMarketOverview(apiKey),
      fetchJson('/api/watchlist')
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
    await fetchJson('/api/watchlist', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        symbol: newSymbol.value.trim(),
        name: newName.value.trim()
      })
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
    await fetchJson(`/api/watchlist/${symbol}`, { method: 'DELETE' });
    await refresh();
  } catch (err) {
    error.value = err.message || 'Failed to remove symbol.';
  }
};

const fetchJson = async (url, options) => {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
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
  const response = await fetch(
    `${FINNHUB_BASE}/quote?symbol=${encodeURIComponent(symbol)}&token=${apiKey}`
  );
  if (!response.ok) {
    throw new Error(`Finnhub quote failed: ${response.status}`);
  }
  return response.json();
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
  const fearGreed = deriveFearGreed(quotes);
  return { asOf: new Date().toISOString(), fearGreed, quotes };
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
      const signal = decideSignal(indicators, mapped.price);
      return {
        ...mapped,
        indicators,
        signal,
        rationale: buildRationale(signal, indicators, mapped.price)
      };
    })
  );
  return items;
};

const deriveFearGreed = (quotes) => {
  const spy = quotes.find((q) => q.symbol === 'SPY');
  let value = 50;
  if (spy) {
    const shifted = 50 + spy.changePercent * 2;
    value = Math.round(Math.max(0, Math.min(100, shifted)));
  }
  return {
    value,
    classification: classifyFearGreed(value)
  };
};

const classifyFearGreed = (value) => {
  if (value <= 25) return 'Extreme Fear';
  if (value <= 45) return 'Fear';
  if (value <= 55) return 'Neutral';
  if (value <= 75) return 'Greed';
  return 'Extreme Greed';
};

const deriveIndicators = (quote) => {
  const rsi = clamp(50 + quote.changePercent * 4, 0, 100);
  const macd = round(quote.changePercent / 2);
  const ma50 = round(quote.price * (1 - quote.changePercent / 200));
  const ma200 = round(quote.price * (1 - quote.changePercent / 150));
  const trend = ma50 >= ma200 ? 'Uptrend' : 'Downtrend';
  return { rsi: round(rsi), macd, ma50, ma200, trend };
};

const decideSignal = (indicators, price) => {
  if (indicators.rsi <= 30 && price > indicators.ma50) return 'BUY';
  if (indicators.rsi >= 70 && price < indicators.ma50) return 'SELL';
  return 'HOLD';
};

const buildRationale = (signal, indicators, price) =>
  `Signal ${signal} based on RSI ${indicators.rsi.toFixed(1)}, MACD ${indicators.macd.toFixed(2)}, ` +
  `price ${price.toFixed(2)} vs MA50 ${indicators.ma50.toFixed(2)} and MA200 ${indicators.ma200.toFixed(2)} ` +
  `(${indicators.trend}).`;

const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
const round = (value) => Math.round(value * 100) / 100;

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

onMounted(refresh);
</script>


