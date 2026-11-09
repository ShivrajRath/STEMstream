import { useCallback, useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { FILTERS, getCategoryMeta } from '../lib/categories';

export default function CategoryFilter({
  activeFilter,
  counts,
  onChange,
  tvMode = false,
  keyboardEnabled = true,
  focused = false,
  onNavigateToVideos,
}) {
  const scrollRef = useRef(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
  // `All`, the disciplines and the personal collections (My List, Watched)
  // share one row, so a remote can reach each of them with left/right.
  const [focusedIndex, setFocusedIndex] = useState(-1);

  const updateScrollState = useCallback(() => {
    const element = scrollRef.current;
    if (!element) return;

    const maxScroll = element.scrollWidth - element.clientWidth;
    setCanScrollLeft(element.scrollLeft > 4);
    setCanScrollRight(maxScroll > 4 && element.scrollLeft < maxScroll - 4);
  }, []);

  useEffect(() => {
    const element = scrollRef.current;
    if (!element) return undefined;

    updateScrollState();
    element.addEventListener('scroll', updateScrollState, { passive: true });

    const observer = new ResizeObserver(updateScrollState);
    observer.observe(element);

    return () => {
      element.removeEventListener('scroll', updateScrollState);
      observer.disconnect();
    };
  }, [updateScrollState]);

  // Keep a valid chip highlighted whenever TV mode or the active category
  // changes.
  useEffect(() => {
    if (!tvMode) {
      setFocusedIndex(-1);
      return;
    }

    setFocusedIndex((current) => {
      if (current >= 0 && current < FILTERS.length) return current;
      const activeIndex = FILTERS.indexOf(activeFilter);
      return activeIndex >= 0 ? activeIndex : 0;
    });
  }, [tvMode, activeFilter]);

  // TV mode keyboard navigation only runs while the category row owns focus,
  // so arrow keys cannot move the categories and the video grid at once.
  useEffect(() => {
    if (!tvMode || !keyboardEnabled || !focused) return undefined;

    const handleKeyDown = (event) => {
      if (event.defaultPrevented) return;

      switch (event.key) {
        case 'ArrowRight':
          event.preventDefault();
          setFocusedIndex((prev) => Math.min(prev + 1, FILTERS.length - 1));
          break;
        case 'ArrowLeft':
          event.preventDefault();
          setFocusedIndex((prev) => Math.max(prev - 1, 0));
          break;
        case 'ArrowDown':
          event.preventDefault();
          onNavigateToVideos?.();
          break;
        case 'Enter':
        case ' ':
          if (focusedIndex >= 0 && focusedIndex < FILTERS.length) {
            event.preventDefault();
            onChange(FILTERS[focusedIndex]);
          }
          break;
        default:
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [tvMode, keyboardEnabled, focused, focusedIndex, onChange, onNavigateToVideos]);

  // Keep the highlighted chip inside the horizontally scrolling row.
  useEffect(() => {
    if (!tvMode || focusedIndex < 0) return;
    const chips = scrollRef.current?.querySelectorAll('[data-category-chip]');
    chips?.[focusedIndex]?.scrollIntoView({
      behavior: 'smooth',
      inline: 'nearest',
      block: 'nearest',
    });
  }, [tvMode, focusedIndex]);

  const scrollBy = (direction) => {
    scrollRef.current?.scrollBy({
      left: direction * 260,
      behavior: 'smooth',
    });
  };

  return (
    <div className="border-t border-white/[0.045]">
      <div className="relative mx-auto max-w-[1600px] px-4 sm:px-6 lg:px-8">
        <div
          ref={scrollRef}
          className={`no-scrollbar flex items-center gap-2 overflow-x-auto ${tvMode ? 'py-4' : 'py-2.5'}`}
          role="group"
          aria-label="Filter videos by discipline, my list or watched"
        >
          {FILTERS.map((category, index) => {
            const meta = getCategoryMeta(category);
            const Icon = meta.icon;
            const active = activeFilter === category;
            const isFocused = tvMode && focused && focusedIndex === index;

            return (
              <button
                key={category}
                type="button"
                aria-pressed={active}
                data-category-chip
                onClick={() => onChange(category)}
                className={`group inline-flex shrink-0 items-center gap-2 rounded-full border font-medium transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50 focus-visible:ring-offset-2 focus-visible:ring-offset-[#08090a] ${
                  tvMode
                    ? 'px-5 py-2.5 text-base'
                    : 'px-3.5 py-1.5 text-[13px]'
                } ${
                  active
                    ? 'border-white bg-white text-[#0b0c0e] shadow-[0_2px_12px_rgba(255,255,255,0.12)]'
                    : 'border-white/[0.08] bg-white/[0.035] text-white/60 hover:border-white/20 hover:bg-white/[0.07] hover:text-white'
                } ${
                  isFocused && tvMode ? 'ring-2 ring-emerald-400 ring-offset-2 ring-offset-[#08090a]' : ''
                }`}
              >
                <Icon
                  aria-hidden="true"
                  className={tvMode ? 'h-5 w-5' : 'h-3.5 w-3.5'}
                />
                <span>{category}</span>
                <span
                  className={`rounded-full font-semibold tabular-nums ${
                    tvMode
                      ? 'px-2 py-0.5 text-xs'
                      : 'px-1.5 py-0.5 text-[10px]'
                  } ${
                    active
                      ? 'bg-black/10 text-black/60'
                      : 'bg-white/[0.06] text-white/30 group-hover:text-white/50'
                  }`}
                >
                  {counts[category] ?? 0}
                </span>
              </button>
            );
          })}
        </div>

        {canScrollLeft && (
          <>
            <div
              aria-hidden="true"
              className="pointer-events-none absolute inset-y-0 left-0 w-14 bg-gradient-to-r from-[#08090a] via-[#08090a]/80 to-transparent"
            />
            <button
              type="button"
              onClick={() => scrollBy(-1)}
              aria-label="Scroll categories left"
              className="absolute left-2 top-1/2 z-10 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full border border-white/10 bg-[#17181c]/90 text-white/60 shadow-lg transition hover:bg-white hover:text-[#0b0c0e] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50"
            >
              <ChevronLeft className="h-4 w-4" aria-hidden="true" />
            </button>
          </>
        )}

        {canScrollRight && (
          <>
            <div
              aria-hidden="true"
              className="pointer-events-none absolute inset-y-0 right-0 w-16 bg-gradient-to-l from-[#08090a] via-[#08090a]/80 to-transparent"
            />
            <button
              type="button"
              onClick={() => scrollBy(1)}
              aria-label="Scroll categories right"
              className="absolute right-2 top-1/2 z-10 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full border border-white/10 bg-[#17181c]/90 text-white/60 shadow-lg transition hover:bg-white hover:text-[#0b0c0e] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50"
            >
              <ChevronRight className="h-4 w-4" aria-hidden="true" />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
