import { RotateCcw, Telescope } from 'lucide-react';
import VideoCard from './VideoCard';

export default function VideoGrid({ videos, onSelectVideo, onReset }) {
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
    <div className="grid grid-cols-1 gap-x-5 gap-y-8 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {videos.map((video, index) => (
        <VideoCard
          key={video.id}
          video={video}
          onSelect={onSelectVideo}
          priority={index < 4}
        />
      ))}
    </div>
  );
}
