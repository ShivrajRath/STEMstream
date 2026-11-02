import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Clock3,
  ExternalLink,
  Gauge,
  Info,
  Lightbulb,
  Maximize2,
  Minimize2,
  ShieldCheck,
  X,
} from 'lucide-react';
import { getCategoryMeta } from '../lib/categories';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'iframe',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

// `fullscreen` must be in the allow list or the embedded player's own
// fullscreen control is blocked by the iframe sandboxing rules.
const IFRAME_ALLOW =
  'accelerometer; autoplay; clipboard-write; encrypted-media; fullscreen; gyroscope; picture-in-picture; web-share';

// Embeds start with captions off, but YouTube still shows them when the
// viewer's own caption preference is on, so the player is asked to unload its
// captions module as well. That command is only accepted once the player is
// ready and needs the JavaScript API enabled and the embedding origin set.
function buildEmbedUrl(youtubeId) {
  const params = new URLSearchParams({
    autoplay: '1',
    rel: '0',
    modestbranding: '1',
    playsinline: '1',
    fs: '1',
    cc_load_policy: '0',
    enablejsapi: '1',
  });

  if (/^https?:$/.test(window.location.protocol)) {
    params.set('origin', window.location.origin);
  }

  return `https://www.youtube-nocookie.com/embed/${youtubeId}?${params.toString()}`;
}

function VideoEmbed({ videoId, title, className }) {
  const iframeRef = useRef(null);

  useEffect(() => {
    const iframe = iframeRef.current;
    if (!iframe) return undefined;

    let cancelled = false;

    const hideCaptions = () => {
      if (cancelled) return;
      iframe.contentWindow?.postMessage(
        JSON.stringify({
          event: 'command',
          func: 'unloadModule',
          args: ['captions'],
        }),
        '*',
      );
    };

    const handleMessage = (event) => {
      if (event.source !== iframe.contentWindow) return;
      if (typeof event.data !== 'string') return;

      let payload;
      try {
        payload = JSON.parse(event.data);
      } catch {
        return;
      }

      if (payload?.event === 'onReady' || payload?.event === 'initialDelivery') {
        hideCaptions();
      }
    };

    window.addEventListener('message', handleMessage);
    iframe.addEventListener('load', hideCaptions);

    return () => {
      cancelled = true;
      window.removeEventListener('message', handleMessage);
      iframe.removeEventListener('load', hideCaptions);
    };
  }, [videoId]);

  return (
    <iframe
      ref={iframeRef}
      src={buildEmbedUrl(videoId)}
      title={title}
      allow={IFRAME_ALLOW}
      referrerPolicy="strict-origin-when-cross-origin"
      allowFullScreen
      className={className}
    />
  );
}

export default function ModalPlayer({ video, onClose, tvMode = false }) {
  const dialogRef = useRef(null);
  const detailsRef = useRef(null);
  const closeButtonRef = useRef(null);
  const showDetailsRef = useRef(false);
  const [showDetails, setShowDetails] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(() =>
    Boolean(document.fullscreenElement),
  );

  const meta = getCategoryMeta(video.category);
  const CategoryIcon = meta.icon;

  useEffect(() => {
    showDetailsRef.current = showDetails;
  }, [showDetails]);

  const toggleFullscreen = useCallback(() => {
    if (document.fullscreenElement) {
      document.exitFullscreen?.().catch(() => {
        // The browser may already be leaving full screen.
      });
    } else {
      document.documentElement.requestFullscreen?.().catch(() => {
        // Fullscreen can be denied; the CSS player still fills the viewport.
      });
    }
  }, []);

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(Boolean(document.fullscreenElement));
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () =>
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  useEffect(() => {
    const previouslyFocused = document.activeElement;
    const previousOverflow = document.body.style.overflow;

    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        // While full screen, let the browser consume the first Escape to leave
        // fullscreen; the player stays open and the next Escape closes it.
        if (document.fullscreenElement && !showDetailsRef.current) return;

        event.preventDefault();
        if (showDetailsRef.current) {
          setShowDetails(false);
          return;
        }
        onClose();
        return;
      }

      if (tvMode && (event.key === 'i' || event.key === 'I')) {
        event.preventDefault();
        setShowDetails((current) => !current);
        return;
      }

      if (event.key !== 'Tab' || !dialogRef.current) return;

      const focusable = Array.from(
        dialogRef.current.querySelectorAll(FOCUSABLE_SELECTOR),
      ).filter(
        (element) => element.offsetParent !== null || element.tagName === 'IFRAME',
      );

      if (focusable.length === 0) {
        event.preventDefault();
        dialogRef.current.focus();
        return;
      }

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const focusedIndex = focusable.indexOf(document.activeElement);

      if (focusedIndex === -1) {
        event.preventDefault();
        (event.shiftKey ? last : first).focus();
      } else if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
      if (previouslyFocused instanceof HTMLElement) {
        previouslyFocused.focus();
      }
      if (document.fullscreenElement) {
        document.exitFullscreen?.().catch(() => {
          // The browser may already be leaving full screen.
        });
      }
    };
  }, [onClose, tvMode]);

  // Focus the info panel so remote-control arrow keys scroll it.
  useEffect(() => {
    if (!showDetails) return;
    detailsRef.current?.focus();
  }, [showDetails]);

  const details = (
    <>
      <div className="flex flex-wrap items-center gap-2">
        <span
          className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 font-semibold uppercase tracking-[0.1em] ${meta.chip} ${tvMode ? 'text-sm' : 'text-[10px]'}`}
        >
          <CategoryIcon
            className={tvMode ? 'h-4 w-4' : 'h-3 w-3'}
            aria-hidden="true"
          />
          {video.category}
        </span>
        <span
          className={`inline-flex items-center gap-1.5 rounded-full border border-white/10 bg-white/[0.04] px-2.5 py-1 font-medium uppercase tracking-[0.08em] text-white/45 ${tvMode ? 'text-sm' : 'text-[10px]'}`}
        >
          <Gauge
            className={tvMode ? 'h-4 w-4' : 'h-3 w-3'}
            aria-hidden="true"
          />
          {video.difficulty}
        </span>
        <span
          className={`inline-flex items-center gap-1.5 rounded-full border border-white/10 bg-white/[0.04] px-2.5 py-1 font-medium uppercase tracking-[0.08em] text-white/45 ${tvMode ? 'text-sm' : 'text-[10px]'}`}
        >
          <Clock3
            className={tvMode ? 'h-4 w-4' : 'h-3 w-3'}
            aria-hidden="true"
          />
          {video.duration}
        </span>
      </div>

      <h2
        className={`mt-4 text-balance font-display font-bold leading-tight tracking-[-0.02em] text-white ${tvMode ? 'text-3xl sm:text-4xl lg:text-5xl' : 'text-xl sm:text-2xl lg:text-3xl'}`}
      >
        {video.title}
      </h2>

      {video.coreQuestion && (
        <p
          className={`mt-2 text-pretty leading-relaxed text-white/45 ${tvMode ? 'text-lg sm:text-xl' : 'text-sm sm:text-[15px]'}`}
        >
          {video.coreQuestion}
        </p>
      )}

      <p className={`mt-3 text-white/40 ${tvMode ? 'text-sm' : 'text-xs'}`}>
        <span className="font-semibold uppercase tracking-[0.14em] text-white/25">
          Presented by
        </span>{' '}
        <span className="font-medium text-white/65">{video.channel}</span>
      </p>

      <p
        className={`mt-5 max-w-3xl text-pretty leading-7 text-white/65 ${tvMode ? 'text-lg' : 'text-sm'}`}
      >
        {video.description}
      </p>

      {video.concepts?.length > 0 && (
        <div className="mt-5">
          <h3
            className={`font-semibold uppercase tracking-[0.18em] text-white/30 ${tvMode ? 'text-xs' : 'text-[10px]'}`}
          >
            Key ideas
          </h3>
          <ul className="mt-2 flex flex-wrap gap-1.5">
            {video.concepts.map((concept) => (
              <li
                key={concept}
                className={`rounded-md border border-white/[0.08] bg-white/[0.035] px-2 py-1 text-white/55 ${tvMode ? 'text-sm' : 'text-[11px]'}`}
              >
                {concept}
              </li>
            ))}
          </ul>
        </div>
      )}

      {video.takeaway && (
        <div
          className={`mt-5 flex gap-3 rounded-xl border border-amber-300/15 bg-amber-300/[0.055] p-4 ${tvMode ? 'p-6' : ''}`}
        >
          <Lightbulb
            className={`mt-0.5 shrink-0 text-amber-300/80 ${tvMode ? 'h-6 w-6' : 'h-4 w-4'}`}
            aria-hidden="true"
          />
          <div>
            <h3
              className={`font-semibold uppercase tracking-[0.18em] text-amber-200/60 ${tvMode ? 'text-xs' : 'text-[10px]'}`}
            >
              The takeaway
            </h3>
            <p
              className={`mt-1 leading-relaxed text-white/70 ${tvMode ? 'text-lg' : 'text-sm'}`}
            >
              {video.takeaway}
            </p>
          </div>
        </div>
      )}

      <div
        className={`mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-white/[0.06] pt-4 ${tvMode ? 'pt-6' : ''}`}
      >
        <a
          href={`https://www.youtube.com/watch?v=${video.youtubeId}`}
          target="_blank"
          rel="noopener noreferrer"
          className={`inline-flex items-center gap-1.5 rounded-md font-medium text-white/45 transition hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50 ${tvMode ? 'text-sm' : 'text-xs'}`}
        >
          Watch on YouTube
          <ExternalLink
            className={tvMode ? 'h-5 w-5' : 'h-3.5 w-3.5'}
            aria-hidden="true"
          />
        </a>
        <span
          className={`inline-flex items-center gap-1.5 text-white/25 ${tvMode ? 'text-sm' : 'text-[11px]'}`}
        >
          <ShieldCheck
            className={tvMode ? 'h-5 w-5' : 'h-3.5 w-3.5'}
            aria-hidden="true"
          />
          Privacy-enhanced YouTube playback
        </span>
      </div>
    </>
  );

  // TV mode: an edge-to-edge player that fills the screen. The metadata moves
  // into an optional info panel (i) so the video owns the whole viewport.
  if (tvMode) {
    return (
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={video.title}
        tabIndex={-1}
        className="fixed inset-0 z-50 flex h-[100dvh] w-screen flex-col overflow-hidden bg-black outline-none"
      >
        <div
          className="relative flex min-h-0 flex-1 items-center justify-center"
          onMouseDown={onClose}
        >
          <div
            className="relative aspect-video max-h-full w-full"
            style={{ maxWidth: 'min(100vw, calc(100dvh * 16 / 9))' }}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <VideoEmbed
              videoId={video.youtubeId}
              title={video.title}
              className="absolute inset-0 h-full w-full border-0"
            />
          </div>

          {showDetails && (
            <div
              ref={detailsRef}
              tabIndex={-1}
              className="absolute inset-x-0 bottom-0 top-24 z-20 overflow-y-auto border-t border-white/10 bg-[#08090a] px-6 py-8 outline-none sm:px-10"
              onMouseDown={(event) => event.stopPropagation()}
            >
              <div className="mx-auto max-w-4xl">{details}</div>
            </div>
          )}

          <div className="pointer-events-none absolute inset-x-0 top-0 z-30 flex items-start justify-between gap-6 bg-gradient-to-b from-black/85 via-black/45 to-transparent px-6 pb-16 pt-5">
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-white/45">
                {video.category} · {video.difficulty} · {video.duration}
              </p>
              <h2 className="mt-1.5 line-clamp-1 font-display text-2xl font-bold tracking-[-0.02em] text-white xl:text-3xl">
                {video.title}
              </h2>
            </div>

            <div
              className="pointer-events-auto flex shrink-0 items-center gap-2"
              onMouseDown={(event) => event.stopPropagation()}
            >
              <button
                type="button"
                onClick={() => setShowDetails((current) => !current)}
                aria-pressed={showDetails}
                className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-black/70 px-4 py-2.5 text-sm font-semibold text-white/80 transition hover:bg-white hover:text-[#0b0c0e] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/60"
              >
                <Info className="h-5 w-5" aria-hidden="true" />
                <span className="hidden sm:inline">
                  {showDetails ? 'Hide info' : 'Info'}
                </span>
              </button>
              <button
                type="button"
                onClick={toggleFullscreen}
                aria-label={isFullscreen ? 'Exit full screen' : 'Enter full screen'}
                title={isFullscreen ? 'Exit full screen' : 'Enter full screen'}
                className="inline-flex items-center justify-center rounded-full border border-white/15 bg-black/70 p-2.5 text-white/80 transition hover:bg-white hover:text-[#0b0c0e] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/60"
              >
                {isFullscreen ? (
                  <Minimize2 className="h-5 w-5" aria-hidden="true" />
                ) : (
                  <Maximize2 className="h-5 w-5" aria-hidden="true" />
                )}
              </button>
              <button
                ref={closeButtonRef}
                type="button"
                onClick={onClose}
                aria-label="Close video player"
                className="inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-black/70 px-4 py-2.5 text-sm font-semibold text-white/80 transition hover:bg-white hover:text-[#0b0c0e] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/60"
              >
                <X className="h-6 w-6" aria-hidden="true" />
                <span className="hidden sm:inline">Esc</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto overscroll-contain bg-black/90 p-3 animate-fade-in sm:p-6"
      onMouseDown={onClose}
    >
      <div className="flex min-h-full items-center justify-center">
        <div
          ref={dialogRef}
          role="dialog"
          aria-modal="true"
          aria-label={video.title}
          tabIndex={-1}
          onMouseDown={(event) => event.stopPropagation()}
          className="flex max-h-[calc(100dvh_-_1.5rem)] w-full max-w-5xl flex-col overflow-hidden rounded-2xl border border-white/10 bg-[#101114] shadow-2xl outline-none animate-scale-up"
        >
          <div className="relative aspect-video w-full shrink-0 bg-black">
            <VideoEmbed
              videoId={video.youtubeId}
              title={video.title}
              className="absolute inset-0 h-full w-full border-0"
            />

            <div
              aria-hidden="true"
              className="pointer-events-none absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-black/60 to-transparent"
            />

            <button
              ref={closeButtonRef}
              type="button"
              onClick={onClose}
              aria-label="Close video player"
              className="absolute right-3 top-3 z-10 inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-black/70 px-2.5 py-2 text-white/80 transition hover:bg-white hover:text-[#0b0c0e] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/60"
            >
              <X className="h-4 w-4" aria-hidden="true" />
              <span className="hidden text-[11px] font-semibold sm:inline">Esc</span>
            </button>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto p-5 sm:p-7">
            {details}
          </div>
        </div>
      </div>
    </div>
  );
}
