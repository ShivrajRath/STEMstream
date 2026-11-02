import { useEffect, useState } from 'react';
import { RotateCcw, Telescope } from 'lucide-react';
import VideoCard from './VideoCard';

export default function VideoGrid({ videos, onSelectVideo, onReset, tvMode = false }) {
  const [focusedIndex, setFocusedIndex] = useState(-1);

  // Determine grid columns based on screen size and TV mode
  const getColumns = () => {
    if (tvMode) {
      if (window.innerWidth >= 1280) return 3; // xl
      if (window.innerWidth >= 1024) return 2; // lg
      return 1; // sm
    }
    if (window.innerWidth >= 1280) return 4; // xl
    if (window.innerWidth >= 1024) return 3; // lg
    if (window.innerWidth >= 640) return 2; // sm
    return 1;
  };

  useEffect(() => {
    if (!tvMode) {
      setFocusedIndex(-1);
      return;
    }

    // Set initial focus to first video when TV mode is enabled
    if (focusedIndex === -1 && videos.length > 0) {
      setFocusedIndex(0);
    }

    const handleKeyDown = (event) => {
      const columns = getColumns();
      const rows = Math.ceil(videos.length / columns);

      switch (event.key) {
        case 'ArrowRight':
          event.preventDefault();
          setFocusedIndex((prev) => {
            if (prev < videos.length - 1) return prev + 1;
            return prev;
          });
          break;
        case 'ArrowLeft':
          event.preventDefault();
          setFocusedIndex((prev) => {
            if (prev > 0) return prev - 1;
            return prev;
          });
          break;
        case 'ArrowDown':
          event.preventDefault();
          setFocusedIndex((prev) => {
            const newRow = Math.floor(prev / columns) + 1;
            const newCol = prev % columns;
            const newIndex = newRow * columns + newCol;
            if (newIndex < videos.length) return newIndex;
            return prev;
          });
          break;
        case 'ArrowUp':
          event.preventDefault();
          setFocusedIndex((prev) => {
            const newRow = Math.floor(prev / columns) - 1;
            const newCol = prev % columns;
            const newIndex = newRow * columns + newCol;
            if (newRow >= 0) return newIndex;
            return prev;
          });
          break;
        case 'Enter':
          setFocusedIndex((prev) => {
            if (prev >= 0 && prev < videos.length) {
              event.preventDefault();
              onSelectVideo(videos[prev]);
            }
            return prev;
          });
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [tvMode, videos, onSelectVideo]);

  // Scroll focused video into view
  useEffect(() => {
    if (tvMode && focusedIndex >= 0) {
      const cards = document.querySelectorAll('[data-video-card]');
      const focusedCard = cards[focusedIndex];
      if (focusedCard) {
        focusedCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
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
    <div className={`grid gap-x-5 gap-y-8 ${tvMode ? 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3' : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'}`}>
      {videos.map((video, index) => (
        <VideoCard
          key={video.id}
          video={video}
          onSelect={onSelectVideo}
          priority={index < 4}
          tvMode={tvMode}
          isFocused={tvMode && focusedIndex === index}
        />
      ))}
    </div>
  );
}
