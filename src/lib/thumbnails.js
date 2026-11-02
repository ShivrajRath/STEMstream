const THUMBNAIL_BASE = 'https://i.ytimg.com/vi';

/**
 * YouTube publishes a fixed set of thumbnail renditions per video:
 * `mqdefault` (320x180, true 16:9), `hqdefault` (480x360, letterboxed) and
 * `maxresdefault` (1280x720, missing on some uploads).
 *
 * Cards advertise the smallest and the HD rendition through `srcset` so a
 * standard-density screen downloads a few kilobytes while a retina screen can
 * still ask for HD. The widths below describe the rendered card width for the
 * 4/3/2/1 column grids.
 */
export const THUMBNAIL_SIZES =
  '(min-width: 1280px) 25vw, (min-width: 1024px) 32vw, (min-width: 640px) 47vw, 94vw';

export function thumbnailUrl(youtubeId, rendition = 'hqdefault') {
  return `${THUMBNAIL_BASE}/${youtubeId}/${rendition}.jpg`;
}

export function thumbnailSrcSet(youtubeId) {
  return [
    `${thumbnailUrl(youtubeId, 'mqdefault')} 320w`,
    `${thumbnailUrl(youtubeId, 'hqdefault')} 480w`,
    `${thumbnailUrl(youtubeId, 'maxresdefault')} 1280w`,
  ].join(', ');
}
