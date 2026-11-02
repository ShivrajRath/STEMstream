export default function Logo({ size = 'h-9 w-9', showWordmark = true }) {
  return (
    <span className="flex items-center gap-2.5">
      <span className={`relative shrink-0 ${size}`} aria-hidden="true">
        <svg viewBox="0 0 32 32" className="h-full w-full drop-shadow-[0_4px_14px_rgba(255,0,60,0.35)]">
          <defs>
            <linearGradient id="stemstream-mark" x1="3" y1="2" x2="29" y2="30" gradientUnits="userSpaceOnUse">
              <stop stopColor="#ff2951" />
              <stop offset="0.55" stopColor="#e8002d" />
              <stop offset="1" stopColor="#ff6a00" />
            </linearGradient>
          </defs>
          <rect x="1" y="1" width="30" height="30" rx="9.5" fill="url(#stemstream-mark)" />
          <ellipse
            cx="16"
            cy="16"
            rx="11.5"
            ry="5.2"
            transform="rotate(-28 16 16)"
            fill="none"
            stroke="white"
            strokeOpacity="0.35"
            strokeWidth="1"
          />
          <path d="M13.4 11.5 21.3 16l-7.9 4.5z" fill="white" />
        </svg>
      </span>
      {showWordmark && (
        <span className="font-display text-[19px] font-bold leading-none tracking-[-0.02em] text-white">
          STEM<span className="text-[#ff4d5f]">Stream</span>
        </span>
      )}
    </span>
  );
}
