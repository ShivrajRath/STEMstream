import { useCallback, useEffect, useMemo, useState } from 'react';

const WATCHED_KEY = 'stemstream:watched';
const LIST_KEY = 'stemstream:my-list';

function readIds(key) {
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((id) => typeof id === 'string') : [];
  } catch {
    return [];
  }
}

function writeIds(key, ids) {
  try {
    window.localStorage.setItem(key, JSON.stringify(ids));
  } catch {
    // Private modes and some TV browsers refuse storage. The session still
    // works; only persistence across reloads is lost.
  }
}

/**
 * An ordered list of video ids, newest first, mirrored into localStorage.
 *
 * Both personal collections (watched history and My List) are the same shape,
 * so they share this one implementation. The stored value is deliberately just
 * ids: the catalogue is the source of truth for everything else, so a video
 * that disappears from a later curation commit simply stops matching instead of
 * leaving a stale copy behind.
 */
function useStoredIds(key) {
  const [ids, setIds] = useState(() => readIds(key));

  useEffect(() => {
    writeIds(key, ids);
  }, [key, ids]);

  const add = useCallback((id) => {
    setIds((current) => (current.includes(id) ? current : [id, ...current]));
  }, []);

  const toggle = useCallback((id) => {
    setIds((current) =>
      current.includes(id)
        ? current.filter((value) => value !== id)
        : [id, ...current],
    );
  }, []);

  const clear = useCallback(() => setIds([]), []);

  const set = useMemo(() => new Set(ids), [ids]);

  return { ids, set, add, toggle, clear };
}

/** Videos that have been played. They move out of the collection into Watched. */
export function useWatched() {
  const { ids, set, add, toggle, clear } = useStoredIds(WATCHED_KEY);
  return {
    watchedIds: ids,
    watched: set,
    markWatched: add,
    toggleWatched: toggle,
    clearWatched: clear,
  };
}

/** The viewer's own list, saved by hand. */
export function useMyList() {
  const { ids, set, toggle } = useStoredIds(LIST_KEY);
  return { savedIds: ids, saved: set, toggleSaved: toggle };
}
