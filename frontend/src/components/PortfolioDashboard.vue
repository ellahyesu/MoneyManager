<template>
  <section class="section portfolio">
    <div class="section-header">
      <div>
        <h2>Portfolio Management</h2>
        <p class="helper">Add holdings and track live portfolio health.</p>
      </div>
      <div class="portfolio-tags">
        <span class="tag">Risk: {{ riskLabel }}</span>
        <span class="tag">Cash {{ cashRatio }}%</span>
      </div>
    </div>

    <div class="portfolio-kpis">
      <div class="kpi">
        <span class="kpi-label">Principal</span>
        <strong>{{ formatCurrency(summary.principal) }}</strong>
      </div>
      <div class="kpi">
        <span class="kpi-label">Valuation</span>
        <strong>{{ formatCurrency(summary.valuation) }}</strong>
      </div>
      <div class="kpi">
        <span class="kpi-label">Realized P/L</span>
        <strong :class="summary.realized >= 0 ? 'positive' : 'negative'">
          {{ formatSigned(summary.realized) }}
        </strong>
      </div>
      <div class="kpi">
        <span class="kpi-label">Unrealized P/L</span>
        <strong :class="summary.unrealized >= 0 ? 'positive' : 'negative'">
          {{ formatSigned(summary.unrealized) }}
        </strong>
      </div>
    </div>

    <div class="card form-card">
      <h3>{{ isEditing ? 'Edit Holding' : 'Add Holding' }}</h3>
      <div class="form-grid">
        <input class="input" v-model="form.symbol" placeholder="Symbol (e.g. GLD)" />
        <select class="input" v-model="form.assetType">
          <option value="US">US Stocks</option>
          <option value="KR">KR Stocks</option>
          <option value="ETF">ETFs/Bonds</option>
          <option value="ALT">Alternatives/Gold</option>
          <option value="CASH">Cash</option>
        </select>
        <input class="input" v-model.number="form.avgPrice" type="number" placeholder="Avg Price" />
        <input class="input" v-model.number="form.principal" type="number" placeholder="Principal" />
        <input class="input" v-model.number="form.currentPrice" type="number" placeholder="Current Price" />
        <input class="input" v-model.number="form.realized" type="number" placeholder="Realized P/L" />
      </div>
      <div class="actions">
        <button class="button" @click="addHolding">{{ isEditing ? 'Save' : 'Add' }}</button>
        <button class="button secondary" @click="resetForm">{{ isEditing ? 'Cancel' : 'Reset' }}</button>
      </div>
      <p class="helper">Unrealized P/L is derived from avg price, principal, and current price.</p>
    </div>

    <div class="card">
      <h3>Holdings</h3>
      <table class="watchlist-table">
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Type</th>
            <th>Avg Price</th>
            <th>Principal</th>
            <th>Current Price</th>
            <th>Unrealized</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in holdings" :key="item.id">
            <td>{{ item.symbol }}</td>
            <td>{{ typeLabel(item.assetType) }}</td>
            <td>{{ formatNumber(item.avgPrice) }}</td>
            <td>{{ formatCurrency(item.principal) }}</td>
            <td>{{ formatNumber(item.currentPrice) }}</td>
            <td :class="item.unrealized >= 0 ? 'positive' : 'negative'">
              {{ formatSigned(item.unrealized) }}
            </td>
            <td>
              <div class="row-actions">
                <button class="button secondary" @click="editHolding(item)">Edit</button>
                <button class="button secondary" @click="removeHolding(item.id)">Remove</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="portfolio-grid">
      <div class="card chart-card">
        <h3>Allocation Overview</h3>
        <div class="donut-wrap">
          <svg viewBox="0 0 160 160" class="donut">
            <circle cx="80" cy="80" r="54" class="donut-base" />
            <circle
              v-for="(slice, idx) in donutSlices"
              :key="slice.label"
              cx="80" cy="80" r="54"
              class="donut-slice"
              :stroke="slice.color"
              :stroke-dasharray="slice.dash"
              :stroke-dashoffset="slice.offset"
              :style="{ animationDelay: `${idx * 0.15}s` }"
            />
          </svg>
          <div class="donut-center">
            <span class="helper">Total Assets</span>
            <strong>{{ formatCurrency(summary.valuation) }}</strong>
          </div>
        </div>
        <ul class="donut-legend">
          <li v-for="slice in allocation" :key="slice.label">
            <span class="legend-dot" :style="{ background: slice.color }"></span>
            {{ slice.label }} {{ slice.value }}%
          </li>
        </ul>
      </div>

      <div class="card chart-card">
        <h3>Asset Change (Last 6 Months)</h3>
        <div class="bar-chart">
          <div
            v-for="bar in monthly"
            :key="bar.label"
            class="bar"
            :style="{ height: `${bar.height}%`, background: bar.color }"
          >
            <span>{{ bar.label }}</span>
          </div>
        </div>
        <p class="helper">This month: +2.4% (up)</p>
      </div>

      <div class="card insight-card">
        <h3>Rebalancing Check</h3>
        <ul class="insight-list">
          <li>Tech allocation is 6% above your target.</li>
          <li>Increase cash by 5% to buffer volatility.</li>
          <li>Add 3-5% to bonds/gold for defense.</li>
        </ul>
        <button class="button">Run Rebalance Simulation</button>
      </div>

      <div class="card insight-card">
        <h3>Risk Alerts</h3>
        <ul class="insight-list">
          <li>GLD volatility rising: review stop-loss/add zones.</li>
          <li>Correlation spike: diversification benefit down.</li>
          <li>3 watchlist names below 20-day average.</li>
        </ul>
        <button class="button secondary">Alert Settings</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';

const holdings = ref([
  {
    id: 1,
    symbol: 'AAPL',
    assetType: 'US',
    avgPrice: 180,
    principal: 3600000,
    currentPrice: 195,
    realized: 200000,
    unrealized: 300000
  }
]);

const form = reactive({
  symbol: '',
  assetType: 'US',
  avgPrice: null,
  principal: null,
  currentPrice: null,
  realized: 0
});

const editingId = ref(null);
const isEditing = computed(() => editingId.value !== null);

const addHolding = () => {
  const missing = [];
  if (!form.symbol || !form.symbol.trim()) missing.push('Symbol');
  if (!form.avgPrice || form.avgPrice <= 0) missing.push('Avg Price');
  if (!form.principal || form.principal <= 0) missing.push('Principal');
  if (!form.currentPrice || form.currentPrice <= 0) missing.push('Current Price');
  if (missing.length) {
    alert(`Missing required fields: ${missing.join(', ')}`);
    return;
  }
  const quantity = form.principal / form.avgPrice;
  const currentValue = quantity * form.currentPrice;
  const unrealized = currentValue - form.principal;
  const record = {
    id: editingId.value ?? Date.now(),
    symbol: form.symbol.toUpperCase(),
    assetType: form.assetType,
    avgPrice: form.avgPrice,
    principal: form.principal,
    currentPrice: form.currentPrice,
    realized: form.realized || 0,
    unrealized: round(unrealized)
  };
  if (editingId.value !== null) {
    const index = holdings.value.findIndex((item) => item.id === editingId.value);
    if (index >= 0) {
      holdings.value.splice(index, 1, record);
    } else {
      holdings.value.unshift(record);
    }
  } else {
    holdings.value.unshift(record);
  }
  resetForm();
};

const resetForm = () => {
  form.symbol = '';
  form.assetType = 'US';
  form.avgPrice = null;
  form.principal = null;
  form.currentPrice = null;
  form.realized = 0;
  editingId.value = null;
};

const editHolding = (item) => {
  form.symbol = item.symbol;
  form.assetType = item.assetType;
  form.avgPrice = item.avgPrice;
  form.principal = item.principal;
  form.currentPrice = item.currentPrice;
  form.realized = item.realized || 0;
  editingId.value = item.id;
};

const removeHolding = (id) => {
  holdings.value = holdings.value.filter((item) => item.id !== id);
};

const summary = computed(() => {
  const principal = holdings.value.reduce((sum, h) => sum + h.principal, 0);
  const valuation = holdings.value.reduce((sum, h) => {
    const quantity = h.principal / h.avgPrice;
    return sum + quantity * h.currentPrice;
  }, 0);
  const realized = holdings.value.reduce((sum, h) => sum + (h.realized || 0), 0);
  const unrealized = valuation - principal;
  return {
    principal: round(principal),
    valuation: round(valuation),
    realized: round(realized),
    unrealized: round(unrealized)
  };
});

const allocation = computed(() => {
  const totals = {
    US: 0,
    KR: 0,
    ETF: 0,
    ALT: 0,
    CASH: 0
  };
  let totalValue = 0;
  holdings.value.forEach((h) => {
    const quantity = h.principal / h.avgPrice;
    const value = quantity * h.currentPrice;
    totals[h.assetType] += value;
    totalValue += value;
  });
  const toPercent = (value) => (totalValue === 0 ? 0 : Math.round((value / totalValue) * 100));
  return [
    { label: 'US Stocks', value: toPercent(totals.US), color: '#2ec4b6' },
    { label: 'KR Stocks', value: toPercent(totals.KR), color: '#ff9f1c' },
    { label: 'ETFs/Bonds', value: toPercent(totals.ETF), color: '#3a7ca5' },
    { label: 'Alternatives/Gold', value: toPercent(totals.ALT), color: '#f4d35e' },
    { label: 'Cash', value: toPercent(totals.CASH), color: '#b9b3a9' }
  ].filter((slice) => slice.value > 0);
});

const donutSlices = computed(() => {
  const radius = 54;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;
  return allocation.value.map((slice) => {
    const dash = (slice.value / 100) * circumference;
    const result = {
      label: slice.label,
      color: slice.color,
      dash: `${dash} ${circumference - dash}`,
      offset: -offset
    };
    offset += dash;
    return result;
  });
});

const monthly = [
  { label: 'Aug', height: 40, color: '#ff9f1c' },
  { label: 'Sep', height: 55, color: '#2ec4b6' },
  { label: 'Oct', height: 35, color: '#3a7ca5' },
  { label: 'Nov', height: 65, color: '#ff6b35' },
  { label: 'Dec', height: 58, color: '#2ec4b6' },
  { label: 'Jan', height: 72, color: '#ff6b35' }
];

const cashRatio = computed(() => {
  const cash = allocation.value.find((slice) => slice.label === 'Cash');
  return cash ? cash.value : 0;
});

const riskLabel = computed(() => (summary.value.unrealized >= 0 ? 'Medium' : 'High'));

const typeLabel = (type) => {
  switch (type) {
    case 'US':
      return 'US Stocks';
    case 'KR':
      return 'KR Stocks';
    case 'ETF':
      return 'ETFs/Bonds';
    case 'ALT':
      return 'Alternatives/Gold';
    case 'CASH':
      return 'Cash';
    default:
      return type;
  }
};

const formatCurrency = (value) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0
  }).format(value || 0);

const formatNumber = (value) =>
  new Intl.NumberFormat('en-US', {
    maximumFractionDigits: 2
  }).format(value || 0);

const formatSigned = (value) => {
  const sign = value >= 0 ? '+' : '-';
  const abs = Math.abs(value || 0);
  return `${sign}${formatCurrency(abs)}`;
};

const round = (value) => Math.round(value * 100) / 100;
</script>
