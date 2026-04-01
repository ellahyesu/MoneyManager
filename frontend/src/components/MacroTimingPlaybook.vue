<template>
  <section class="section playbook-page">
    <div class="section-header">
      <div>
        <h2>Macro Timing Playbook</h2>
        <p class="helper">
          Macro proxies are translated into concrete entry, add, trim, and stop-review zones for your holdings.
        </p>
      </div>
      <button class="button secondary" @click="loadPlaybook" :disabled="loading">Refresh Playbook</button>
    </div>

    <div v-if="loading" class="helper">Building scenarios from macro signals and portfolio holdings...</div>
    <div v-else-if="playbookError" class="empty-state">
      <h3>Playbook unavailable</h3>
      <p class="helper">{{ playbookError }}</p>
    </div>
    <div v-else-if="!props.holdings.length" class="empty-state">
      <h3>No holdings yet</h3>
      <p class="helper">Add holdings in the Portfolio tab to generate timing scenarios.</p>
    </div>
    <template v-else>
      <div class="playbook-hero">
        <div class="playbook-score">
          <span class="kpi-label">Market Regime</span>
          <strong>{{ environment.regime }}</strong>
          <p class="helper">{{ environment.summary }}</p>
        </div>
        <div class="playbook-callout">
          <span class="pill" :class="environment.biasClass">{{ environment.biasLabel }}</span>
          <p>{{ environment.actionBias }}</p>
        </div>
      </div>

      <div class="macro-grid">
        <article v-for="metric in environment.metrics" :key="metric.label" class="card macro-card">
          <div class="macro-card-top">
            <span class="symbol">{{ metric.label }}</span>
            <span class="signal" :class="metric.toneClass">{{ metric.tone }}</span>
          </div>
          <strong class="macro-value">{{ metric.value }}</strong>
          <p class="helper">{{ metric.note }}</p>
        </article>
      </div>

      <section class="scenario-stack">
        <article v-for="scenario in scenarios" :key="scenario.symbol" class="card scenario-card">
          <div class="scenario-head">
            <div>
              <div class="symbol">{{ scenario.typeLabel }} - {{ scenario.symbol }}</div>
              <h3>{{ scenario.symbol }}</h3>
            </div>
            <div class="scenario-prices">
              <strong>{{ formatNumber(scenario.currentPrice) }}</strong>
              <span class="helper">Avg {{ formatNumber(scenario.avgPrice) }}</span>
            </div>
          </div>

          <div class="scenario-grid">
            <div class="scenario-kpi">
              <span class="kpi-label">Very Good Entry</span>
              <strong>{{ formatNumber(scenario.entryStrong) }}</strong>
              <p>{{ scenario.entryStrongText }}</p>
            </div>
            <div class="scenario-kpi">
              <span class="kpi-label">Staged Add</span>
              <strong>{{ formatNumber(scenario.entryBase) }}</strong>
              <p>{{ scenario.entryBaseText }}</p>
            </div>
            <div class="scenario-kpi">
              <span class="kpi-label">Trim / Profit</span>
              <strong>{{ formatNumber(scenario.trimPrice) }}</strong>
              <p>{{ scenario.trimText }}</p>
            </div>
            <div class="scenario-kpi danger">
              <span class="kpi-label">Stop Review</span>
              <strong>{{ formatNumber(scenario.stopPrice) }}</strong>
              <p>{{ scenario.stopText }}</p>
            </div>
          </div>

          <div class="scenario-footer">
            <div class="pillar-list">
              <span v-for="reason in scenario.drivers" :key="reason" class="tag soft">{{ reason }}</span>
            </div>
            <p class="helper">{{ scenario.narrative }}</p>
          </div>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';

const props = defineProps({
  holdings: {
    type: Array,
    required: true
  },
  apiKey: {
    type: String,
    default: ''
  }
});

const loading = ref(false);
const environment = ref(createDefaultEnvironment());
const scenarios = ref([]);
const playbookError = ref('');

const FINNHUB_BASE = 'https://finnhub.io/api/v1';
const macroSeeds = [
  { symbol: 'SPY', label: 'S&P 500' },
  { symbol: 'QQQ', label: 'Nasdaq' },
  { symbol: 'USO', label: 'Oil' },
  { symbol: 'TLT', label: '10Y Pressure' },
  { symbol: 'GLD', label: 'Gold' },
  { symbol: 'UUP', label: 'Dollar' },
  { symbol: 'XLE', label: 'Energy Leadership' }
];

const loadPlaybook = async () => {
  loading.value = true;
  playbookError.value = '';
  try {
    const [macroQuotes, holdingQuotes] = await Promise.all([
      Promise.all(macroSeeds.map((seed) => fetchQuote(seed.symbol, props.apiKey))),
      Promise.all(props.holdings.map((holding) => fetchQuote(holding.symbol, props.apiKey)))
    ]);

    const macroMap = Object.fromEntries(
      macroSeeds.map((seed, index) => [seed.symbol, mapQuote(seed.symbol, macroQuotes[index])])
    );
    const quoteMap = Object.fromEntries(
      props.holdings.map((holding, index) => [holding.symbol, mapQuote(holding.symbol, holdingQuotes[index])])
    );

    environment.value = buildEnvironment(macroMap);
    scenarios.value = props.holdings.map((holding) => buildScenario(holding, quoteMap[holding.symbol], environment.value));
  } catch (error) {
    console.error('Failed to build macro playbook.', error);
    environment.value = createDefaultEnvironment();
    scenarios.value = props.holdings.map((holding) => buildFallbackScenario(holding));
    playbookError.value = 'Live macro data could not be loaded. Showing fallback scenarios from local pricing assumptions.';
  } finally {
    loading.value = false;
  }
};

const fetchQuote = async (symbol, apiKey) => {
  if (!apiKey) return mockQuote(symbol);
  try {
    const response = await fetch(`${FINNHUB_BASE}/quote?symbol=${encodeURIComponent(symbol)}&token=${apiKey}`);
    if (!response.ok) return mockQuote(symbol);
    const data = await response.json();
    if (data?.c === undefined || data?.c === null || data.c === 0) return mockQuote(symbol);
    return data;
  } catch {
    return mockQuote(symbol);
  }
};

const mapQuote = (symbol, raw) => ({
  symbol,
  price: round(raw.c),
  change: round(raw.d),
  changePercent: round(raw.dp)
});

const buildEnvironment = (quotes) => {
  const spy = quotes.SPY;
  const qqq = quotes.QQQ;
  const oil = quotes.USO;
  const tlt = quotes.TLT;
  const gld = quotes.GLD;
  const uup = quotes.UUP;
  const xle = quotes.XLE;

  const oilShock = oil.changePercent;
  const ratePressure = round(-tlt.changePercent);
  const dollarStrength = uup.changePercent;
  const goldBid = gld.changePercent;
  const riskTone = round((spy.changePercent + qqq.changePercent) / 2);
  const energyLeadership = round(xle.changePercent - spy.changePercent);
  const geoStress = clamp(
    45 + oilShock * 3 + Math.max(ratePressure, 0) * 2 + goldBid * 2 + dollarStrength * 3 + Math.max(-riskTone, 0) * 6,
    0,
    100
  );

  const regime = geoStress >= 70
    ? 'Defensive / Stress'
    : geoStress >= 55
      ? 'Tight / Event-Driven'
      : riskTone >= 0.8 && ratePressure <= 0
        ? 'Risk-On / Growth Friendly'
        : 'Balanced / Selective';

  const biasLabel = geoStress >= 65 ? 'Capital Preservation First' : riskTone >= 0.8 ? 'Buy Dips, Trim Rips' : 'Selective Scaling';
  const biasClass = geoStress >= 65 ? 'sell' : riskTone >= 0.8 ? 'buy' : 'hold';
  const actionBias = geoStress >= 65
    ? 'Prefer deeper entry discounts, tighter stops, and faster trims into strength.'
    : riskTone >= 0.8
      ? 'Buy pullbacks in leaders rather than chasing straight-line moves.'
      : 'Scale positions patiently and demand confirmation before adding size.';

  return {
    regime,
    geoStress,
    oilShock,
    ratePressure,
    dollarStrength,
    goldBid,
    riskTone,
    energyLeadership,
    biasLabel,
    biasClass,
    actionBias,
    summary: `Geo-stress ${Math.round(geoStress)}/100, oil ${formatSigned(oilShock)}%, rate pressure ${formatSigned(ratePressure)}%, risk tone ${formatSigned(riskTone)}%.`,
    metrics: [
      metricCard('Geopolitical Stress', `${Math.round(geoStress)}/100`, geoStress >= 65 ? 'High' : geoStress >= 50 ? 'Elevated' : 'Contained', geoStress >= 65 ? 'sell' : geoStress >= 50 ? 'hold' : 'buy', 'Oil, gold, dollar, and equity weakness are blended into a live stress proxy.'),
      metricCard('Oil', `${formatSigned(oilShock)}%`, oilShock >= 1.5 ? 'Heating Up' : oilShock <= -1 ? 'Cooling' : 'Stable', oilShock >= 1.5 ? 'sell' : oilShock <= -1 ? 'buy' : 'hold', 'Crude spikes can quickly change inflation and headline risk.'),
      metricCard('10Y Yield Pressure', `${formatSigned(ratePressure)}%`, ratePressure >= 1 ? 'Higher Yields' : ratePressure <= -1 ? 'Lower Yields' : 'Flat', ratePressure >= 1 ? 'sell' : ratePressure <= -1 ? 'buy' : 'hold', 'Proxy derived from long-duration Treasury behavior; falling TLT implies yield pressure.'),
      metricCard('Dollar', `${formatSigned(dollarStrength)}%`, dollarStrength >= 0.6 ? 'Firm' : dollarStrength <= -0.6 ? 'Soft' : 'Neutral', dollarStrength >= 0.6 ? 'sell' : dollarStrength <= -0.6 ? 'buy' : 'hold', 'A stronger dollar generally tightens global liquidity.'),
      metricCard('Gold', `${formatSigned(goldBid)}%`, goldBid >= 0.8 ? 'Defensive Bid' : goldBid <= -0.8 ? 'Risk-On' : 'Quiet', goldBid >= 0.8 ? 'sell' : goldBid <= -0.8 ? 'buy' : 'hold', 'Gold strength often confirms hedging demand or inflation concern.'),
      metricCard('Risk Tone', `${formatSigned(riskTone)}%`, riskTone >= 0.8 ? 'Constructive' : riskTone <= -0.8 ? 'Fragile' : 'Mixed', riskTone >= 0.8 ? 'buy' : riskTone <= -0.8 ? 'sell' : 'hold', 'SPY and QQQ jointly show whether dips should be bought or faded.')
    ]
  };
};

const buildScenario = (holding, quote, env) => {
  const currentPrice = quote?.price || holding.currentPrice;
  const avgPrice = holding.avgPrice;
  const profile = classifyHolding(holding);
  const penalty = computeMacroPenalty(profile, env);
  const entryBase = round(currentPrice * (1 - clamp(0.05 + penalty, 0.04, 0.17)));
  const entryStrong = round(currentPrice * (1 - clamp(0.09 + penalty, 0.08, 0.24)));
  const trimPrice = round(currentPrice * (1 + clamp(0.08 - penalty / 2 + profile.upsideBoost, 0.06, 0.18)));
  const stopPrice = round(Math.max(avgPrice * (1 - clamp(0.06 + penalty + profile.stopExtra, 0.06, 0.18)), currentPrice * (1 - clamp(0.06 + penalty + profile.stopExtra, 0.06, 0.18))));

  return {
    symbol: holding.symbol,
    typeLabel: typeLabel(holding.assetType),
    currentPrice,
    avgPrice,
    entryBase,
    entryStrong,
    trimPrice,
    stopPrice,
    entryStrongText: `${formatNumber(entryStrong)} nearby would be a very good entry if selling comes from macro fear rather than company damage.`,
    entryBaseText: `${formatNumber(entryBase)} is the first staged buy zone while market stress stays manageable.`,
    trimText: `${formatNumber(trimPrice)} is a reasonable area to scale out if price outruns improving macro conditions.`,
    stopText: `${formatNumber(stopPrice)} is where thesis damage starts to matter and stop-loss discipline should be reviewed.`,
    drivers: buildDrivers(profile, env),
    narrative: buildNarrative(profile, env, entryBase, entryStrong, trimPrice, stopPrice)
  };
};

const buildFallbackScenario = (holding) => {
  const currentPrice = holding.currentPrice || holding.avgPrice || 0;
  const avgPrice = holding.avgPrice || currentPrice;
  const entryBase = round(currentPrice * 0.94);
  const entryStrong = round(currentPrice * 0.89);
  const trimPrice = round(currentPrice * 1.1);
  const stopPrice = round(Math.max(currentPrice * 0.91, avgPrice * 0.9));

  return {
    symbol: holding.symbol,
    typeLabel: typeLabel(holding.assetType),
    currentPrice,
    avgPrice,
    entryBase,
    entryStrong,
    trimPrice,
    stopPrice,
    entryStrongText: `${formatNumber(entryStrong)} nearby would be a strong discounted entry if the broad tape stabilizes.`,
    entryBaseText: `${formatNumber(entryBase)} is the first staged buy zone from current local pricing assumptions.`,
    trimText: `${formatNumber(trimPrice)} is a practical profit-taking zone if momentum becomes stretched.`,
    stopText: `${formatNumber(stopPrice)} is the first level where stop-loss review should start.`,
    drivers: ['fallback local pricing', 'portfolio average cost', 'volatility buffer'],
    narrative: `Live macro data was unavailable, so this scenario uses conservative fallback bands around your current holding price. Prefer smaller size until live signals return.`
  };
};

const classifyHolding = (holding) => {
  const symbol = holding.symbol.toUpperCase();
  const growthSymbols = ['AAPL', 'MSFT', 'NVDA', 'TSLA', 'AMD', 'META', 'AMZN', 'QQQ'];
  const energySymbols = ['XLE', 'CVX', 'XOM', 'OXY', 'SLB'];
  const hardAssetSymbols = ['GLD', 'SLV', 'GDX'];
  const defensiveSymbols = ['JNJ', 'PG', 'KO', 'PEP', 'XLV', 'XLP'];

  if (growthSymbols.includes(symbol) || holding.assetType === 'US') return { bucket: 'growth', upsideBoost: 0.03, stopExtra: 0.02 };
  if (energySymbols.includes(symbol)) return { bucket: 'energy', upsideBoost: 0.04, stopExtra: 0.01 };
  if (hardAssetSymbols.includes(symbol) || holding.assetType === 'ALT') return { bucket: 'hard-asset', upsideBoost: 0.02, stopExtra: 0 };
  if (defensiveSymbols.includes(symbol) || holding.assetType === 'ETF') return { bucket: 'defensive', upsideBoost: 0.01, stopExtra: 0.01 };
  return { bucket: 'balanced', upsideBoost: 0.02, stopExtra: 0.01 };
};

const computeMacroPenalty = (profile, env) => {
  switch (profile.bucket) {
    case 'growth':
      return clamp(env.ratePressure * 0.012 + env.geoStress * 0.0007 + Math.max(-env.riskTone, 0) * 0.015, 0.02, 0.09);
    case 'energy':
      return clamp(Math.max(-env.oilShock, 0) * 0.015 + Math.max(-env.energyLeadership, 0) * 0.01, 0.015, 0.06);
    case 'hard-asset':
      return clamp(Math.max(-env.goldBid, 0) * 0.01 + Math.max(-env.geoStress + 55, 0) * 0.0004, 0.015, 0.05);
    case 'defensive':
      return clamp(env.ratePressure * 0.006 + Math.max(-env.riskTone, 0) * 0.008, 0.015, 0.05);
    default:
      return clamp(env.ratePressure * 0.008 + env.geoStress * 0.0005, 0.02, 0.06);
  }
};

const buildDrivers = (profile, env) => {
  const drivers = [];
  if (env.geoStress >= 60) drivers.push('geopolitical stress elevated');
  if (env.oilShock >= 1.5) drivers.push('oil firming');
  if (env.ratePressure >= 1) drivers.push('10Y yield pressure up');
  if (env.goldBid >= 0.8) drivers.push('gold defensive bid');
  if (env.dollarStrength >= 0.6) drivers.push('dollar tightening');

  switch (profile.bucket) {
    case 'growth':
      drivers.push('growth multiple sensitivity');
      break;
    case 'energy':
      drivers.push('energy beta supported by crude');
      break;
    case 'hard-asset':
      drivers.push('hard-asset hedge behavior');
      break;
    case 'defensive':
      drivers.push('defensive relative strength');
      break;
    default:
      drivers.push('mixed macro sensitivity');
      break;
  }
  return drivers.slice(0, 4);
};

const buildNarrative = (profile, env, entryBase, entryStrong, trimPrice, stopPrice) => {
  if (profile.bucket === 'growth') {
    return `Rates and geopolitical stress matter most here. Prefer first adds near ${formatNumber(entryBase)} and reserve heavier buying for ${formatNumber(entryStrong)} if yields stop rising. If price spikes toward ${formatNumber(trimPrice)} while oil and the dollar stay firm, trim into strength. A clean break below ${formatNumber(stopPrice)} means reassessing risk quickly.`;
  }
  if (profile.bucket === 'energy') {
    return `This name can outperform when oil and geopolitical risk stay firm. Pullbacks into ${formatNumber(entryBase)} are buyable if crude remains bid; ${formatNumber(entryStrong)} becomes the aggressive reload zone on broad market fear. If price extends into ${formatNumber(trimPrice)} while crude momentum fades, take partial profit. Below ${formatNumber(stopPrice)}, the crude-linked thesis is weakening.`;
  }
  if (profile.bucket === 'hard-asset') {
    return `This setup benefits when hedging demand stays alive. Use ${formatNumber(entryBase)} as a patient add zone and ${formatNumber(entryStrong)} when cross-asset stress rises sharply. Into ${formatNumber(trimPrice)}, trim if defensive positioning becomes crowded. A move under ${formatNumber(stopPrice)} suggests the hedge bid is fading.`;
  }
  if (profile.bucket === 'defensive') {
    return `This is more about capital protection than upside chase. Entries near ${formatNumber(entryBase)} make sense on broad pullbacks, while ${formatNumber(entryStrong)} is attractive if the market derisks without company-specific damage. Use ${formatNumber(trimPrice)} to harvest strength when the market rotates into safety. Below ${formatNumber(stopPrice)}, relative strength is no longer paying you enough.`;
  }
  return `Macro is mixed, so use staggered adds near ${formatNumber(entryBase)} and only get aggressive near ${formatNumber(entryStrong)} if the broad tape stabilizes. Take some money off into ${formatNumber(trimPrice)} when stress remains unresolved, and treat ${formatNumber(stopPrice)} as the line where thesis and position size both need review.`;
};

const metricCard = (label, value, tone, toneClass, note) => ({
  label,
  value,
  tone,
  toneClass,
  note
});

const createDefaultEnvironment = () => ({
  regime: 'Balanced / Selective',
  geoStress: 50,
  oilShock: 0,
  ratePressure: 0,
  dollarStrength: 0,
  goldBid: 0,
  riskTone: 0,
  energyLeadership: 0,
  biasLabel: 'Selective Scaling',
  biasClass: 'hold',
  actionBias: 'Scale positions patiently and demand confirmation before adding size.',
  summary: 'Refresh the playbook to load macro proxies.',
  metrics: []
});

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

const formatNumber = (value) =>
  new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(value || 0);

const formatSigned = (value) => {
  const sign = value >= 0 ? '+' : '';
  return `${sign}${round(value)}`;
};

const round = (value) => Math.round(value * 100) / 100;
const clamp = (value, min, max) => Math.max(min, Math.min(max, value));

const mockQuote = (symbol) => {
  const seed = Array.from(symbol).reduce((sum, char) => sum + char.charCodeAt(0), 0);
  const base = 80 + (seed % 220);
  const change = ((seed % 17) - 8) * 0.68;
  return {
    c: round(base + change * 2.6),
    d: round(change),
    dp: round((change / base) * 100)
  };
};

watch(() => props.holdings, loadPlaybook, { deep: true });
onMounted(loadPlaybook);
</script>
