import { useEffect, useRef, useState } from 'react';
import { RotateCcw, Telescope } from 'lucide-react';
import VideoCard from './VideoCard';

export default function VideoGrid({
  videos,
  onSelectVideo,
  onReset,
  tvMode = false,
  keyboardEnabled = true,
  focused = false,
  onNavigateToCategories,
}) {
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const didNavigateRef = useRef(false);

  // Must match the Tailwind grid classes below, or vertical navigation drifts.
  const getColumns = () => {
    if (tvMode) {
      if (window.innerWidth >= 1280) return 3;
      if (window.innerWidth >= 1024) return 2;
      return 1;
    }
    if (window.innerWidth >= 1280) return 4;
    if (window.innerWidth >= 1024) return 3;
    if (window.innerWidth >= 640) return 2;
    return 1;
  };

  // Reset or repair the focused card when TV mode or the visible set changes.
  useEffect(() => {
    if (!tvMode) {
      setFocusedIndex(-1);
      return;
    }

    setFocusedIndex((current) => {
      if (current >= 0 && current < videos.length) return current;
      return videos.length > 0 ? 0 : -1;
    });
  }, [tvMode, videos.length]);

  // The grid handles arrow keys only while it owns focus and the player is
  // closed, so navigation never happens behind an open video.
  useEffect(() => {
    if (!tvMode || !keyboardEnabled || !focused) return undefined;

    const handleKeyDown = (event) => {
      if (event.defaultPrevented) return;

      const total = videos.length;
      if (total === 0) return;

      const columns = getColumns();
      const current = focusedIndex >= 0 ? focusedIndex : 0;

      const moveTo = (nextIndex) => {
        event.preventDefault();
        didNavigateRef.current = true;
        setFocusedIndex(nextIndex);
      };

      switch (event.key) {
        case 'ArrowRight':
          if (current < total - 1) moveTo(current + 1);
          break;
        case 'ArrowLeft':
          if (current > 0) moveTo(current - 1);
          break;
        case 'ArrowDown': {
          const next = current + columns;
          if (next < total) moveTo(next);
          break;
        }
        case 'ArrowUp': {
          const previous = current - columns;
          if (previous >= 0) {
            moveTo(previous);
          } else {
            event.preventDefault();
            onNavigateToCategories?.();
          }
          break;
        }
        case 'Enter':
        case ' ':
          if (focusedIndex >= 0 && focusedIndex < total) {
            event.preventDefault();
            onSelectVideo(videos[focusedIndex]);
          }
          break;
        default:
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [
    tvMode,
    keyboardEnabled,
    focused,
    focusedIndex,
    videos,
    onSelectVideo,
    onNavigateToCategories,
  ]);

  // Follow the focused card with minimal scrolling. The initial focus should
  // not yank the page down when TV mode is switched on.
  useEffect(() => {
    if (!tvMode || focusedIndex < 0 || !didNavigateRef.current) return;
    const cards = document.querySelectorAll('[data-video-card]');
    cards[focusedIndex]?.scrollIntoView({
      behavior: 'smooth',
      inline: 'nearest',
      block: 'nearest',
    });
  }, [focusedIndex, tvMode]);

  if (videos.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-white/10 bg-white/[0.02] px-6 py-20 text-center">
        <span className="flex h-14 w-14 items-center justify-center rounded-full border border-white/10 bg-white/[0.04]">
          <Telescope className="h-6 w-6 text-white/40" aria-hidden="true" />
        </span>
        <h3 className="mt-5 font-display text-lg font-semibold text-white">
          Nothing in this corner of the universe
        </h3>
        <p className="mt-2 max-w-sm text-sm leading-relaxed text-white/40">
          Try a different phrase, or clear the filters and browse the full
          collection.
        </p>
        <button
          type="button"
          onClick={onReset}
          className="mt-6 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/[0.06] px-4 py-2 text-sm font-medium text-white transition hover:bg-white/[0.1] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50"
        >
          <RotateCcw className="h-4 w-4" aria-hidden="true" />
          Reset filters
        </button>
      </div>
    );
  }

  return (
    <div className={`grid gap-x-5 gap-y-8 ${tvMode ? 'grid-cols-1 sm:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3' : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'}`}>
      {videos.map((video, index) => (
        <VideoCard
          key={video.id}
          video={video}
          onSelect={onSelectVideo}
          priority={index < 4}
          tvMode={tvMode}
          isFocused={tvMode && focused && focusedIndex === index}
        />
      ))}
    </div>
  );
}
