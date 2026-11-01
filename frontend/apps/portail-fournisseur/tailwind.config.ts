import type { Config } from 'tailwindcss'
import base from '../web-interne/tailwind.config'

/** Meme theme que la plateforme interne ; on scanne aussi le design system partage. */
export default {
  ...base,
  content: ['./index.html', './src/**/*.{ts,tsx}', '../web-interne/src/shared/**/*.{ts,tsx}'],
} satisfies Config
