import { memo, useState } from 'react';
import { Bookmark, Check, Clock3, Play } from 'lucide-react';
import { getCategoryMeta } from '../lib/categories';
import {
  THUMBNAIL_SIZES,
  thumbnailSrcSet,
  thumbnailUrl,
} from '../lib/thumbnails';

function VideoCard({
  video,
  onSelect,
  priority = false,
  tvMode = false,
  isFocused = false,
  saved = false,
  watched = false,
  onToggleSaved,
  onToggleWatched,
}) {
  const meta = getCategoryMeta(video.category);
  const Icon = meta.icon;

  // `srcset` lets the browser pick the cheapest rendition that fits the card.
  // maxresdefault is not generated for every upload, so an error drops back to
  // hqdefault (which almost always exists) and finally to a styled placeholder.
  const [thumbnailStage, setThumbnailStage] = useState(0);
  const thumbnailUrlFallback = thumbnailUrl(video.youtubeId, 'hqdefault');

  return (
    <div
      data-video-card
      className={`group relative block w-full ${
        isFocused && tvMode
          ? 'rounded-2xl ring-2 ring-emerald-400 ring-offset-2 ring-offset-[#08090a]'
          : ''
      }`}
    >
      <button
        type="button"
        onClick={() => onSelect(video)}
        aria-label={`Play ${video.title} by ${video.channel}`}
        className="block w-full rounded-2xl text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50 focus-visible:ring-offset-4 focus-visible:ring-offset-[#08090a]"
      >
        <div className="relative aspect-video w-full overflow-hidden rounded-xl border border-white/[0.08] bg-[#141518] shadow-[0_1px_0_rgba(255,255,255,0.04)_inset] transition-colors duration-300 group-hover:border-white/20">
          {thumbnailStage < 2 ? (
            <img
              src={thumbnailUrlFallback}
              srcSet={thumbnailStage === 0 ? thumbnailSrcSet(video.youtubeId) : undefined}
              sizes={thumbnailStage === 0 ? THUMBNAIL_SIZES : undefined}
              alt={`Thumbnail for ${video.title}`}
              width="1280"
              height="720"
              onError={() => setThumbnailStage((stage) => stage + 1)}
              loading={priority ? 'eager' : 'lazy'}
              fetchPriority={priority ? 'high' : undefined}
              decoding="async"
              className="h-full w-full object-cover transition-transform duration-500 ease-out group-hover:scale-[1.045]"
            />
          ) : (
            <div className="flex h-full w-full flex-col items-center justify-center gap-2 bg-[#141518]">
              <Icon className={`h-8 w-8 ${meta.text}`} aria-hidden="true" />
              <span className="text-[10px] font-semibold uppercase tracking-[0.16em] text-white/30">
                {video.category}
              </span>
            </div>
          )}

          <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/60 via-black/0 to-black/10 opacity-80"
          />

          <span className={`absolute bottom-2.5 right-2.5 inline-flex items-center gap-1 rounded-md bg-black/80 px-1.5 py-0.5 font-medium tabular-nums text-white/90 ${tvMode ? 'text-sm' : 'text-[11px]'}`}>
            <Clock3 className={tvMode ? 'h-4 w-4' : 'h-3 w-3'} aria-hidden="true" />
            {video.duration}
          </span>

          <span
            aria-hidden="true"
            className="absolute inset-0 flex items-center justify-center opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-within:opacity-100"
          >
            <span className={`flex items-center justify-center rounded-full bg-white/95 text-[#0b0c0e] shadow-2xl transition-transform duration-200 group-hover:scale-100 ${tvMode ? 'h-20 w-20 scale-90' : 'h-14 w-14 scale-90'}`}>
              <Play className={`ml-0.5 fill-current ${tvMode ? 'h-8 w-8' : 'h-6 w-6'}`} />
            </span>
          </span>
        </div>

        <div className={`mt-3 flex gap-3 ${tvMode ? 'gap-4' : ''}`}>
          <span
            aria-hidden="true"
            className={`mt-0.5 flex shrink-0 items-center justify-center rounded-full border ${meta.chip} ${tvMode ? 'h-12 w-12' : 'h-9 w-9'}`}
          >
            <Icon className={tvMode ? 'h-5 w-5' : 'h-4 w-4'} />
          </span>

          <div className="min-w-0 flex-1">
            <h3 className={`line-clamp-2 font-semibold leading-snug tracking-[-0.01em] text-white/90 transition-colors group-hover:text-white ${tvMode ? 'text-xl' : 'text-[15px]'}`}>
              {video.title}
            </h3>

            {video.coreQuestion && (
              <p className={`mt-1 line-clamp-2 leading-relaxed text-white/45 ${tvMode ? 'text-base' : 'text-[12.5px]'}`}>
                {video.coreQuestion}
              </p>
            )}

            <div className={`mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 ${tvMode ? 'text-sm' : 'text-[11px]'}`}>
              <span
                className={`inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 font-semibold uppercase tracking-[0.08em] ${meta.chip}`}
              >
                <span className={`h-1 w-1 rounded-full ${meta.dot}`} />
                {video.category}
              </span>
              <span className="truncate text-white/45">{video.channel}</span>
              <span aria-hidden="true" className="h-2.5 w-px bg-white/15" />
              <span className="text-white/30">{video.difficulty}</span>
            </div>
          </div>
        </div>
      </button>

      {/* Save lives outside the play button so the card never nests one button
          inside another. It sits over the thumbnail, like a playlist add. */}
      {onToggleSaved && (
        <button
          type="button"
          onClick={() => onToggleSaved(video)}
          aria-pressed={saved}
          aria-label={
            saved
              ? `Remove ${video.title} from my list`
              : `Save ${video.title} to my list`
          }
          title={saved ? 'Remove from my list' : 'Save to my list'}
          className={`absolute right-2.5 top-2.5 z-10 inline-flex items-center justify-center rounded-full border shadow-lg transition focus:outline-none focus-visible:ring-2 focus-visible:ring-white/70 ${
            tvMode ? 'h-11 w-11' : 'h-8 w-8'
          } ${
            saved
              ? 'border-amber-300/70 bg-amber-300 text-[#0b0c0e]'
              : 'border-white/25 bg-black/70 text-white/80 hover:bg-white hover:text-[#0b0c0e]'
          }`}
        >
          <Bookmark
            className={tvMode ? 'h-5 w-5' : 'h-4 w-4'}
            fill={saved ? 'currentColor' : 'none'}
            aria-hidden="true"
          />
        </button>
      )}

      {/* A watched video can be put back into the collection straight from its
          card, which is the only place the badge appears in the Watched view. */}
      {watched &&
        (onToggleWatched ? (
          <button
            type="button"
            onClick={() => onToggleWatched(video)}
            aria-label={`Mark ${video.title} as unwatched`}
            title="Mark as unwatched"
            className={`absolute left-2.5 top-2.5 z-10 inline-flex items-center gap-1 rounded-md bg-emerald-400/95 px-2 py-1 font-semibold uppercase tracking-[0.08em] text-[#04120a] shadow-lg transition hover:bg-emerald-300 focus:outline-none focus-visible:ring-2 focus-visible:ring-white/70 ${
              tvMode ? 'text-xs' : 'text-[10px]'
            }`}
          >
            <Check className={tvMode ? 'h-4 w-4' : 'h-3 w-3'} aria-hidden="true" />
            Watched
          </button>
        ) : (
          <span
            className={`absolute left-2.5 top-2.5 z-10 inline-flex items-center gap-1 rounded-md bg-emerald-400/95 px-2 py-1 font-semibold uppercase tracking-[0.08em] text-[#04120a] shadow-lg ${
              tvMode ? 'text-xs' : 'text-[10px]'
            }`}
          >
            <Check className={tvMode ? 'h-4 w-4' : 'h-3 w-3'} aria-hidden="true" />
            Watched
          </span>
        ))}
    </div>
  );
}

// Cards only change when their video, personal state, focus state or layout
// mode changes, so search typing and opening the player do not re-render the
// whole grid.
export default memo(VideoCard);
