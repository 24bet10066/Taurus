/** @type {import('tailwindcss').Config} */
//
// ServiceOS — design tokens
// Warm Indian-business light theme. Saffron brand, deep-ink type.
// Old token names (void/base/surface/elevated/accent/text.*) are kept as aliases
// remapped to the new palette so any unmigrated component still renders ok.
//
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // ── Surfaces (warm light) ─────────────────────────────────────────
        canvas:    '#FFF8F0', // page background — warm cream
        paper:     '#FFFFFF', // cards
        'paper-2': '#FAF6F0', // sub-elevated / hover
        'paper-3': '#F5F0E8', // inputs / muted blocks

        // ── Type ──────────────────────────────────────────────────────────
        ink:     '#1A1209', // primary text — deep warm black
        'ink-2': '#5C5247', // secondary
        'ink-3': '#8B8377', // tertiary / muted
        'ink-4': '#B8AFA0', // placeholder / disabled

        // ── Lines ─────────────────────────────────────────────────────────
        line:     '#EDE6D9', // subtle dividers
        'line-2': '#DDD2BE', // borders, focused inputs

        // ── Brand ─────────────────────────────────────────────────────────
        brand:        '#DC4F00', // saffron / amber-orange
        'brand-2':    '#B53F00', // pressed / deeper
        'brand-3':    '#7A2B00', // text on tint
        'brand-tint': '#FEF3E8',
        'brand-tint-2':'#FCE0C2',

        // ── Status semantic ──────────────────────────────────────────────
        money:         '#047857', // green for revenue / completed
        'money-tint':  '#D1FAE5',
        urgent:        '#B91C1C', // red for problems
        'urgent-tint': '#FEE2E2',
        pending:       '#B45309', // amber waiting
        'pending-tint':'#FEF3C7',

        // ── Job pipeline (light-bg safe) ──────────────────────────────────
        job: {
          requested: '#B45309',
          assigned:  '#B53F00',
          transit:   '#6D28D9',
          customer:  '#0E7490',
          progress:  '#7C3AED',
          parts:     '#C2410C',
          completed: '#047857',
          cancelled: '#B91C1C',
        },

        // ── LEGACY aliases (so any old class still renders ok) ───────────
        void:    '#FFF8F0',
        base:    '#FFFFFF',
        surface: '#FFFFFF',
        elevated:'#FFFFFF',
        overlay: '#FAF6F0',
        accent: {
          DEFAULT: '#DC4F00',
          dim:  'rgba(220,79,0,0.10)',
          glow: 'rgba(220,79,0,0.18)',
        },
        text: {
          1: '#1A1209',
          2: '#5C5247',
          3: '#8B8377',
        },
      },

      fontFamily: {
        display: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'],
        sans:    ['"Plus Jakarta Sans"', 'Inter', 'system-ui', 'sans-serif'],
        body:    ['Inter', 'system-ui', 'sans-serif'],
        hindi:   ['"Noto Sans Devanagari"', '"Plus Jakarta Sans"', 'sans-serif'],
        mono:    ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
      },

      fontSize: {
        mega: ['2.75rem', { lineHeight: '1.05', letterSpacing: '-0.02em',  fontWeight: '700' }],
        big:  ['1.75rem', { lineHeight: '1.1',  letterSpacing: '-0.015em', fontWeight: '700' }],
      },

      animation: {
        'pulse-dot': 'pulseDot 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'slide-up':  'slideUp 0.32s cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-in':  'slideIn 0.28s cubic-bezier(0.16, 1, 0.3, 1)',
        'fade-up':   'fadeUp 0.35s cubic-bezier(0.16, 1, 0.3, 1)',
        'shimmer':   'shimmer 1.5s linear infinite',
        'pop':       'pop 0.22s cubic-bezier(0.34, 1.56, 0.64, 1)',
        'tap':       'tap 0.18s ease-out',
      },
      keyframes: {
        pulseDot: {
          '0%, 100%': { opacity: 1, transform: 'scale(1)' },
          '50%':      { opacity: 0.4, transform: 'scale(0.7)' },
        },
        slideIn: {
          '0%':   { transform: 'translateX(100%)', opacity: 0 },
          '100%': { transform: 'translateX(0)',    opacity: 1 },
        },
        slideUp: {
          '0%':   { transform: 'translateY(100%)' },
          '100%': { transform: 'translateY(0)' },
        },
        fadeUp: {
          '0%':   { transform: 'translateY(8px)', opacity: 0 },
          '100%': { transform: 'translateY(0)',   opacity: 1 },
        },
        shimmer: {
          '0%':   { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition:  '200% 0' },
        },
        pop: {
          '0%':   { transform: 'scale(0.92)', opacity: 0 },
          '100%': { transform: 'scale(1)',    opacity: 1 },
        },
        tap: {
          '0%':   { transform: 'scale(1)' },
          '50%':  { transform: 'scale(0.96)' },
          '100%': { transform: 'scale(1)' },
        },
      },

      boxShadow: {
        'card':       '0 1px 2px rgba(26,18,9,0.04), 0 1px 1px rgba(26,18,9,0.03)',
        'card-2':     '0 2px 8px rgba(26,18,9,0.06), 0 1px 2px rgba(26,18,9,0.04)',
        'panel':      '0 12px 32px rgba(26,18,9,0.08), 0 2px 6px rgba(26,18,9,0.04)',
        'sheet':      '0 -8px 32px rgba(26,18,9,0.10)',
        'brand':      '0 1px 2px rgba(220,79,0,0.20), 0 4px 12px rgba(220,79,0,0.18)',
        'focus':      '0 0 0 4px rgba(220,79,0,0.18)',
        'focus-money':'0 0 0 4px rgba(4,120,87,0.18)',
      },

      borderRadius: {
        xl2:  '1rem',
        '2xl2':'1.25rem',
      },
    },
  },
  plugins: [],
};
