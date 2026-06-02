/* ═══════════════════════════════════════════════════════════
   LinkSnap — Frontend Application Logic
   Vanilla JS, no dependencies (except Chart.js for charts)
   ═══════════════════════════════════════════════════════════ */

'use strict';

// ─── Configuration ──────────────────────────────────────────────────────────
const API_BASE = '/api';
const PAGE_SIZE = 10;

// ─── State ──────────────────────────────────────────────────────────────────
const state = {
  currentPage: 0,
  totalPages: 0,
  totalElements: 0,
  allUrls: [],       // full list for client-side search filter
  searchTerm: '',
  dailyChart: null,
  browserChart: null,
};

// ─── DOM References ──────────────────────────────────────────────────────────
const dom = {
  // Shorten form
  shortenForm:    () => document.getElementById('shorten-form'),
  longUrlInput:   () => document.getElementById('long-url-input'),
  aliasInput:     () => document.getElementById('alias-input'),
  ttlSelect:      () => document.getElementById('ttl-select'),
  shortenBtn:     () => document.getElementById('shorten-btn'),
  btnText:        () => document.getElementById('btn-text'),
  btnSpinner:     () => document.getElementById('btn-spinner'),
  errorMsg:       () => document.getElementById('error-msg'),

  // Result
  resultCard:     () => document.getElementById('result-card'),
  resultShortUrl: () => document.getElementById('result-short-url'),
  resultOrigUrl:  () => document.getElementById('result-original-url'),
  resultExpires:  () => document.getElementById('result-expires'),
  resultExpCont:  () => document.getElementById('result-expires-container'),
  copyBtn:        () => document.getElementById('copy-btn'),
  copyIcon:       () => document.getElementById('copy-icon'),
  copyLabel:      () => document.getElementById('copy-label'),

  // Stats
  statTotal:      () => document.getElementById('stat-total'),
  statClicks:     () => document.getElementById('stat-clicks'),

  // Dashboard
  searchInput:    () => document.getElementById('search-input'),
  refreshBtn:     () => document.getElementById('refresh-btn'),
  tableSkeleton:  () => document.getElementById('table-skeleton'),
  tableWrapper:   () => document.getElementById('table-wrapper'),
  urlTableBody:   () => document.getElementById('url-table-body'),
  emptyState:     () => document.getElementById('empty-state'),
  prevPage:       () => document.getElementById('prev-page'),
  nextPage:       () => document.getElementById('next-page'),
  pageInfo:       () => document.getElementById('page-info'),
  pagination:     () => document.getElementById('pagination'),

  // Analytics modal
  analyticsModal: () => document.getElementById('analytics-modal'),
  modalClose:     () => document.getElementById('modal-close'),
  aTotalClicks:   () => document.getElementById('a-total-clicks'),
  aUniqueClicks:  () => document.getElementById('a-unique-clicks'),
  aCode:          () => document.getElementById('a-code'),
  referrerList:   () => document.getElementById('referrer-list'),

  // Toast
  toast:          () => document.getElementById('toast'),
};

// ─── API Helpers ──────────────────────────────────────────────────────────────

async function apiRequest(method, path, body = null) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
  };
  if (body) options.body = JSON.stringify(body);

  const response = await fetch(API_BASE + path, options);
  const json = await response.json().catch(() => null);

  if (!response.ok || (json && !json.success)) {
    const msg = json?.error || `HTTP ${response.status} — ${response.statusText}`;
    throw new Error(msg);
  }

  return json?.data ?? json;
}

// ─── Shorten Form ─────────────────────────────────────────────────────────────

function initShortenForm() {
  dom.shortenForm().addEventListener('submit', async (e) => {
    e.preventDefault();
    await handleShorten();
  });
}

async function handleShorten() {
  const longUrl = dom.longUrlInput().value.trim();
  const customAlias = dom.aliasInput().value.trim() || null;
  const ttlDays = dom.ttlSelect().value ? parseInt(dom.ttlSelect().value) : null;

  if (!longUrl) {
    showError('Please enter a URL to shorten.');
    return;
  }

  setLoadingState(true);
  hideError();
  hideResult();

  try {
    const data = await apiRequest('POST', '/shorten', {
      longUrl,
      customAlias,
      ttlDays,
    });
    showResult(data);
    dom.longUrlInput().value = '';
    dom.aliasInput().value = '';
    dom.ttlSelect().value = '';
    // Refresh dashboard to include the new link
    await loadUrls(0, false);
  } catch (err) {
    showError(err.message);
  } finally {
    setLoadingState(false);
  }
}

function setLoadingState(loading) {
  dom.shortenBtn().disabled = loading;
  dom.btnText().classList.toggle('hidden', loading);
  dom.btnSpinner().classList.toggle('hidden', !loading);
}

function showError(msg) {
  const el = dom.errorMsg();
  el.textContent = '⚠️ ' + msg;
  el.classList.remove('hidden');
}

function hideError() {
  dom.errorMsg().classList.add('hidden');
}

function showResult(data) {
  const shortUrlEl = dom.resultShortUrl();
  shortUrlEl.textContent = data.shortUrl;
  shortUrlEl.href = data.shortUrl;

  dom.resultOrigUrl().textContent = data.longUrl;
  dom.resultOrigUrl().title = data.longUrl;

  if (data.expiresAt) {
    dom.resultExpires().textContent = formatDate(data.expiresAt);
    dom.resultExpCont().style.display = '';
  } else {
    dom.resultExpCont().style.display = 'none';
  }

  dom.resultCard().classList.remove('hidden');

  // Scroll to result card
  dom.resultCard().scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function hideResult() {
  dom.resultCard().classList.add('hidden');
}

// ─── Copy to Clipboard ────────────────────────────────────────────────────────

function initCopyButton() {
  dom.copyBtn().addEventListener('click', async () => {
    const url = dom.resultShortUrl().textContent;
    try {
      await navigator.clipboard.writeText(url);
      dom.copyBtn().classList.add('copied');
      dom.copyIcon().textContent = '✅';
      dom.copyLabel().textContent = 'Copied!';
      showToast('Copied to clipboard!', 'success');
      setTimeout(() => {
        dom.copyBtn().classList.remove('copied');
        dom.copyIcon().textContent = '📋';
        dom.copyLabel().textContent = 'Copy';
      }, 2500);
    } catch {
      // Fallback for non-HTTPS or older browsers
      const ta = document.createElement('textarea');
      ta.value = url;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      showToast('Copied!', 'success');
    }
  });
}

// ─── URL Dashboard ────────────────────────────────────────────────────────────

async function loadUrls(page = 0, showSkeleton = true) {
  state.currentPage = page;

  if (showSkeleton) {
    dom.tableSkeleton().classList.remove('hidden');
    dom.tableWrapper().classList.add('hidden');
    dom.emptyState().classList.add('hidden');
    dom.pagination().style.visibility = 'hidden';
  }

  try {
    const data = await apiRequest('GET', `/urls?page=${page}&size=${PAGE_SIZE}`);
    state.allUrls = data.content || [];
    state.totalPages = data.totalPages || 0;
    state.totalElements = data.totalElements || 0;

    renderTable(state.allUrls);
    updatePagination();
    updateStats();
  } catch (err) {
    showToast('Failed to load URLs: ' + err.message, 'error');
  } finally {
    dom.tableSkeleton().classList.add('hidden');
    dom.tableWrapper().classList.remove('hidden');
    dom.pagination().style.visibility = 'visible';
  }
}

function renderTable(urls) {
  const filtered = state.searchTerm
    ? urls.filter(u =>
        u.longUrl.toLowerCase().includes(state.searchTerm) ||
        (u.code || '').toLowerCase().includes(state.searchTerm) ||
        (u.customAlias || '').toLowerCase().includes(state.searchTerm))
    : urls;

  const tbody = dom.urlTableBody();
  tbody.innerHTML = '';

  if (filtered.length === 0) {
    dom.tableWrapper().classList.add('hidden');
    dom.emptyState().classList.remove('hidden');
    return;
  }

  dom.tableWrapper().classList.remove('hidden');
  dom.emptyState().classList.add('hidden');

  filtered.forEach(url => tbody.appendChild(buildTableRow(url)));
}

function buildTableRow(url) {
  const tr = document.createElement('tr');

  const expiryLabel = getExpiryLabel(url.expiresAt, url.expired);

  tr.innerHTML = `
    <td>
      <a href="${escHtml(url.shortUrl)}" target="_blank" rel="noopener noreferrer"
         class="table-short-url" title="${escHtml(url.shortUrl)}">
        ${escHtml(url.shortUrl.replace(/^https?:\/\//, ''))}
      </a>
    </td>
    <td>
      <span class="table-long-url" title="${escHtml(url.longUrl)}">
        ${escHtml(url.longUrl)}
      </span>
    </td>
    <td><span class="click-badge">🖱 ${formatNumber(url.clicks)}</span></td>
    <td>${formatDate(url.createdAt)}</td>
    <td>${expiryLabel}</td>
    <td>
      <div class="action-btns">
        <button class="btn-icon" title="View analytics"
                onclick="openAnalytics('${escHtml(url.code)}')" aria-label="View analytics for ${escHtml(url.code)}">
          📊
        </button>
        <button class="btn-icon" title="Copy short URL"
                onclick="copyToClipboard('${escHtml(url.shortUrl)}')" aria-label="Copy short URL">
          📋
        </button>
        <button class="btn-icon btn-danger" title="Delete"
                onclick="deleteUrl(${url.id}, '${escHtml(url.code)}')" aria-label="Delete ${escHtml(url.code)}">
          🗑
        </button>
      </div>
    </td>
  `;
  return tr;
}

function getExpiryLabel(expiresAt, expired) {
  if (!expiresAt) return `<span class="expiry-badge expiry-permanent">∞ Permanent</span>`;
  if (expired) return `<span class="expiry-badge expiry-expired">Expired</span>`;

  const diff = new Date(expiresAt) - Date.now();
  const days = Math.ceil(diff / 86400000);
  if (days <= 7) return `<span class="expiry-badge expiry-soon">⚠ ${days}d left</span>`;
  return `<span class="expiry-badge expiry-permanent">${formatDate(expiresAt)}</span>`;
}

function updatePagination() {
  dom.prevPage().disabled = state.currentPage === 0;
  dom.nextPage().disabled = state.currentPage >= state.totalPages - 1;
  dom.pageInfo().textContent = `Page ${state.currentPage + 1} of ${Math.max(state.totalPages, 1)}`;
}

function updateStats() {
  dom.statTotal().textContent = formatNumber(state.totalElements);
  const totalClicks = state.allUrls.reduce((sum, u) => sum + (u.clicks || 0), 0);
  dom.statClicks().textContent = formatNumber(totalClicks);
}

// ─── Delete ──────────────────────────────────────────────────────────────────

async function deleteUrl(id, code) {
  if (!confirm(`Delete the short link "${code}"? This cannot be undone.`)) return;
  try {
    await apiRequest('DELETE', `/urls/${id}`);
    showToast('Link deleted successfully.', 'success');
    await loadUrls(state.currentPage, false);
  } catch (err) {
    showToast('Failed to delete: ' + err.message, 'error');
  }
}

// ─── Analytics Modal ──────────────────────────────────────────────────────────

async function openAnalytics(code) {
  dom.analyticsModal().classList.remove('hidden');
  document.body.style.overflow = 'hidden';

  // Reset
  dom.aTotalClicks().textContent = '…';
  dom.aUniqueClicks().textContent = '…';
  dom.aCode().textContent = code;
  dom.referrerList().innerHTML = '';
  destroyCharts();

  try {
    const data = await apiRequest('GET', `/analytics/${code}`);
    renderAnalytics(data);
  } catch (err) {
    showToast('Failed to load analytics: ' + err.message, 'error');
    closeAnalytics();
  }
}

function renderAnalytics(data) {
  dom.aTotalClicks().textContent = formatNumber(data.totalClicks);
  dom.aUniqueClicks().textContent = formatNumber(data.uniqueClicks);
  dom.aCode().textContent = data.code;

  // Daily chart
  if (data.dailyBreakdown && data.dailyBreakdown.length > 0) {
    renderDailyChart(data.dailyBreakdown);
  }

  // Referrers
  renderReferrers(data.topReferrers || []);

  // Browser chart
  if (data.browserBreakdown && Object.keys(data.browserBreakdown).length > 0) {
    renderBrowserChart(data.browserBreakdown);
  }
}

function renderDailyChart(daily) {
  const ctx = document.getElementById('daily-chart');
  if (state.dailyChart) state.dailyChart.destroy();

  state.dailyChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: daily.map(d => d.date),
      datasets: [{
        label: 'Clicks',
        data: daily.map(d => d.clicks),
        backgroundColor: 'rgba(99, 102, 241, 0.5)',
        borderColor: '#6366f1',
        borderWidth: 2,
        borderRadius: 6,
        borderSkipped: false,
      }],
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#111827',
          borderColor: '#1f2937',
          borderWidth: 1,
          titleColor: '#f0f4ff',
          bodyColor: '#94a3b8',
        },
      },
      scales: {
        x: {
          ticks: { color: '#4b5563', font: { size: 11 } },
          grid: { color: 'rgba(255,255,255,0.04)' },
        },
        y: {
          ticks: { color: '#4b5563', font: { size: 11 } },
          grid: { color: 'rgba(255,255,255,0.04)' },
          beginAtZero: true,
        },
      },
    },
  });
}

function renderReferrers(referrers) {
  const container = dom.referrerList();
  if (referrers.length === 0) {
    container.innerHTML = '<div style="color: #4b5563; font-size: 0.85rem;">No referrer data yet.</div>';
    return;
  }
  const max = Math.max(...referrers.map(r => r.clicks));
  container.innerHTML = referrers.map(r => `
    <div class="referrer-item">
      <span class="referrer-name" title="${escHtml(r.referer)}">${escHtml(r.referer)}</span>
      <div class="referrer-bar-bg">
        <div class="referrer-bar" style="width: ${Math.round((r.clicks / max) * 100)}%"></div>
      </div>
      <span class="referrer-count">${formatNumber(r.clicks)}</span>
    </div>
  `).join('');
}

function renderBrowserChart(browsers) {
  const ctx = document.getElementById('browser-chart');
  if (state.browserChart) state.browserChart.destroy();

  const labels = Object.keys(browsers);
  const data = Object.values(browsers);
  const colors = ['#6366f1','#8b5cf6','#3b82f6','#10b981','#f59e0b','#ef4444','#ec4899'];

  state.browserChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: colors.slice(0, labels.length),
        borderColor: '#0f1623',
        borderWidth: 3,
      }],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { color: '#94a3b8', padding: 16, font: { size: 12 } },
        },
        tooltip: {
          backgroundColor: '#111827',
          borderColor: '#1f2937',
          borderWidth: 1,
          titleColor: '#f0f4ff',
          bodyColor: '#94a3b8',
        },
      },
      cutout: '65%',
    },
  });
}

function destroyCharts() {
  if (state.dailyChart) { state.dailyChart.destroy(); state.dailyChart = null; }
  if (state.browserChart) { state.browserChart.destroy(); state.browserChart = null; }
}

function closeAnalytics() {
  dom.analyticsModal().classList.add('hidden');
  document.body.style.overflow = '';
  destroyCharts();
}

// ─── Copy helper (for table rows) ────────────────────────────────────────────

function copyToClipboard(text) {
  navigator.clipboard.writeText(text)
    .then(() => showToast('Copied to clipboard!', 'success'))
    .catch(() => showToast('Could not copy.', 'error'));
}

// ─── Toast ────────────────────────────────────────────────────────────────────

let toastTimeout = null;

function showToast(msg, type = '') {
  const el = dom.toast();
  el.textContent = (type === 'success' ? '✅ ' : type === 'error' ? '❌ ' : '') + msg;
  el.className = 'toast' + (type ? ' ' + type : '');
  el.classList.remove('hidden');

  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => el.classList.add('hidden'), 3000);
}

// ─── Formatting Helpers ───────────────────────────────────────────────────────

function formatDate(isoString) {
  if (!isoString) return '—';
  try {
    return new Date(isoString).toLocaleDateString(undefined, {
      year: 'numeric', month: 'short', day: 'numeric',
    });
  } catch { return '—'; }
}

function formatNumber(n) {
  if (n === undefined || n === null) return '0';
  return Number(n).toLocaleString();
}

function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;');
}

// ─── Search ───────────────────────────────────────────────────────────────────

function initSearch() {
  let debounceTimer;
  dom.searchInput().addEventListener('input', (e) => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      state.searchTerm = e.target.value.toLowerCase().trim();
      renderTable(state.allUrls);
    }, 250);
  });
}

// ─── Pagination Controls ─────────────────────────────────────────────────────

function initPagination() {
  dom.prevPage().addEventListener('click', () => {
    if (state.currentPage > 0) loadUrls(state.currentPage - 1);
  });
  dom.nextPage().addEventListener('click', () => {
    if (state.currentPage < state.totalPages - 1) loadUrls(state.currentPage + 1);
  });
}

// ─── Navbar scroll effect ─────────────────────────────────────────────────────

function initNavbar() {
  const navbar = document.getElementById('navbar');
  window.addEventListener('scroll', () => {
    navbar.style.background = window.scrollY > 40
      ? 'rgba(8, 11, 18, 0.95)'
      : 'rgba(8, 11, 18, 0.7)';
  }, { passive: true });
}

// ─── Refresh ──────────────────────────────────────────────────────────────────

function initRefreshButton() {
  dom.refreshBtn().addEventListener('click', async () => {
    dom.refreshBtn().classList.add('spinning');
    await loadUrls(state.currentPage);
    dom.refreshBtn().classList.remove('spinning');
    showToast('List refreshed', 'success');
  });
}

// ─── Close modal on overlay click or Escape ───────────────────────────────────

function initModalEvents() {
  dom.modalClose().addEventListener('click', closeAnalytics);
  dom.analyticsModal().addEventListener('click', (e) => {
    if (e.target === dom.analyticsModal()) closeAnalytics();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !dom.analyticsModal().classList.contains('hidden')) {
      closeAnalytics();
    }
  });
}

// ─── Boot ─────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initShortenForm();
  initCopyButton();
  initSearch();
  initPagination();
  initRefreshButton();
  initModalEvents();

  // Load the dashboard immediately
  loadUrls(0);
});

// Expose globals needed by inline onclick handlers in the table
window.openAnalytics = openAnalytics;
window.copyToClipboard = copyToClipboard;
window.deleteUrl = deleteUrl;
