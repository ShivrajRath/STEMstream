const TV_MODE_STORAGE_KEY = 'stemstream:tv-mode';

// Smart televisions and streaming sticks usually announce themselves in the
// user agent. Desktop browsers running on a TV-sized display are covered by the
// viewport + input heuristic in `isTvDevice`.
const TV_USER_AGENT_PATTERN =
  /(smart-?tv|smarttv|hbbtv|netcast|webos|web0s|tizen|viera|bravia|aquos|googletv|google tv|android tv|appletv|apple tv|roku|crkey|firetv|fire tv|aft[a-z0-9]{1,3}\b|nettv|opera tv|philipstv|hisense)/i;

function hasTouchInput() {
  return navigator.maxTouchPoints > 0 || 'ontouchstart' in window;
}

/**
 * True when the current browser looks like a television or a TV streaming
 * device. Detection is intentionally conservative: a large desktop monitor
 * with a mouse stays in desktop mode.
 */
export function isTvDevice() {
  if (typeof window === 'undefined') return false;

  if (TV_USER_AGENT_PATTERN.test(navigator.userAgent ?? '')) return true;

  if (/\btv\b/i.test(navigator.userAgentData?.platform ?? '')) return true;

  if (hasTouchInput()) return false;

  const width = window.innerWidth;
  const height = window.innerHeight;

  // Televisions render 16:9 panels at 720p, 1080p or 4K, and browsers usually
  // report CSS pixels of 1280x720 or 1920x1080.
  if (width < 1280 || height < 720) return false;

  const aspectRatio = width / height;
  if (aspectRatio < 1.5 || aspectRatio > 1.95) return false;

  // A remote control has no hover-capable pointer; a desktop with a mouse does.
  const noHover = window.matchMedia?.('(hover: none)')?.matches ?? false;
  const coarsePointer = window.matchMedia?.('(pointer: coarse)')?.matches ?? false;

  return noHover || coarsePointer;
}

/** The viewer's explicit choice, or null when they never toggled TV mode. */
export function loadTvModePreference() {
  try {
    const stored = window.localStorage.getItem(TV_MODE_STORAGE_KEY);
    if (stored === 'on') return true;
    if (stored === 'off') return false;
  } catch {
    // Some TV browsers disable storage; fall back to device detection.
  }
  return null;
}

export function saveTvModePreference(enabled) {
  try {
    window.localStorage.setItem(TV_MODE_STORAGE_KEY, enabled ? 'on' : 'off');
  } catch {
    // Storage is a convenience, not a requirement.
  }
}

/**
 * Default for the first render. Reading it during render (rather than in an
 * effect) keeps the hero banner from flashing before TV mode applies.
 */
export function getInitialTvMode() {
  const stored = loadTvModePreference();
  return stored === null ? isTvDevice() : stored;
}
