import { useState } from 'react';
import { Clock3, Play } from 'lucide-react';
import { getCategoryMeta } from '../lib/categories';

export default function VideoCard({ video, onSelect, priority = false, tvMode = false, isFocused = false }) {
  const meta = getCategoryMeta(video.category);
  const Icon = meta.icon;

  // maxresdefault is the preferred thumbnail, but YouTube does not generate it
  // for every upload. Fall back to hqdefault and finally to a styled placeholder.
  const [thumbnailStage, setThumbnailStage] = useState(0);
  const thumbnailUrl =
    thumbnailStage === 0
      ? `https://img.youtube.com/vi/${video.youtubeId}/maxresdefault.jpg`
      : `https://img.youtube.com/vi/${video.youtubeId}/hqdefault.jpg`;

  return (
    <button
      type="button"
      onClick={() => onSelect(video)}
      aria-label={`Play ${video.title} by ${video.channel}`}
      data-video-card
      className={`group block w-full rounded-2xl text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50 focus-visible:ring-offset-4 focus-visible:ring-offset-[#08090a] ${
        isFocused && tvMode ? 'ring-2 ring-emerald-400 ring-offset-2 ring-offset-[#08090a]' : ''
      }`}
    >
      <div className="relative aspect-video w-full overflow-hidden rounded-xl border border-white/[0.08] bg-[#141518] shadow-[0_1px_0_rgba(255,255,255,0.04)_inset] transition-colors duration-300 group-hover:border-white/20">
        {thumbnailStage < 2 ? (
          <img
            src={thumbnailUrl}
            alt={`Thumbnail for ${video.title}`}
            onError={() => setThumbnailStage((stage) => stage + 1)}
            loading={priority ? 'eager' : 'lazy'}
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

        <span className={`absolute bottom-2.5 right-2.5 inline-flex items-center gap-1 rounded-md bg-black/75 px-1.5 py-0.5 font-medium tabular-nums text-white/90 backdrop-blur-md ${tvMode ? 'text-sm' : 'text-[11px]'}`}>
          <Clock3 className={tvMode ? 'h-4 w-4' : 'h-3 w-3'} aria-hidden="true" />
          {video.duration}
        </span>

        <span
          aria-hidden="true"
          className="absolute inset-0 flex items-center justify-center opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-visible:opacity-100"
        >
          <span className={`flex items-center justify-center rounded-full bg-white/95 text-[#0b0c0e] shadow-2xl transition-transform duration-200 group-hover:scale-100 group-focus-visible:scale-100 ${tvMode ? 'h-20 w-20 scale-90' : 'h-14 w-14 scale-90'}`}>
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
  );
}
