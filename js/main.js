/**
 * main.js — Core Application Logic
 *
 * Responsibilities:
 *  - URL shortening form submission (real API + demo mode fallback)
 *  - Copy to clipboard with animation
 *  - Recent links sidebar
 *  - Hero live-stats fetch
 *  - API docs accordion
 *  - Utility helpers
 */
'use strict';

/* ══════════════════════════════════════════════════════════
   STATE
══════════════════════════════════════════════════════════ */
let isDemoMode  = false;
let lastCreated = null;

/* ══════════════════════════════════════════════════════════
   LOCAL STORAGE HELPERS
══════════════════════════════════════════════════════════ */
function getStoredLinks() {
  try { return JSON.parse(localStorage.getItem('ls_links') || '[]'); }
  catch { return []; }
}
function saveLinks(links) {
  localStorage.setItem('ls_links', JSON.stringify(links));
}
function addLink(link) {
  const links = getStoredLinks();
  links.unshift(link);
  if (links.length > 50) links.pop(); // cap at 50
  saveLinks(links);
}

/* ══════════════════════════════════════════════════════════
   BASE62 CODE GENERATOR (for demo mode)
══════════════════════════════════════════════════════════ */
const BASE62_CHARS = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
function genBase62(len = 6) {
  const arr = new Uint8Array(len);
  crypto.getRandomValues(arr);
  return Array.from(arr, b => BASE62_CHARS[b % 62]).join('');
}

/* ══════════════════════════════════════════════════════════
   URL VALIDATION
══════════════════════════════════════════════════════════ */
function isValidUrl(str) {
  try {
    const url = new URL(str);
    return ['http:', 'https:'].includes(url.protocol);
  } catch { return false; }
}

/* ══════════════════════════════════════════════════════════
   API CALL — SHORTEN
══════════════════════════════════════════════════════════ */
async function callShortenApi(payload) {
  const controller = new AbortController();
  const timeout    = setTimeout(() => controller.abort(), Config.API_TIMEOUT_MS);

  try {
    const res = await fetch(`${Config.API_BASE_URL}/api/shorten`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(payload),
      signal:  controller.signal,
    });
    clearTimeout(timeout);

    const json = await res.json();
    if (!res.ok) {
      const msg = json.error || json.message || `Server error (HTTP ${res.status})`;
      throw new Error(msg);
    }
    return { ok: true, data: json.data ?? json };
  } catch (err) {
    clearTimeout(timeout);
    if (err.name === 'AbortError') {
      return { ok: false, error: 'Request timed out. Check your backend is running.' };
    }
    if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError')) {
      return { ok: false, error: 'NETWORK', originalError: err.message };
    }
    return { ok: false, error: err.message };
  }
}

/* ══════════════════════════════════════════════════════════
   DEMO MODE SIMULATION
══════════════════════════════════════════════════════════ */
function simulateShorten(payload) {
  const links = getStoredLinks();

  // Check alias uniqueness (local)
  if (payload.customAlias) {
    if (!/^[a-zA-Z0-9\-_]{2,30}$/.test(payload.customAlias)) {
      return { ok: false, error: 'Alias must be 2–30 chars: letters, digits, hyphens, underscores.' };
    }
    if (links.some(l => l.code === payload.customAlias || l.alias === payload.customAlias)) {
      return { ok: false, error: `The alias "${payload.customAlias}" is already taken.` };
    }
  }

  const code    = payload.customAlias || genBase62(6);
  const now     = Date.now();
  const expires = payload.ttlDays
    ? new Date(now + parseInt(payload.ttlDays) * 86400000).toISOString()
    : null;

  const record = {
    id:        now,
    code,
    alias:     payload.customAlias || null,
    longUrl:   payload.longUrl,
    shortUrl:  `${Config.SHORT_URL_PREFIX}/${code}`,
    createdAt: new Date(now).toISOString(),
    expiresAt: expires,
    clicks:    0,
  };

  addLink(record);
  return { ok: true, data: record };
}

/* ══════════════════════════════════════════════════════════
   UI HELPERS
══════════════════════════════════════════════════════════ */
function setLoading(on) {
  const btn     = document.getElementById('shorten-btn');
  const label   = document.getElementById('btn-text');
  const arrow   = document.querySelector('.btn-arrow');
  const spinner = document.getElementById('btn-spinner');

  btn.disabled = on;
  label.textContent = on ? 'Shortening…' : 'Shorten URL';
  arrow?.classList.toggle('hidden', on);
  spinner?.classList.toggle('hidden', !on);
}

function showError(msg) {
  const el = document.getElementById('error-msg');
  el.innerHTML = `⚠️ ${escHtml(msg)}`;
  el.classList.remove('hidden');
  document.getElementById('result-card').classList.add('hidden');
}

function clearError() {
  document.getElementById('error-msg').classList.add('hidden');
}

function escHtml(str) {
  return String(str)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function fmtDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en', { year:'numeric', month:'short', day:'numeric' });
}

function timeSince(iso) {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return fmtDate(iso);
}

/* ══════════════════════════════════════════════════════════
   SHOW RESULT
══════════════════════════════════════════════════════════ */
function showResult(data) {
  const card   = document.getElementById('result-card');
  const urlEl  = document.getElementById('result-short-url');
  const origEl = document.getElementById('result-original');
  const crtEl  = document.getElementById('result-created');
  const expBdg = document.getElementById('result-expiry-badge');
  const clicksEl = document.getElementById('result-clicks');

  const shortUrl = data.shortUrl || `${Config.SHORT_URL_PREFIX}/${data.code}`;

  urlEl.href        = data.longUrl; // actual redirect target
  urlEl.textContent = shortUrl;

  origEl.textContent = data.longUrl.length > 60
    ? data.longUrl.slice(0, 57) + '…'
    : data.longUrl;
  origEl.title = data.longUrl;

  crtEl.textContent = 'Created ' + timeSince(data.createdAt || new Date().toISOString());
  clicksEl.textContent = (data.clicks ?? 0).toLocaleString();

  if (data.expiresAt) {
    expBdg.textContent = `Expires ${fmtDate(data.expiresAt)}`;
    expBdg.classList.remove('hidden');
  } else {
    expBdg.classList.add('hidden');
  }

  // Reset copy button
  const copyBtn = document.getElementById('copy-btn');
  copyBtn.classList.remove('done');
  copyBtn.querySelector('.copy-label').textContent = 'Copy';
  copyBtn.querySelector('.copy-icon-def').classList.remove('hidden');
  copyBtn.querySelector('.copy-icon-done').classList.add('hidden');

  // Wire analytics peek button
  const analyticsBtn = document.getElementById('analytics-peek-btn');
  analyticsBtn.onclick = () => {
    document.getElementById('analytics-code-input').value = data.code;
    document.getElementById('analytics-section').scrollIntoView({ behavior: 'smooth' });
    setTimeout(() => window.loadAnalytics?.(data.code), 600);
  };

  card.classList.remove('hidden');
  clearError();
  lastCreated = data;
}

/* ══════════════════════════════════════════════════════════
   RECENT LINKS SIDEBAR
══════════════════════════════════════════════════════════ */
function renderRecentLinks() {
  const list  = document.getElementById('recent-list');
  const empty = list?.querySelector('.recent-empty');
  if (!list) return;

  const links = getStoredLinks().slice(0, 6);
  if (links.length === 0) return;

  list.innerHTML = links.map(l => `
    <div class="recent-link-item">
      <div class="rli-code">
        <a href="${escHtml(l.longUrl)}" target="_blank" rel="noopener" title="Open original URL">
          ${escHtml(Config.SHORT_URL_PREFIX + '/' + l.code)}
        </a>
      </div>
      <div class="rli-orig" title="${escHtml(l.longUrl)}">${escHtml(l.longUrl)}</div>
      <div class="rli-clicks">🖱 <strong>${(l.clicks || 0).toLocaleString()}</strong> clicks · ${timeSince(l.createdAt)}</div>
    </div>
  `).join('');
}

/* ══════════════════════════════════════════════════════════
   COPY TO CLIPBOARD
══════════════════════════════════════════════════════════ */
function initCopyButton() {
  const copyBtn = document.getElementById('copy-btn');
  if (!copyBtn) return;

  copyBtn.addEventListener('click', async () => {
    if (!lastCreated) return;
    const shortUrl = lastCreated.shortUrl || `${Config.SHORT_URL_PREFIX}/${lastCreated.code}`;

    try {
      await navigator.clipboard.writeText(shortUrl);
    } catch {
      // Fallback for non-HTTPS
      const ta = document.createElement('textarea');
      ta.value = shortUrl;
      ta.style.cssText = 'position:fixed;opacity:0;top:0;left:0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    }

    // Visual confirmation
    copyBtn.classList.add('done');
    copyBtn.querySelector('.copy-label').textContent = 'Copied!';
    copyBtn.querySelector('.copy-icon-def').classList.add('hidden');
    copyBtn.querySelector('.copy-icon-done').classList.remove('hidden');
    window.showToast?.('Short URL copied to clipboard!', 'ok');

    setTimeout(() => {
      copyBtn.classList.remove('done');
      copyBtn.querySelector('.copy-label').textContent = 'Copy';
      copyBtn.querySelector('.copy-icon-def').classList.remove('hidden');
      copyBtn.querySelector('.copy-icon-done').classList.add('hidden');
    }, 2500);
  });
}

/* ══════════════════════════════════════════════════════════
   ALIAS TOGGLE
══════════════════════════════════════════════════════════ */
function initAliasToggle() {
  const checkbox  = document.getElementById('alias-checkbox');
  const group     = document.getElementById('alias-group');
  const aliasInput = document.getElementById('custom-alias');

  checkbox?.addEventListener('change', () => {
    group?.classList.toggle('hidden', !checkbox.checked);
    if (checkbox.checked) {
      aliasInput?.focus();
    } else {
      if (aliasInput) aliasInput.value = '';
    }
  });
}

/* ══════════════════════════════════════════════════════════
   MAIN FORM HANDLER
══════════════════════════════════════════════════════════ */
async function handleShortenSubmit(e) {
  e.preventDefault();

  const longUrl = document.getElementById('long-url').value.trim();
  const alias   = document.getElementById('custom-alias')?.value.trim() || '';
  const ttlDays = document.getElementById('demo-ttl')?.value || '';

  clearError();

  // Client-side validation
  if (!isValidUrl(longUrl)) {
    showError('Please enter a valid HTTP or HTTPS URL (e.g. https://example.com).');
    return;
  }
  if (alias && !/^[a-zA-Z0-9\-_]{2,30}$/.test(alias)) {
    showError('Alias must be 2–30 characters: letters, digits, hyphens, and underscores only.');
    return;
  }

  setLoading(true);

  const payload = {
    longUrl,
    customAlias: alias || null,
    ttlDays:     ttlDays ? parseInt(ttlDays) : null,
  };

  let result;

  if (Config.FORCE_DEMO_MODE) {
    // Artificial delay for demo realism
    await new Promise(r => setTimeout(r, 600));
    result = simulateShorten(payload);
    isDemoMode = true;
  } else {
    result = await callShortenApi(payload);

    if (!result.ok && result.error === 'NETWORK') {
      // Backend unreachable → fall back to demo mode silently
      isDemoMode = true;
      await new Promise(r => setTimeout(r, 400));
      result = simulateShorten(payload);
    }
  }

  setLoading(false);

  if (!result.ok) {
    showError(result.error || 'Something went wrong. Please try again.');
    return;
  }

  showResult(result.data);
  renderRecentLinks();
  updateHeroStats();
  window.showToast?.('Short link created!', 'ok');

  // Scroll to result on mobile
  if (window.innerWidth < 768) {
    setTimeout(() => {
      document.getElementById('result-card')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 300);
  }
}

/* ══════════════════════════════════════════════════════════
   HERO STATS — LIVE FETCH
══════════════════════════════════════════════════════════ */
async function updateHeroStats() {
  // Always update from local storage first (instant)
  const local = getStoredLinks();
  const localStats = {
    totalLinks:  local.length,
    totalClicks: local.reduce((s, l) => s + (l.clicks || 0), 0),
    activeLinks: local.filter(l => !l.expiresAt || new Date(l.expiresAt) > new Date()).length,
  };

  // If we have local data, mix it with demo baseline
  const demo = Config.DEMO_STATS;
  const stats = {
    totalLinks:  demo.totalLinks  + localStats.totalLinks,
    totalClicks: demo.totalClicks + localStats.totalClicks,
    activeLinks: demo.activeLinks + localStats.activeLinks,
  };

  window.startCounters?.(stats);

  // Try to also fetch from real API
  if (!Config.FORCE_DEMO_MODE && !isDemoMode) {
    try {
      const controller = new AbortController();
      const timeout    = setTimeout(() => controller.abort(), 4000);
      const res        = await fetch(`${Config.API_BASE_URL}/actuator/health`, { signal: controller.signal });
      clearTimeout(timeout);

      if (res.ok) {
        // Backend is up — try to get real stats
        const statsRes = await fetch(`${Config.API_BASE_URL}/api/stats`, { signal: new AbortController().signal });
        if (statsRes.ok) {
          const data = await statsRes.json();
          if (data?.data) window.startCounters?.(data.data);
        }
      }
    } catch (_) {
      // Silently ignore — demo stats already displayed
    }
  }
}

/* ══════════════════════════════════════════════════════════
   API DOCS ACCORDION
══════════════════════════════════════════════════════════ */
window.toggleEndpoint = function (header) {
  const card = header.closest('.api-endpoint-card');
  if (!card) return;

  const isOpen = card.classList.contains('is-open');

  // Close all
  document.querySelectorAll('.api-endpoint-card.is-open').forEach(c => {
    if (c !== card) c.classList.remove('is-open');
  });

  card.classList.toggle('is-open', !isOpen);
};

// Open first endpoint by default
function initApiDocs() {
  const first = document.querySelector('.api-endpoint-card');
  first?.classList.add('is-open');
}

/* ══════════════════════════════════════════════════════════
   SMOOTH SCROLL HELPER (used in HTML onclick)
══════════════════════════════════════════════════════════ */
window.scrollToEl = function (selector) {
  document.querySelector(selector)?.scrollIntoView({ behavior: 'smooth' });
};

/* ══════════════════════════════════════════════════════════
   URL INPUT LIVE VALIDATION FEEDBACK
══════════════════════════════════════════════════════════ */
function initInputFeedback() {
  const input = document.getElementById('long-url');
  const group = document.getElementById('url-input-group');
  if (!input || !group) return;

  let debounce;
  input.addEventListener('input', () => {
    clearTimeout(debounce);
    const val = input.value.trim();
    if (!val) { group.style.borderColor = ''; return; }

    debounce = setTimeout(() => {
      if (isValidUrl(val)) {
        group.style.borderColor = 'rgba(16,185,129,.6)';
        group.style.boxShadow   = '0 0 0 3px rgba(16,185,129,.1)';
      } else {
        group.style.borderColor = 'rgba(239,68,68,.4)';
        group.style.boxShadow   = '0 0 0 3px rgba(239,68,68,.08)';
      }
    }, 400);
  });

  input.addEventListener('blur', () => {
    group.style.borderColor = '';
    group.style.boxShadow   = '';
  });
}

/* ══════════════════════════════════════════════
   DYNAMIC PREFIX UPDATER
   ══════════════════════════════════════════════ */
function updateVisualPrefixes() {
  const host = window.location.protocol.startsWith('http')
    ? window.location.host
    : 'localhost:8080';
  
  const displayHost = host + '/';
  
  document.querySelectorAll('.alias-prefix, .lookup-prefix').forEach(el => {
    el.textContent = displayHost;
  });
}

/* ══════════════════════════════════════════════
   BOOT
   ══════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  // Update UI visual prefixes dynamically
  updateVisualPrefixes();

  // Form
  document.getElementById('shorten-form')?.addEventListener('submit', handleShortenSubmit);

  // UI init
  initCopyButton();
  initAliasToggle();
  initApiDocs();
  initInputFeedback();

  // Render recent links in sidebar
  renderRecentLinks();

  // Hero stats (with slight delay so counters are in view)
  setTimeout(updateHeroStats, 800);

  console.log(
    '%c⚡ LinkSnap%c Showcase loaded\n%cBackend: %s | Demo: %s',
    'color:#818cf8;font-size:1.2rem;font-weight:bold',
    'color:#94a3b8;font-size:1rem',
    'color:#4b5563;font-size:.875rem',
    Config.API_BASE_URL,
    Config.FORCE_DEMO_MODE ? 'Forced ON' : 'Auto-detect',
  );
});
