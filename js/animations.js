/**
 * animations.js — Visual Effects & Scroll Animations
 *
 * Handles:
 *  - Particle canvas background
 *  - Intersection Observer scroll reveal
 *  - Animated number counters
 *  - Scroll progress bar
 *  - Navbar scroll state
 *  - Theme toggle
 */
'use strict';

/* ══════════════════════════════════════════════════════════
   PARTICLE CANVAS BACKGROUND
══════════════════════════════════════════════════════════ */
(function initParticles() {
  const canvas = document.getElementById('particle-canvas');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  let W, H, particles = [], RAF;

  const COLORS = ['#2563eb', '#3b82f6', '#0ea5e9', '#64748b', '#10b981'];
  const COUNT  = window.innerWidth < 768 ? 40 : 80;

  function resize() {
    W = canvas.width  = window.innerWidth;
    H = canvas.height = window.innerHeight;
  }

  function createParticle() {
    return {
      x:     Math.random() * W,
      y:     Math.random() * H,
      r:     Math.random() * 1.8 + 0.3,
      vx:    (Math.random() - .5) * .35,
      vy:    (Math.random() - .5) * .35,
      alpha: Math.random() * .5 + .1,
      color: COLORS[Math.floor(Math.random() * COLORS.length)],
    };
  }

  function drawLines() {
    const DIST = 130;
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x;
        const dy = particles[i].y - particles[j].y;
        const d  = Math.sqrt(dx * dx + dy * dy);
        if (d < DIST) {
          ctx.beginPath();
          ctx.strokeStyle = `rgba(37,99,235,${(1 - d / DIST) * 0.08})`;
          ctx.lineWidth = .6;
          ctx.moveTo(particles[i].x, particles[i].y);
          ctx.lineTo(particles[j].x, particles[j].y);
          ctx.stroke();
        }
      }
    }
  }

  function tick() {
    ctx.clearRect(0, 0, W, H);

    particles.forEach(p => {
      p.x += p.vx;
      p.y += p.vy;
      if (p.x < 0) p.x = W;
      if (p.x > W) p.x = 0;
      if (p.y < 0) p.y = H;
      if (p.y > H) p.y = 0;

      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fillStyle = p.color + Math.floor(p.alpha * 255).toString(16).padStart(2, '0');
      ctx.fill();
    });

    drawLines();
    RAF = requestAnimationFrame(tick);
  }

  function init() {
    resize();
    particles = Array.from({ length: COUNT }, createParticle);
    tick();
  }

  // Pause when tab hidden for performance
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) cancelAnimationFrame(RAF);
    else tick();
  });

  window.addEventListener('resize', () => {
    resize();
    // Keep particle count proportional on resize
    while (particles.length < COUNT) particles.push(createParticle());
    while (particles.length > COUNT) particles.pop();
  }, { passive: true });

  init();
})();


/* ══════════════════════════════════════════════════════════
   SCROLL PROGRESS BAR
══════════════════════════════════════════════════════════ */
(function initScrollProgress() {
  const bar = document.getElementById('scroll-bar');
  if (!bar) return;

  window.addEventListener('scroll', () => {
    const scrollTop = document.documentElement.scrollTop;
    const scrollH   = document.documentElement.scrollHeight - document.documentElement.clientHeight;
    bar.style.width  = (scrollH > 0 ? (scrollTop / scrollH) * 100 : 0) + '%';
  }, { passive: true });
})();


/* ══════════════════════════════════════════════════════════
   NAVBAR SCROLL STATE
══════════════════════════════════════════════════════════ */
(function initNavbar() {
  const navbar = document.getElementById('navbar');
  if (!navbar) return;

  window.addEventListener('scroll', () => {
    navbar.classList.toggle('scrolled', window.scrollY > 60);
  }, { passive: true });

  // Active link highlighting
  const navLinks = document.querySelectorAll('.nav-link');
  const sections = Array.from(navLinks)
    .map(link => {
      const id = link.getAttribute('href')?.replace('#', '');
      return id ? document.getElementById(id) : null;
    })
    .filter(Boolean);

  const io = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        navLinks.forEach(l => l.classList.remove('active'));
        const active = document.querySelector(`.nav-link[href="#${entry.target.id}"]`);
        if (active) active.classList.add('active');
      }
    });
  }, { rootMargin: '-50% 0px -50% 0px' });

  sections.forEach(s => io.observe(s));
})();


/* ══════════════════════════════════════════════════════════
   SCROLL REVEAL ANIMATIONS
══════════════════════════════════════════════════════════ */
(function initScrollReveal() {
  const elements = document.querySelectorAll('[data-animate]');
  if (!elements.length) return;

  // Apply delay
  elements.forEach(el => {
    const delay = el.dataset.delay || 0;
    el.style.transitionDelay = delay + 'ms';
  });

  const io = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        io.unobserve(entry.target);
      }
    });
  }, {
    threshold: 0.1,
    rootMargin: '0px 0px -60px 0px',
  });

  elements.forEach(el => io.observe(el));
})();


/* ══════════════════════════════════════════════════════════
   ANIMATED NUMBER COUNTERS
══════════════════════════════════════════════════════════ */
/**
 * Animates a number from `start` to `end` over `duration` ms.
 * Uses easeOutExpo for a natural deceleration feel.
 */
function animateCounter(el, start, end, duration = 1500) {
  if (!el || start === end) return;
  const startTime = performance.now();

  function easeOutExpo(t) {
    return t === 1 ? 1 : 1 - Math.pow(2, -10 * t);
  }

  function step(now) {
    const elapsed  = now - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const value    = Math.round(start + (end - start) * easeOutExpo(progress));
    el.textContent = value.toLocaleString();
    if (progress < 1) requestAnimationFrame(step);
  }

  requestAnimationFrame(step);
}

/**
 * Initialise hero stat counters.
 * Called with real values from main.js after API fetch, or with demo values.
 */
window.startCounters = function ({ totalLinks = 0, totalClicks = 0, activeLinks = 0 } = {}) {
  const els = {
    total:   document.getElementById('stat-total'),
    clicks:  document.getElementById('stat-clicks'),
    active:  document.getElementById('stat-active'),
  };

  // Only animate once
  if (els.total?.dataset.animated) return;
  if (els.total) els.total.dataset.animated = '1';

  animateCounter(els.total,  0, totalLinks,  1800);
  animateCounter(els.clicks, 0, totalClicks, 2200);
  animateCounter(els.active, 0, activeLinks, 1500);
};

// Trigger counters when hero stats come into view
(function watchHeroStats() {
  const statsEl = document.querySelector('.hero-stats');
  if (!statsEl) return;

  const io = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        // Will be triggered again by main.js with real data
        // Kick off with zeros in case API hasn't responded yet
        window.startCounters();
        io.unobserve(entry.target);
      }
    });
  }, { threshold: 0.5 });

  io.observe(statsEl);
})();


/* ══════════════════════════════════════════════════════════
   THEME TOGGLE (Dark / Light)
══════════════════════════════════════════════════════════ */
(function initTheme() {
  const btn      = document.getElementById('theme-toggle');
  const iconMoon = btn?.querySelector('.icon-moon');
  const iconSun  = btn?.querySelector('.icon-sun');
  const html     = document.documentElement;

  // Restore saved theme
  const saved = localStorage.getItem('ls_theme') || 'dark';
  setTheme(saved);

  btn?.addEventListener('click', () => {
    const next = html.dataset.theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    localStorage.setItem('ls_theme', next);
  });

  function setTheme(theme) {
    html.dataset.theme = theme;
    if (iconMoon && iconSun) {
      iconMoon.classList.toggle('hidden', theme === 'light');
      iconSun.classList.toggle('hidden',  theme === 'dark');
    }
  }
})();


/* ══════════════════════════════════════════════════════════
   TOAST NOTIFICATIONS
══════════════════════════════════════════════════════════ */
window.showToast = function (message, type = '', duration = 3500) {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const icons = { ok: '✅', err: '❌', warn: '⚠️', '': 'ℹ️' };
  const toast = document.createElement('div');
  toast.className = `toast${type ? ` toast-${type}` : ''}`;
  toast.innerHTML = `<span>${icons[type] ?? 'ℹ️'}</span><span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('toast-fade-out');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
  }, duration);
};
