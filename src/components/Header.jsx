import { useEffect, useRef } from 'react';
import { Dice4, Monitor, Search, Sparkles, Tv, X } from 'lucide-react';
import Logo from './Logo';

export default function Header({
  searchQuery,
  onSearchChange,
  resultCount,
  totalCount,
  onReset,
  onFeelingLucky,
  shortcutsEnabled = true,
  tvMode = false,
  onToggleTvMode,
}) {
  const inputRef = useRef(null);

  useEffect(() => {
    if (!shortcutsEnabled) return undefined;

    const handleKeyDown = (event) => {
      const element = event.target;
      const isTyping =
        element instanceof HTMLElement &&
        (element.tagName === 'INPUT' ||
          element.tagName === 'TEXTAREA' ||
          element.isContentEditable);

      if (
        event.key === '/' &&
        !isTyping &&
        !event.metaKey &&
        !event.ctrlKey &&
        !event.altKey
      ) {
        event.preventDefault();
        inputRef.current?.focus();
        inputRef.current?.select();
      }

      if (event.key === 'Escape' && document.activeElement === inputRef.current) {
        onSearchChange('');
        inputRef.current?.blur();
      }

      if (
        event.key === 'l' &&
        !isTyping &&
        !event.metaKey &&
        !event.ctrlKey &&
        !event.altKey
      ) {
        event.preventDefault();
        onFeelingLucky?.();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onSearchChange, onFeelingLucky, shortcutsEnabled]);

  const searchField = (
    <form
      role="search"
      onSubmit={(event) => event.preventDefault()}
      className="relative w-full"
    >
      <Search
        aria-hidden="true"
        className={`pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-white/35 ${tvMode ? 'h-6 w-6' : 'h-4 w-4'}`}
      />
      <input
        ref={inputRef}
        type="search"
        value={searchQuery}
        onChange={(event) => onSearchChange(event.target.value)}
        placeholder="Search ideas, concepts, creators…"
        aria-label="Search videos"
        aria-keyshortcuts="/"
        className={`${tvMode ? 'h-14 text-lg pl-14 pr-24' : 'h-10 text-sm pl-11 pr-20'} w-full rounded-full border border-white/10 bg-white/[0.045] text-white placeholder:text-white/30 transition focus:border-white/25 focus:bg-white/[0.07] focus:outline-none focus:ring-2 focus:ring-white/10 [&::-webkit-search-cancel-button]:hidden`}
      />
      {searchQuery ? (
        <button
          type="button"
          onClick={() => {
            onSearchChange('');
            inputRef.current?.focus();
          }}
          aria-label="Clear search"
          className={`absolute right-2.5 top-1/2 inline-flex items-center justify-center rounded-full text-white/40 transition hover:bg-white/10 hover:text-white ${tvMode ? 'h-8 w-8 -translate-y-1/2' : 'h-6 w-6 -translate-y-1/2'}`}
        >
          <X className={tvMode ? 'h-5 w-5' : 'h-3.5 w-3.5'} />
        </button>
      ) : (
        <kbd
          aria-hidden="true"
          className={`pointer-events-none absolute right-3 top-1/2 hidden -translate-y-1/2 rounded border border-white/10 bg-white/[0.04] font-sans font-medium text-white/30 sm:block ${tvMode ? 'px-2 py-1 text-sm' : 'px-1.5 py-0.5 text-[10px]'}`}
        >
          /
        </kbd>
      )}
    </form>
  );

  return (
    <div className="mx-auto max-w-[1600px] px-4 sm:px-6 lg:px-8">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-3 py-3 sm:h-16 sm:flex-nowrap sm:gap-6 sm:py-0">
        <button
          type="button"
          onClick={onReset}
          className="shrink-0 rounded-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-white/40 focus-visible:ring-offset-2 focus-visible:ring-offset-[#08090a]"
          aria-label="STEMStream home — reset filters"
        >
          <Logo />
        </button>

        <div className="order-3 flex w-full min-w-0 justify-center sm:order-2 sm:w-auto sm:flex-1">
          <div className="w-full max-w-xl">{searchField}</div>
        </div>

        <div className="order-2 ml-auto flex shrink-0 items-center gap-3 sm:order-3">
          <button
            type="button"
            onClick={onFeelingLucky}
            disabled={resultCount === 0}
            className={`inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.035] font-medium text-white/50 transition hover:border-white/20 hover:bg-white/[0.08] hover:text-white disabled:opacity-40 disabled:hover:border-white/10 disabled:hover:bg-white/[0.035] disabled:hover:text-white/50 focus:outline-none focus-visible:ring-2 focus-visible:ring-white/40 ${tvMode ? 'px-4 py-2 text-sm' : 'px-3 py-1.5 text-xs'}`}
            aria-label="Play a random video"
            title="Play a random video (press L)"
          >
            <Dice4 className={tvMode ? 'h-5 w-5' : 'h-3.5 w-3.5'} aria-hidden="true" />
            <span className={tvMode ? 'hidden sm:inline' : 'hidden sm:inline'}>I'm feeling lucky</span>
          </button>
          <button
            type="button"
            onClick={onToggleTvMode}
            className={`inline-flex items-center gap-2 rounded-full border font-medium transition focus:outline-none focus-visible:ring-2 focus-visible:ring-white/40 ${
              tvMode
                ? 'border-emerald-500/50 bg-emerald-500/10 text-emerald-400 hover:border-emerald-500/70 hover:bg-emerald-500/15'
                : 'border-white/10 bg-white/[0.035] text-white/50 hover:border-white/20 hover:bg-white/[0.08] hover:text-white'
            } ${tvMode ? 'px-4 py-2 text-sm' : 'px-3 py-1.5 text-xs'}`}
            aria-label={`Toggle TV mode (${tvMode ? 'on' : 'off'})`}
            title={tvMode ? 'Exit TV mode' : 'Enter TV mode'}
          >
            {tvMode ? <Tv className="h-5 w-5" aria-hidden="true" /> : <Monitor className="h-4 w-4" aria-hidden="true" />}
            <span className="hidden sm:inline">{tvMode ? 'TV Mode' : 'TV Mode'}</span>
          </button>
          <span className={`hidden items-center gap-2 rounded-full border border-white/10 bg-white/[0.035] text-white/50 lg:inline-flex ${tvMode ? 'px-4 py-2 text-sm' : 'px-3 py-1.5 text-xs'}`}>
            <Sparkles className={tvMode ? 'h-5 w-5 text-amber-300/80' : 'h-3.5 w-3.5 text-amber-300/80'} aria-hidden="true" />
            Human-curated
          </span>
          <span className={`inline-flex items-center gap-1.5 text-white/40 ${tvMode ? 'text-sm' : 'text-xs'}`}>
            <span className="relative flex h-1.5 w-1.5">
              <span className="ambient-ping absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />
              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-400" />
            </span>
            <span className="tabular-nums">
              {resultCount} / {totalCount}
            </span>
          </span>
        </div>
      </div>

      <p className="sr-only" aria-live="polite">
        {resultCount} of {totalCount} videos shown
      </p>
    </div>
  );
}
