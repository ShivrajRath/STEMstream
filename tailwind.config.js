/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: [
          'Inter',
          'system-ui',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Roboto',
          'sans-serif',
        ],
        display: [
          'Space Grotesk',
          'Inter',
          'system-ui',
          'sans-serif',
        ],
      },
      colors: {
        yt: {
          base: "#08090a",
          surface: "#141518",
          hover: "#1c1e22",
          border: "#2a2c31",
          accent: "#ff0033",
          text: "#f1f1f1",
          muted: "#a1a1aa",
        },
        stem: {
          physics: "#38bdf8",
          space: "#a78bfa",
          cs: "#34d399",
          engineering: "#fb923c",
          biology: "#f472b6",
          math: "#facc15",
        },
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        scaleUp: {
          '0%': { opacity: '0', transform: 'scale(0.97) translateY(10px)' },
          '100%': { opacity: '1', transform: 'scale(1) translateY(0)' },
        },
      },
      animation: {
        'fade-in': 'fadeIn 180ms ease-out both',
        'scale-up': 'scaleUp 260ms cubic-bezier(0.16, 1, 0.3, 1) both',
      },
    },
  },
  plugins: [],
}
