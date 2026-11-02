import { useCallback, useMemo, useState } from 'react';
import {
  ArrowUp,
  Atom,
  Dice4,
  Film,
  Layers3,
  Monitor,
  RotateCcw,
  ShieldCheck,
  Sparkles,
  Tv,
} from 'lucide-react';
import Header from './components/Header';
import CategoryFilter from './components/CategoryFilter';
import VideoGrid from './components/VideoGrid';
import ModalPlayer from './components/ModalPlayer';
import Logo from './components/Logo';
import { CATEGORIES } from './lib/categories';
import videos from './data/data.json';
import { getInitialTvMode, saveTvModePreference } from './lib/tv';

const categoryCounts = { All: videos.length };

for (const category of CATEGORIES) {
  if (category !== 'All') categoryCounts[category] = 0;
}

for (const video of videos) {
  categoryCounts[video.category] = (categoryCounts[video.category] ?? 0) + 1;
}

function OrbitGraphic() {
  return (
    <div
      aria-hidden="true"
      className="relative hidden h-[280px] w-[280px] shrink-0 items-center justify-center lg:flex xl:h-[320px] xl:w-[320px]"
    >
      <div className="absolute inset-0 rounded-full border border-white/[0.06]" />
      <div className="absolute inset-10 rounded-full border border-white/[0.08]" />
      <div className="absolute inset-20 rounded-full border border-dashed border-white/[0.1]" />

      <div className="absolute inset-0 animate-[spin_60s_linear_infinite]">
        <span className="absolute left-1/2 top-0 h-2 w-2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-sky-400 shadow-[0_0_24px_6px_rgba(56,189,248,0.35)]" />
      </div>
      <div className="absolute inset-10 animate-[spin_42s_linear_infinite_reverse]">
        <span className="absolute right-0 top-1/2 h-2.5 w-2.5 translate-x-1/2 -translate-y-1/2 rounded-full bg-pink-400 shadow-[0_0_26px_7px_rgba(244,114,182,0.3)]" />
      </div>
      <div className="absolute inset-20 animate-[spin_52s_linear_infinite]">
        <span className="absolute bottom-0 left-1/2 h-2 w-2 -translate-x-1/2 translate-y-1/2 rounded-full bg-amber-300 shadow-[0_0_24px_6px_rgba(252,211,77,0.3)]" />
      </div>

      <div className="absolute inset-[5.5rem] rounded-full bg-red-500/10 blur-3xl" />
      <div className="relative flex h-28 w-28 items-center justify-center rounded-full border border-white/10 bg-white/[0.04] shadow-[0_0_80px_rgba(255,0,51,0.16)] backdrop-blur-sm">
        <Atom className="h-12 w-12 text-white/75" strokeWidth={1.1} />
      </div>
    </div>
  );
}

const PAGE_SIZE = 24;

export default function App() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('All');
  const [selectedVideo, setSelectedVideo] = useState(null);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  // Televisions start in TV mode; every other device starts in desktop mode
  // unless the viewer has made an explicit choice.
  const [tvMode, setTvMode] = useState(() => getInitialTvMode());
  const [tvFocusZone, setTvFocusZone] = useState('grid');

  const toggleTvMode = useCallback(() => {
    setTvMode((current) => {
      const next = !current;
      saveTvModePreference(next);
      return next;
    });
    setTvFocusZone('grid');
  }, []);

  const focusVideos = useCallback(() => setTvFocusZone('grid'), []);
  const focusCategories = useCallback(() => setTvFocusZone('categories'), []);

  const selectVideo = useCallback(
    (video) => {
      // Ask for full screen inside the click handler: browsers only grant it
      // while the user activation is fresh. The player itself also fills the
      // viewport, so TVs that refuse the Fullscreen API still get an
      // edge-to-edge video.
      if (tvMode && !document.fullscreenElement) {
        document.documentElement.requestFullscreen?.().catch(() => {
          // Fullscreen is a nice-to-have; never block playback on it.
        });
      }
      setSelectedVideo(video);
    },
    [tvMode],
  );

  const closeVideo = useCallback(() => {
    if (document.fullscreenElement) {
      document.exitFullscreen?.().catch(() => {
        // The browser may already be leaving full screen.
      });
    }
    setSelectedVideo(null);
    setTvFocusZone('grid');
  }, []);

  const resetFilters = useCallback(() => {
    setSearchQuery('');
    setActiveCategory('All');
  }, []);

  const filteredVideos = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    const tokens = query ? query.split(/\s+/) : [];

    return videos.filter((video) => {
      const matchesCategory =
        activeCategory === 'All' || video.category === activeCategory;
      if (!matchesCategory) return false;
      if (tokens.length === 0) return true;

      const haystack = [
        video.title,
        video.coreQuestion,
        video.channel,
        video.category,
        video.difficulty,
        video.description,
        video.takeaway,
        ...(video.concepts ?? []),
      ]
        .join(' ')
        .toLowerCase();

      return tokens.every((token) => haystack.includes(token));
    });
  }, [activeCategory, searchQuery]);

  const playRandomVideo = useCallback(() => {
    if (filteredVideos.length === 0) return;
    const randomIndex = Math.floor(Math.random() * filteredVideos.length);
    selectVideo(filteredVideos[randomIndex]);
  }, [filteredVideos, selectVideo]);

  const filterKey = `${activeCategory}|${searchQuery}`;
  const [trackedFilterKey, setTrackedFilterKey] = useState(filterKey);
  if (filterKey !== trackedFilterKey) {
    setTrackedFilterKey(filterKey);
    setVisibleCount(PAGE_SIZE);
  }

  const visibleVideos = useMemo(
    () => filteredVideos.slice(0, visibleCount),
    [filteredVideos, visibleCount],
  );

  const hasMore = visibleCount < filteredVideos.length;

  const loadMore = useCallback(() => {
    setVisibleCount((count) => Math.min(count + PAGE_SIZE, filteredVideos.length));
  }, [filteredVideos.length]);

  const hasActiveFilters = searchQuery.trim() !== '' || activeCategory !== 'All';
  const collectionTitle =
    activeCategory === 'All' ? 'Start anywhere. Get curious.' : activeCategory;

  return (
    <div id="top" className="min-h-screen bg-[#08090a] text-white">
      <a
        href="#collection"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[60] focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-black"
      >
        Skip to the collection
      </a>

      <div className="sticky top-0 z-40 border-b border-white/[0.06] bg-[#08090a]/85 backdrop-blur-xl">
        <Header
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          resultCount={filteredVideos.length}
          totalCount={videos.length}
          onReset={resetFilters}
          onFeelingLucky={playRandomVideo}
          shortcutsEnabled={!selectedVideo}
          tvMode={tvMode}
          onToggleTvMode={toggleTvMode}
        />
        <CategoryFilter
          activeCategory={activeCategory}
          counts={categoryCounts}
          onChange={setActiveCategory}
          tvMode={tvMode}
          keyboardEnabled={!selectedVideo}
          focused={tvFocusZone === 'categories'}
          onNavigateToVideos={focusVideos}
        />
      </div>

      <main>
        {!tvMode && (
          <section className="relative isolate overflow-hidden border-b border-white/[0.06]">
            <div aria-hidden="true" className="absolute inset-0 -z-10">
              <div className="absolute -left-24 top-[-12rem] h-[32rem] w-[32rem] rounded-full bg-red-600/[0.09] blur-[130px]" />
              <div className="absolute -right-20 top-[-6rem] h-[28rem] w-[28rem] rounded-full bg-violet-600/[0.08] blur-[120px]" />
              <div
                className="absolute inset-0 opacity-40"
                style={{
                  backgroundImage:
                    'linear-gradient(to right, rgba(255,255,255,0.045) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.045) 1px, transparent 1px)',
                  backgroundSize: '72px 72px',
                  maskImage:
                    'radial-gradient(ellipse at center, black 15%, transparent 72%)',
                  WebkitMaskImage:
                    'radial-gradient(ellipse at center, black 15%, transparent 72%)',
                }}
              />
            </div>

            <div className="mx-auto flex max-w-[1600px] flex-col gap-8 px-4 pb-10 pt-10 sm:px-6 sm:pb-12 sm:pt-12 lg:flex-row lg:items-center lg:justify-between lg:gap-8 lg:px-8 lg:py-10">
              <div className="max-w-2xl">
                <span className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-3 py-1.5 text-[11px] font-medium tracking-wide text-white/55">
                  <span className="relative flex h-1.5 w-1.5">
                    <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-red-400 opacity-60" />
                    <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-red-400" />
                  </span>
                  Curated, not algorithmic
                </span>

                <h1 className="mt-5 text-balance font-display text-[clamp(2.5rem,5.5vw,3.75rem)] font-bold leading-[0.98] tracking-[-0.045em] text-white">
                  Feed your{' '}
                  <span className="block bg-gradient-to-r from-[#ff7a66] via-[#ff2d55] to-[#ff9a3c] bg-clip-text text-transparent">
                    curiosity.
                  </span>
                </h1>

                <p className="mt-4 max-w-xl text-pretty text-base leading-relaxed text-white/55">
                  A hand-picked library of the internet's best science, space,
                  engineering and mathematics videos. Chosen for the idea — not the
                  algorithm, and not the channel.
                </p>

                <ul className="mt-6 flex flex-wrap items-center gap-x-6 gap-y-3 text-[13px] text-white/45">
                  <li className="inline-flex items-center gap-2">
                    <Film className="h-4 w-4 text-white/25" aria-hidden="true" />
                    {videos.length} curated videos
                  </li>
                  <li className="inline-flex items-center gap-2">
                    <Layers3 className="h-4 w-4 text-white/25" aria-hidden="true" />
                    {CATEGORIES.length - 1} disciplines
                  </li>
                  <li className="inline-flex items-center gap-2">
                    <Sparkles className="h-4 w-4 text-white/25" aria-hidden="true" />
                    Chosen by humans
                  </li>
                </ul>
              </div>

              <OrbitGraphic />
            </div>
          </section>
        )}

        <section
          id="collection"
          aria-labelledby="collection-heading"
          className="mx-auto max-w-[1600px] scroll-mt-44 px-4 py-8 sm:px-6 sm:py-10 lg:scroll-mt-32 lg:px-8"
        >
          <div className="flex flex-col gap-5 border-b border-white/[0.06] pb-6 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-white/30">
                The collection
              </p>
              <h2
                id="collection-heading"
                className="mt-2 font-display text-2xl font-bold tracking-[-0.025em] text-white sm:text-3xl"
              >
                {collectionTitle}
              </h2>
              <p className="mt-2 max-w-2xl text-sm leading-relaxed text-white/45">
                {searchQuery.trim()
                  ? `Search results for “${searchQuery.trim()}” · ${filteredVideos.length} of ${videos.length} videos`
                  : activeCategory === 'All'
                    ? `${filteredVideos.length} videos across ${CATEGORIES.length - 1} disciplines, ordered by editorial pick.`
                    : `${filteredVideos.length} hand-picked ${activeCategory.toLowerCase()} videos.`}
              </p>
            </div>

            {hasActiveFilters && (
              <button
                type="button"
                onClick={resetFilters}
                className="inline-flex shrink-0 items-center gap-2 self-start rounded-full border border-white/10 bg-white/[0.04] px-3.5 py-2 text-xs font-medium text-white/55 transition hover:border-white/20 hover:bg-white/[0.08] hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50 sm:self-auto"
              >
                <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
                Reset filters
              </button>
            )}
          </div>

          <p className="sr-only" aria-live="polite">
            {filteredVideos.length} videos match your filters.
          </p>

          <div className="mt-6">
            <VideoGrid
              videos={visibleVideos}
              onSelectVideo={selectVideo}
              onReset={resetFilters}
              tvMode={tvMode}
              keyboardEnabled={!selectedVideo}
              focused={tvFocusZone === 'grid'}
              onNavigateToCategories={focusCategories}
            />
          </div>

          {hasMore && (
            <div className="mt-10 flex flex-col items-center gap-3">
              <p className="text-xs text-white/40">
                Showing {visibleVideos.length} of {filteredVideos.length} videos
              </p>
              <button
                type="button"
                onClick={loadMore}
                className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/[0.06] px-5 py-2.5 text-sm font-medium text-white transition hover:border-white/25 hover:bg-white/[0.1] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50"
              >
                Load more
              </button>
            </div>
          )}
        </section>
      </main>

      <footer className="border-t border-white/[0.06] bg-[#090a0c]">
        <div className="mx-auto flex max-w-[1600px] flex-col gap-7 px-4 py-10 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
          <div>
            <Logo size="h-8 w-8" />
            <p className="mt-3 max-w-sm text-xs leading-relaxed text-white/40">
              A small, hand-curated library for people who like understanding how
              things work. Built with React, Vite and a JSON file.
            </p>
          </div>

          <div className="flex flex-col gap-2.5 text-[11px] text-white/35 sm:flex-row sm:flex-wrap sm:items-center sm:gap-5">
            <span className="inline-flex items-center gap-1.5">
              <ShieldCheck className="h-3.5 w-3.5" aria-hidden="true" />
              Privacy-enhanced playback
            </span>
            <span>Videos belong to their creators.</span>
            <a
              href="#top"
              className="inline-flex items-center gap-1.5 text-white/35 transition hover:text-white/70 focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50"
            >
              Back to top
              <ArrowUp className="h-3.5 w-3.5" aria-hidden="true" />
            </a>
          </div>
        </div>
      </footer>

      {selectedVideo && (
        <ModalPlayer video={selectedVideo} onClose={closeVideo} tvMode={tvMode} />
      )}
    </div>
  );
}
