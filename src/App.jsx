import { useCallback, useDeferredValue, useMemo, useState } from 'react';
import {
  ArrowUp,
  Atom,
  Film,
  Layers3,
  RotateCcw,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import Header from './components/Header';
import CategoryFilter from './components/CategoryFilter';
import VideoGrid from './components/VideoGrid';
import ModalPlayer from './components/ModalPlayer';
import Logo from './components/Logo';
import { CATEGORIES } from './lib/categories';
import { useMyList, useWatched } from './lib/personal';
import videos from './data/data.json';
import { getInitialTvMode, saveTvModePreference } from './lib/tv';

const VIDEOS_BY_ID = new Map(videos.map((video) => [video.id, video]));

/** Catalogue records for a stored id list, newest first, skipping removed ids. */
function videosForIds(ids) {
  return ids.map((id) => VIDEOS_BY_ID.get(id)).filter(Boolean);
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

      <div className="ambient-spin absolute inset-0 animate-[spin_60s_linear_infinite]">
        <span className="absolute left-1/2 top-0 h-2 w-2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-sky-400 shadow-[0_0_24px_6px_rgba(56,189,248,0.35)]" />
      </div>
      <div className="ambient-spin absolute inset-10 animate-[spin_42s_linear_infinite_reverse]">
        <span className="absolute right-0 top-1/2 h-2.5 w-2.5 translate-x-1/2 -translate-y-1/2 rounded-full bg-pink-400 shadow-[0_0_26px_7px_rgba(244,114,182,0.3)]" />
      </div>
      <div className="ambient-spin absolute inset-20 animate-[spin_52s_linear_infinite]">
        <span className="absolute bottom-0 left-1/2 h-2 w-2 -translate-x-1/2 translate-y-1/2 rounded-full bg-amber-300 shadow-[0_0_24px_6px_rgba(252,211,77,0.3)]" />
      </div>

      <div className="absolute inset-[5.5rem] rounded-full bg-red-500/10 blur-3xl" />
      <div className="relative flex h-28 w-28 items-center justify-center rounded-full border border-white/10 bg-white/[0.04] shadow-[0_0_80px_rgba(255,0,51,0.16)]">
        <Atom className="h-12 w-12 text-white/75" strokeWidth={1.1} />
      </div>
    </div>
  );
}

const PAGE_SIZE = 24;

// Search haystacks are built once per video and reused. Rebuilding them inside
// the filter loop would allocate a string per video on every keystroke, which is
// noticeable on low-powered TVs and phones.
const searchHaystacks = new WeakMap();

function getSearchableText(video) {
  let haystack = searchHaystacks.get(video);
  if (haystack === undefined) {
    haystack = [
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
    searchHaystacks.set(video, haystack);
  }
  return haystack;
}

export default function App() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState('All');
  const [selectedVideo, setSelectedVideo] = useState(null);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  // Typing stays instant; the (much heavier) result list catches up at a lower
  // priority so a slow device never blocks the search input.
  const deferredSearchQuery = useDeferredValue(searchQuery);
  // Televisions start in TV mode; every other device starts in desktop mode
  // unless the viewer has made an explicit choice.
  const [tvMode, setTvMode] = useState(() => getInitialTvMode());
  const [tvFocusZone, setTvFocusZone] = useState('grid');

  const {
    watchedIds,
    watched,
    markWatched,
    toggleWatched,
    clearWatched,
  } = useWatched();
  const { savedIds, saved, toggleSaved } = useMyList();

  const isListView = activeFilter === 'My List';
  const isWatchedView = activeFilter === 'Watched';

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
      // Opening the player is the "watched" signal. From then on the video is
      // filed under Watched instead of the main collection.
      markWatched(video.id);
      setSelectedVideo(video);
    },
    [tvMode, markWatched],
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

  const toggleSavedVideo = useCallback(
    (video) => toggleSaved(video.id),
    [toggleSaved],
  );

  const toggleWatchedVideo = useCallback(
    (video) => toggleWatched(video.id),
    [toggleWatched],
  );

  const changeSearch = useCallback((value) => {
    setSearchQuery(value);
    setVisibleCount(PAGE_SIZE);
  }, []);

  const changeFilter = useCallback((filter) => {
    setActiveFilter(filter);
    setVisibleCount(PAGE_SIZE);
    // A new filter is a different list, so the old scroll offset would leave
    // the viewport parked on a row that no longer exists. Bring the collection
    // top back into view so the first video is what you see. The section
    // already carries the sticky-header scroll margin.
    document.getElementById('collection')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  }, []);

  const resetFilters = useCallback(() => {
    setSearchQuery('');
    setActiveFilter('All');
    setVisibleCount(PAGE_SIZE);
  }, []);

  // Category counts describe what the collection will actually show, so a
  // watched video stops counting towards its discipline.
  const filterCounts = useMemo(() => {
    const counts = {
      All: 0,
      'My List': savedIds.length,
      Watched: watchedIds.length,
    };

    for (const category of CATEGORIES) {
      if (category !== 'All') counts[category] = 0;
    }

    for (const video of videos) {
      if (watched.has(video.id)) continue;
      counts.All += 1;
      if (counts[video.category] !== undefined) counts[video.category] += 1;
    }

    return counts;
  }, [savedIds.length, watched, watchedIds.length]);

  const filteredVideos = useMemo(() => {
    let source;

    if (isWatchedView) {
      source = videosForIds(watchedIds);
    } else if (isListView) {
      source = videosForIds(savedIds);
    } else {
      source = videos.filter(
        (video) =>
          !watched.has(video.id) &&
          (activeFilter === 'All' || video.category === activeFilter),
      );
    }

    const query = deferredSearchQuery.trim().toLowerCase();
    if (!query) return source;

    const tokens = query.split(/\s+/);
    return source.filter((video) => {
      const haystack = getSearchableText(video);
      return tokens.every((token) => haystack.includes(token));
    });
  }, [
    activeFilter,
    deferredSearchQuery,
    isListView,
    isWatchedView,
    savedIds,
    watched,
    watchedIds,
  ]);

  const playRandomVideo = useCallback(() => {
    if (filteredVideos.length === 0) return;
    const randomIndex = Math.floor(Math.random() * filteredVideos.length);
    selectVideo(filteredVideos[randomIndex]);
  }, [filteredVideos, selectVideo]);

  const visibleVideos = useMemo(
    () => filteredVideos.slice(0, visibleCount),
    [filteredVideos, visibleCount],
  );

  const hasMore = visibleCount < filteredVideos.length;

  const loadMore = useCallback(() => {
    setVisibleCount((count) => Math.min(count + PAGE_SIZE, filteredVideos.length));
  }, [filteredVideos.length]);

  const hasActiveFilters = searchQuery.trim() !== '' || activeFilter !== 'All';
  const collectionTitle = isListView
    ? 'My list'
    : isWatchedView
      ? 'Watched'
      : activeFilter === 'All'
        ? 'Start anywhere. Get curious.'
        : activeFilter;

  const disciplineCount = CATEGORIES.length - 1;
  const collectionSummary = searchQuery.trim()
    ? `${isListView ? 'My list' : isWatchedView ? 'Watched' : activeFilter === 'All' ? 'All videos' : activeFilter} · ${filteredVideos.length} ${filteredVideos.length === 1 ? 'result' : 'results'} for “${searchQuery.trim()}”`
    : isListView
      ? `${savedIds.length} ${savedIds.length === 1 ? 'video' : 'videos'} saved for later. Only you can see this list.`
      : isWatchedView
        ? `${watchedIds.length} watched ${watchedIds.length === 1 ? 'video' : 'videos'}, most recent first.`
        : activeFilter === 'All'
          ? watchedIds.length > 0
            ? `${filteredVideos.length} unwatched videos across ${disciplineCount} disciplines. ${watchedIds.length} watched ${watchedIds.length === 1 ? 'video is' : 'videos are'} filed under Watched.`
            : `${filteredVideos.length} videos across ${disciplineCount} disciplines, ordered by editorial pick.`
          : `${filteredVideos.length} unwatched ${activeFilter.toLowerCase()} videos.`;

  const emptyState = isListView
    ? {
        title: 'Your list is empty',
        message:
          'Press the bookmark on any card, or Add to my list in the player, to keep a video here for later.',
      }
    : isWatchedView
      ? {
          title: 'Nothing watched yet',
          message:
            'A video moves here as soon as you play it, so the main collection stays focused on what is next.',
        }
      : watchedIds.length > 0
        ? {
            title: 'You are all caught up',
            message:
              'Everything here has been watched and moved to the Watched section. Open it to rewatch a video or put one back.',
          }
        : {
            title: 'Nothing in this corner of the universe',
            message:
              'Try a different phrase, or clear the filters and browse the full collection.',
          };

  return (
    <div
      id="top"
      className={`min-h-screen bg-[#08090a] text-white ${selectedVideo ? 'is-playing' : ''}`}
    >
      <a
        href="#collection"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[60] focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-black"
      >
        Skip to the collection
      </a>

      <div className="sticky top-0 z-40 border-b border-white/[0.06] bg-[#08090a]">
        <Header
          searchQuery={searchQuery}
          onSearchChange={changeSearch}
          resultCount={filteredVideos.length}
          totalCount={videos.length}
          onReset={resetFilters}
          onFeelingLucky={playRandomVideo}
          shortcutsEnabled={!selectedVideo}
          tvMode={tvMode}
          onToggleTvMode={toggleTvMode}
        />
        <CategoryFilter
          activeFilter={activeFilter}
          counts={filterCounts}
          onChange={changeFilter}
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
                    <span className="ambient-ping absolute inline-flex h-full w-full animate-ping rounded-full bg-red-400 opacity-60" />
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
                    {disciplineCount} disciplines
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
                {isListView ? 'Saved by you' : isWatchedView ? 'Your history' : 'The collection'}
              </p>
              <h2
                id="collection-heading"
                className="mt-2 font-display text-2xl font-bold tracking-[-0.025em] text-white sm:text-3xl"
              >
                {collectionTitle}
              </h2>
              <p className="mt-2 max-w-2xl text-sm leading-relaxed text-white/45">
                {collectionSummary}
              </p>
            </div>

            <div className="flex shrink-0 flex-wrap items-center gap-2 self-start sm:self-auto">
              {isWatchedView && watchedIds.length > 0 && (
                <button
                  type="button"
                  onClick={() => {
                    if (
                      window.confirm(
                        'Clear the watched history? The videos return to the main collection.',
                      )
                    ) {
                      clearWatched();
                    }
                  }}
                  className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-3.5 py-2 text-xs font-medium text-white/55 transition hover:border-white/20 hover:bg-white/[0.08] hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50"
                >
                  <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
                  Clear history
                </button>
              )}

              {hasActiveFilters && (
                <button
                  type="button"
                  onClick={resetFilters}
                  className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-3.5 py-2 text-xs font-medium text-white/55 transition hover:border-white/20 hover:bg-white/[0.08] hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50"
                >
                  <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
                  Reset filters
                </button>
              )}
            </div>
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
              saved={saved}
              watched={watched}
              onToggleSaved={toggleSavedVideo}
              onToggleWatched={toggleWatchedVideo}
              emptyState={emptyState}
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
        <ModalPlayer
          video={selectedVideo}
          onClose={closeVideo}
          tvMode={tvMode}
          saved={saved.has(selectedVideo.id)}
          watched={watched.has(selectedVideo.id)}
          onToggleSaved={toggleSavedVideo}
          onToggleWatched={toggleWatchedVideo}
        />
      )}
    </div>
  );
}
