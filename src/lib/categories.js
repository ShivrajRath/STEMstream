import {
  Atom,
  Cpu,
  Dna,
  LayoutGrid,
  Orbit,
  Sigma,
  Wrench,
} from 'lucide-react';

/**
 * Single source of truth for category labels, icons and accent colours.
 * Keep the category names aligned with `VALID_CATEGORIES` in
 * `scripts/update_content.py` and the entries in `src/data/data.json`.
 */
export const CATEGORY_META = {
  All: {
    icon: LayoutGrid,
    dot: 'bg-white',
    text: 'text-white',
    chip: 'border-white/15 bg-white/[0.07] text-white/75',
  },
  Physics: {
    icon: Atom,
    dot: 'bg-sky-400',
    text: 'text-sky-300',
    chip: 'border-sky-400/25 bg-sky-400/10 text-sky-300',
  },
  Space: {
    icon: Orbit,
    dot: 'bg-violet-400',
    text: 'text-violet-300',
    chip: 'border-violet-400/25 bg-violet-400/10 text-violet-300',
  },
  'Computer Science': {
    icon: Cpu,
    dot: 'bg-emerald-400',
    text: 'text-emerald-300',
    chip: 'border-emerald-400/25 bg-emerald-400/10 text-emerald-300',
  },
  Engineering: {
    icon: Wrench,
    dot: 'bg-orange-400',
    text: 'text-orange-300',
    chip: 'border-orange-400/25 bg-orange-400/10 text-orange-300',
  },
  Biology: {
    icon: Dna,
    dot: 'bg-pink-400',
    text: 'text-pink-300',
    chip: 'border-pink-400/25 bg-pink-400/10 text-pink-300',
  },
  Math: {
    icon: Sigma,
    dot: 'bg-yellow-400',
    text: 'text-yellow-300',
    chip: 'border-yellow-400/25 bg-yellow-400/10 text-yellow-300',
  },
};

export const CATEGORIES = Object.keys(CATEGORY_META);

const FALLBACK_META = {
  icon: LayoutGrid,
  dot: 'bg-white',
  text: 'text-white/70',
  chip: 'border-white/15 bg-white/[0.07] text-white/70',
};

export function getCategoryMeta(category) {
  return CATEGORY_META[category] ?? FALLBACK_META;
}
