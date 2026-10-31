import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useMediaQuery } from '@/shared/lib/hooks'
import { storage, STORAGE_KEYS } from '@/shared/lib/storage'
import { ThemeContext, type ThemePreference } from './theme-context'

function readPreference(): ThemePreference {
  const stored = storage.get(STORAGE_KEYS.theme)
  return stored === 'light' || stored === 'dark' ? stored : 'system'
}

/**
 * Applique la classe `dark` sur <html> selon la preference (persistee) ou le systeme.
 * Un script inline dans index.html evite le flash au chargement.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemePreference>(readPreference)
  const systemDark = useMediaQuery('(prefers-color-scheme: dark)')
  const resolvedTheme = theme === 'system' ? (systemDark ? 'dark' : 'light') : theme

  useEffect(() => {
    const root = document.documentElement
    root.classList.toggle('dark', resolvedTheme === 'dark')
    root.style.colorScheme = resolvedTheme
  }, [resolvedTheme])

  const setTheme = useCallback((next: ThemePreference) => {
    setThemeState(next)
    storage.set(STORAGE_KEYS.theme, next)
  }, [])

  const value = useMemo(
    () => ({
      theme,
      resolvedTheme,
      setTheme,
      toggleTheme: () => {
        setTheme(resolvedTheme === 'dark' ? 'light' : 'dark')
      },
    }),
    [theme, resolvedTheme, setTheme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
