import { useEffect, useRef } from 'react';
import {
  Clock3,
  ExternalLink,
  Gauge,
  Lightbulb,
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

export default function ModalPlayer({ video, onClose, tvMode = false }) {
  const dialogRef = useRef(null);
  const closeButtonRef = useRef(null);
  const meta = getCategoryMeta(video.category);
  const CategoryIcon = meta.icon;

  useEffect(() => {
    const previouslyFocused = document.activeElement;
    const previousOverflow = document.body.style.overflow;

    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();

    // Request full screen in TV mode
    if (tvMode && dialogRef.current) {
      dialogRef.current.requestFullscreen?.().catch(() => {
        // Full screen might be denied, continue without it
      });
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }

      if (event.key !== 'Tab' || !dialogRef.current) return;

      const focusable = Array.from(
        dialogRef.current.querySelectorAll(FOCUSABLE_SELECTOR),
      ).filter((element) => element.offsetParent !== null || element.tagName === 'IFRAME');

      if (focusable.length === 0) {
        event.preventDefault();
        dialogRef.current.focus();
        return;
      }

      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (!dialogRef.current.contains(document.activeElement)) {
        event.preventDefault();
        first.focus();
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
      // Exit full screen if we're in TV mode
      if (tvMode && document.fullscreenElement) {
        document.exitFullscreen?.().catch(() => {
          // Ignore errors when exiting full screen
        });
      }
    };
  }, [onClose, tvMode]);

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto overscroll-contain bg-black/85 p-3 backdrop-blur-md animate-fade-in sm:p-6"
      onMouseDown={onClose}
    >
      <div className="flex min-h-full items-center justify-center">
        <div
          ref={dialogRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="video-modal-title"
          tabIndex={-1}
          onMouseDown={(event) => event.stopPropagation()}
          className={`flex max-h-[calc(100dvh_-_1.5rem)] w-full flex-col overflow-hidden rounded-2xl border border-white/10 bg-[#101114] shadow-[0_32px_100px_rgba(0,0,0,0.85)] outline-none animate-scale-up ${tvMode ? 'max-w-7xl' : 'max-w-5xl'}`}
        >
          <div className="relative aspect-video w-full shrink-0 bg-black">
            <iframe
              src={`https://www.youtube-nocookie.com/embed/${video.youtubeId}?autoplay=1&rel=0&modestbranding=1&playsinline=1`}
              title={video.title}
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerPolicy="strict-origin-when-cross-origin"
              allowFullScreen
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
              className={`absolute right-3 top-3 z-10 inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-black/55 text-white/80 backdrop-blur-md transition hover:bg-white hover:text-[#0b0c0e] focus:outline-none focus-visible:ring-2 focus-visible:ring-white/60 ${tvMode ? 'px-4 py-3' : 'px-2.5 py-2'}`}
            >
              <X className={tvMode ? 'h-6 w-6' : 'h-4 w-4'} aria-hidden="true" />
              <span className={`hidden font-semibold sm:inline ${tvMode ? 'text-sm' : 'text-[11px]'}`}>Esc</span>
            </button>
          </div>

          <div className={`min-h-0 flex-1 overflow-y-auto ${tvMode ? 'p-8' : 'p-5 sm:p-7'}`}>
            <div className="flex flex-wrap items-center gap-2">
              <span
                className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 font-semibold uppercase tracking-[0.1em] ${meta.chip} ${tvMode ? 'text-sm' : 'text-[10px]'}`}
              >
                <CategoryIcon className={tvMode ? 'h-4 w-4' : 'h-3 w-3'} aria-hidden="true" />
                {video.category}
              </span>
              <span className={`inline-flex items-center gap-1.5 rounded-full border border-white/10 bg-white/[0.04] px-2.5 py-1 font-medium uppercase tracking-[0.08em] text-white/45 ${tvMode ? 'text-sm' : 'text-[10px]'}`}>
                <Gauge className={tvMode ? 'h-4 w-4' : 'h-3 w-3'} aria-hidden="true" />
                {video.difficulty}
              </span>
              <span className={`inline-flex items-center gap-1.5 rounded-full border border-white/10 bg-white/[0.04] px-2.5 py-1 font-medium uppercase tracking-[0.08em] text-white/45 ${tvMode ? 'text-sm' : 'text-[10px]'}`}>
                <Clock3 className={tvMode ? 'h-4 w-4' : 'h-3 w-3'} aria-hidden="true" />
                {video.duration}
              </span>
            </div>

            <h2
              id="video-modal-title"
              className={`mt-4 text-balance font-display font-bold leading-tight tracking-[-0.02em] text-white ${tvMode ? 'text-3xl sm:text-4xl lg:text-5xl' : 'text-xl sm:text-2xl lg:text-3xl'}`}
            >
              {video.title}
            </h2>

            {video.coreQuestion && (
              <p className={`mt-2 text-pretty leading-relaxed text-white/45 ${tvMode ? 'text-lg sm:text-xl' : 'text-sm sm:text-[15px]'}`}>
                {video.coreQuestion}
              </p>
            )}

            <p className={`mt-3 text-white/40 ${tvMode ? 'text-sm' : 'text-xs'}`}>
              <span className="font-semibold uppercase tracking-[0.14em] text-white/25">
                Presented by
              </span>{' '}
              <span className="font-medium text-white/65">{video.channel}</span>
            </p>

            <p className={`mt-5 max-w-3xl text-pretty leading-7 text-white/65 ${tvMode ? 'text-lg' : 'text-sm'}`}>
              {video.description}
            </p>

            {video.concepts?.length > 0 && (
              <div className="mt-5">
                <h3 className={`font-semibold uppercase tracking-[0.18em] text-white/30 ${tvMode ? 'text-xs' : 'text-[10px]'}`}>
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
              <div className={`mt-5 flex gap-3 rounded-xl border border-amber-300/15 bg-amber-300/[0.055] p-4 ${tvMode ? 'p-6' : ''}`}>
                <Lightbulb
                  className={`mt-0.5 shrink-0 text-amber-300/80 ${tvMode ? 'h-6 w-6' : 'h-4 w-4'}`}
                  aria-hidden="true"
                />
                <div>
                  <h3 className={`font-semibold uppercase tracking-[0.18em] text-amber-200/60 ${tvMode ? 'text-xs' : 'text-[10px]'}`}>
                    The takeaway
                  </h3>
                  <p className={`mt-1 leading-relaxed text-white/70 ${tvMode ? 'text-lg' : 'text-sm'}`}>
                    {video.takeaway}
                  </p>
                </div>
              </div>
            )}

            <div className={`mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-white/[0.06] pt-4 ${tvMode ? 'pt-6' : ''}`}>
              <a
                href={`https://www.youtube.com/watch?v=${video.youtubeId}`}
                target="_blank"
                rel="noopener noreferrer"
                className={`inline-flex items-center gap-1.5 rounded-md font-medium text-white/45 transition hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50 ${tvMode ? 'text-sm' : 'text-xs'}`}
              >
                Watch on YouTube
                <ExternalLink className={tvMode ? 'h-5 w-5' : 'h-3.5 w-3.5'} aria-hidden="true" />
              </a>
              <span className={`inline-flex items-center gap-1.5 text-white/25 ${tvMode ? 'text-sm' : 'text-[11px]'}`}>
                <ShieldCheck className={tvMode ? 'h-5 w-5' : 'h-3.5 w-3.5'} aria-hidden="true" />
                Privacy-enhanced YouTube playback
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
