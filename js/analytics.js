/**
 * analytics.js — Chart.js Dashboard
 *
 * Handles fetching analytics data from the Spring Boot API
 * and rendering four charts: daily trend, traffic sources,
 * top referrers, and browser breakdown.
 *
 * Falls back to realistic demo data when the API is unavailable.
 */
'use strict';

// Chart instances — kept so we can destroy before re-render
const _charts = {};

// Chart.js global defaults (Neubrutalism theme)
Chart.defaults.color           = '#000000';
Chart.defaults.borderColor     = '#000000';
Chart.defaults.font.family     = 'Space Grotesk, Inter, system-ui, sans-serif';
Chart.defaults.font.size       = 12;
Chart.defaults.font.weight     = 'bold';
Chart.defaults.plugins.legend.labels.boxWidth = 12;
Chart.defaults.plugins.legend.labels.padding  = 16;
Chart.defaults.plugins.tooltip.backgroundColor = '#ffe600';
Chart.defaults.plugins.tooltip.borderColor     = '#000000';
Chart.defaults.plugins.tooltip.borderWidth     = 3;
Chart.defaults.plugins.tooltip.titleColor      = '#000000';
Chart.defaults.plugins.tooltip.bodyColor       = '#000000';
Chart.defaults.plugins.tooltip.titleFont       = { weight: 'bold' };
Chart.defaults.plugins.tooltip.bodyFont        = { weight: 'bold' };
Chart.defaults.plugins.tooltip.padding         = 12;
Chart.defaults.plugins.tooltip.cornerRadius    = 4;

function updateChartDefaults() {
  const isLight = document.documentElement.getAttribute('data-theme') === 'light';
  Chart.defaults.color = isLight ? '#000000' : '#ffffff';
  Chart.defaults.borderColor = isLight ? '#000000' : '#444444';
  Chart.defaults.plugins.tooltip.backgroundColor = isLight ? '#ffe600' : '#1e1e1e';
  Chart.defaults.plugins.tooltip.titleColor = isLight ? '#000000' : '#ffffff';
  Chart.defaults.plugins.tooltip.bodyColor = isLight ? '#000000' : '#ffffff';
}

/**
 * Generate realistic demo analytics data for a given number of total clicks.
 */
function makeDemoData(totalClicks = 142, code = 'demo') {
  const days = 14;
  const labels = [];
  const daily  = [];

  // Distribute clicks across 14 days with a bell-curve-ish shape
  const weights = [.02,.03,.04,.06,.09,.12,.15,.13,.11,.09,.07,.04,.03,.02];
  for (let i = 0; i < days; i++) {
    const d = new Date();
    d.setDate(d.getDate() - (days - 1 - i));
    labels.push(d.toLocaleDateString('en', { month: 'short', day: 'numeric' }));
    daily.push(Math.round(totalClicks * weights[i]));
  }

  return {
    code,
    longUrl: 'https://example.com/demo-url',
    totalClicks,
    uniqueClicks:  Math.round(totalClicks * .72),
    todayClicks:   daily[days - 1],
    ctrIndex:      ((Math.random() * 4) + 1).toFixed(1) + 'x',
    dailyBreakdown: labels.map((date, i) => ({ date, clicks: daily[i] })),
    topReferrers: [
      { referer: 'Direct',    clicks: Math.round(totalClicks * .38) },
      { referer: 'Twitter/X', clicks: Math.round(totalClicks * .22) },
      { referer: 'WhatsApp',  clicks: Math.round(totalClicks * .16) },
      { referer: 'LinkedIn',  clicks: Math.round(totalClicks * .13) },
      { referer: 'Other',     clicks: Math.round(totalClicks * .11) },
    ],
    browserBreakdown: {
      'Chrome':  Math.round(totalClicks * .56),
      'Safari':  Math.round(totalClicks * .22),
      'Firefox': Math.round(totalClicks * .12),
      'Edge':    Math.round(totalClicks * .07),
      'Other':   Math.round(totalClicks * .03),
    },
    trafficSources: {
      'Mobile':  Math.round(totalClicks * .57),
      'Desktop': Math.round(totalClicks * .36),
      'Tablet':  Math.round(totalClicks * .07),
    },
  };
}

/**
 * Fetch analytics from the API, with demo-mode fallback.
 */
async function fetchAnalytics(code) {
  if (!code || window.Config?.FORCE_DEMO_MODE) {
    return makeDemoData(Math.floor(Math.random() * 400) + 80, code || 'demo');
  }

  // First check localStorage (for links created in demo mode)
  try {
    const stored = JSON.parse(localStorage.getItem('ls_links') || '[]');
    const local  = stored.find(l => l.code === code || l.alias === code);
    if (local) {
      return makeDemoData(local.clicks || 0, code);
    }
  } catch (_) {}

  // Try real API
  const controller = new AbortController();
  const timeout    = setTimeout(() => controller.abort(), Config.API_TIMEOUT_MS);

  try {
    const res  = await fetch(`${Config.API_BASE_URL}/api/analytics/${encodeURIComponent(code)}`, {
      signal: controller.signal,
    });
    clearTimeout(timeout);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const json = await res.json();
    return json.data ?? json;
  } catch (err) {
    clearTimeout(timeout);
    console.warn('[analytics] API unavailable, using demo data:', err.message);
    return makeDemoData(Math.floor(Math.random() * 300) + 50, code);
  }
}

/**
 * Destroy a named chart if it exists.
 */
function destroyChart(name) {
  if (_charts[name]) {
    _charts[name].destroy();
    delete _charts[name];
  }
}

/* ── DAILY TREND CHART (Bar) ──────────────────────────────────── */
function renderDailyChart(data) {
  destroyChart('daily');
  const ctx = document.getElementById('daily-chart');
  if (!ctx) return;

  const labels = data.dailyBreakdown.map(d => d.date);
  const values = data.dailyBreakdown.map(d => d.clicks);
  const maxVal = Math.max(...values);

  updateChartDefaults();
  const isLight = document.documentElement.getAttribute('data-theme') === 'light';
  _charts.daily = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Clicks',
        data: values,
        backgroundColor: '#ff5c00',
        borderColor: isLight ? '#000000' : '#ffffff',
        borderWidth: 2,
        borderRadius: 0,
        borderSkipped: false,
        hoverBackgroundColor: '#ffe600',
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: ctx => ` ${ctx.parsed.y.toLocaleString()} clicks`,
          },
        },
      },
      scales: {
        x: {
          grid: { color: isLight ? '#000000' : '#444444', lineWidth: 1 },
          ticks: { maxRotation: 45, font: { size: 10 } },
        },
        y: {
          beginAtZero: true,
          grid: { color: isLight ? '#000000' : '#444444', lineWidth: 1 },
          ticks: {
            stepSize: Math.ceil(maxVal / 5) || 1,
            callback: v => v.toLocaleString(),
          },
        },
      },
    },
  });
}

/* ── TRAFFIC SOURCE CHART (Doughnut) ─────────────────────────── */
function renderSourceChart(data) {
  destroyChart('source');
  const ctx = document.getElementById('source-chart');
  if (!ctx) return;

  const sources = data.trafficSources || { Mobile: 57, Desktop: 36, Tablet: 7 };
  const labels  = Object.keys(sources);
  const values  = Object.values(sources);

  updateChartDefaults();
  const isLight = document.documentElement.getAttribute('data-theme') === 'light';
  _charts.source = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data: values,
        backgroundColor: ['#ffe600', '#00f0ff', '#ff007f'],
        borderColor: isLight ? '#000000' : '#ffffff',
        borderWidth: 2,
        hoverOffset: 8,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      cutout: '70%',
      plugins: {
        legend: { position: 'bottom' },
        tooltip: {
          callbacks: {
            label: ctx => ` ${ctx.label}: ${ctx.parsed.toLocaleString()} clicks (${Math.round(ctx.parsed / ctx.dataset.data.reduce((a,b) => a+b,0) * 100)}%)`,
          },
        },
      },
    },
  });
}

/* ── REFERRER CHART (Horizontal Bar) ─────────────────────────── */
function renderReferrerChart(data) {
  destroyChart('referrer');
  const ctx = document.getElementById('referrer-chart');
  if (!ctx) return;

  const refs   = data.topReferrers.slice(0, 5);
  const labels = refs.map(r => r.referer);
  const values = refs.map(r => r.clicks);

  updateChartDefaults();
  const isLight = document.documentElement.getAttribute('data-theme') === 'light';
  _charts.referrer = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Clicks',
        data: values,
        backgroundColor: '#00f0ff',
        borderColor: isLight ? '#000000' : '#ffffff',
        borderWidth: 2,
        borderRadius: 0,
        borderSkipped: false,
        hoverBackgroundColor: '#ffe600',
      }],
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: true,
      plugins: { legend: { display: false } },
      scales: {
        x: {
          grid: { color: isLight ? '#000000' : '#444444', lineWidth: 1 },
          ticks: { callback: v => v.toLocaleString(), font: { size: 10 } },
        },
        y: {
          grid: { display: false },
          ticks: { font: { size: 11 } },
        },
      },
    },
  });
}

/* ── BROWSER BREAKDOWN CHART (Doughnut) ──────────────────────── */
function renderBrowserChart(data) {
  destroyChart('browser');
  const ctx = document.getElementById('browser-chart');
  if (!ctx) return;

  const browsers = data.browserBreakdown || { Chrome: 56, Safari: 22, Firefox: 12, Edge: 7, Other: 3 };
  const labels   = Object.keys(browsers);
  const values   = Object.values(browsers);

  updateChartDefaults();
  const isLight = document.documentElement.getAttribute('data-theme') === 'light';
  _charts.browser = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data: values,
        backgroundColor: ['#ffe600', '#ff5c00', '#00f0ff', '#ff007f', '#39ff14'],
        borderColor: isLight ? '#000000' : '#ffffff',
        borderWidth: 2,
        hoverOffset: 8,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      cutout: '68%',
      plugins: {
        legend: { position: 'bottom' },
        tooltip: {
          callbacks: {
            label: ctx => ` ${ctx.label}: ${ctx.parsed.toLocaleString()}`,
          },
        },
      },
    },
  });
}

/* ── STATS ROW ────────────────────────────────────────────────── */
function renderStats(data) {
  const set = (id, val) => {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  };
  set('a-total',  data.totalClicks.toLocaleString());
  set('a-unique', data.uniqueClicks.toLocaleString());
  set('a-today',  (data.todayClicks || 0).toLocaleString());
  set('a-ctr',    data.ctrIndex || '—');
}

/* ── MAIN ENTRY: loadAnalytics() ──────────────────────────────── */
async function loadAnalytics(code) {
  const dashboard = document.getElementById('analytics-dashboard');
  const label     = document.getElementById('afetch-label');
  const spinner   = document.getElementById('afetch-spinner');

  // Show loading state
  if (label)   label.classList.add('hidden');
  if (spinner) spinner.classList.remove('hidden');

  const data = await fetchAnalytics(code);

  // Render
  renderStats(data);
  renderDailyChart(data);
  renderSourceChart(data);
  renderReferrerChart(data);
  renderBrowserChart(data);

  // Show dashboard
  if (dashboard) {
    dashboard.classList.remove('hidden');
    setTimeout(() => dashboard.classList.add('loaded'), 50);
  }

  // Reset button
  if (label)   label.classList.remove('hidden');
  if (spinner) spinner.classList.add('hidden');

  // Update chip
  const chip = document.getElementById('modal-url-chip');
  if (chip) {
    const host = window.location.protocol.startsWith('http')
      ? window.location.host
      : 'localhost:8080';
    chip.textContent = `${host}/${code || 'demo'}`;
  }

  window.showToast?.(`Analytics loaded for "${code || 'demo'}"`, 'ok');
}

/* ── WIRE UP BUTTON ───────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  const btn   = document.getElementById('analytics-fetch-btn');
  const input = document.getElementById('analytics-code-input');

  btn?.addEventListener('click', () => {
    const code = input?.value.trim() || '';
    loadAnalytics(code);
  });

  input?.addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      const code = input.value.trim() || '';
      loadAnalytics(code);
    }
  });

  // Auto-load demo analytics on page load after a short delay
  setTimeout(() => {
    loadAnalytics('');
  }, 1200);
});

// Export for main.js to call after creating a link
window.loadAnalytics = loadAnalytics;
