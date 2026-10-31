import { useEffect, useState, useSyncExternalStore } from 'react'

/**
 * Indique si une media query CSS correspond (mise a jour en direct).
 * @example const isDesktop = useMediaQuery('(min-width: 1024px)')
 */
export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    (onChange) => {
      const mql = window.matchMedia(query)
      mql.addEventListener('change', onChange)
      return () => {
        mql.removeEventListener('change', onChange)
      }
    },
    () => window.matchMedia(query).matches,
    () => false,
  )
}

/** Points de rupture alignes sur Tailwind (md = 768px, lg = 1024px, xl = 1280px, 2xl = 1536px). */
export const BREAKPOINTS = {
  md: '(min-width: 768px)',
  lg: '(min-width: 1024px)',
  xl: '(min-width: 1280px)',
  '2xl': '(min-width: 1536px)',
} as const

/**
 * Retarde la propagation d'une valeur (recherche texte, filtres).
 * @param delay delai en millisecondes (300 par defaut)
 */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebounced(value)
    }, delay)
    return () => {
      window.clearTimeout(timer)
    }
  }, [value, delay])
  return debounced
}

/** Detecte macOS/iOS pour afficher le bon raccourci (Cmd ou Ctrl). */
export function isApplePlatform(): boolean {
  return typeof navigator !== 'undefined' && /Mac|iPhone|iPad|iPod/i.test(navigator.userAgent)
}
