/**
 * config.js — Configuration
 *
 * This is a standalone frontend-only application.
 * All links are stored in your browser's localStorage.
 *
 * To connect a real Spring Boot backend later, set:
 *   FORCE_DEMO_MODE: false
 *   API_BASE_URL: 'https://your-backend.com'
 */
const Config = {
  /**
   * Backend API base URL (only used when FORCE_DEMO_MODE is false).
   */
  API_BASE_URL: 'http://localhost:8080',

  /**
   * The short URL prefix shown to users in the UI.
   */
  SHORT_URL_PREFIX: 'linksnap.app',

  /**
   * Timeout for API calls in milliseconds.
   */
  API_TIMEOUT_MS: 8000,

  /**
   * TRUE  → pure frontend mode, all data in localStorage (default).
   * FALSE → calls the Spring Boot REST API; falls back if unreachable.
   */
  FORCE_DEMO_MODE: true,

  /**
   * Baseline stats displayed in the hero counters.
   */
  DEMO_STATS: {
    totalLinks:   1284,
    totalClicks:  47392,
    activeLinks:  918,
  },
};

window.Config = Config;
