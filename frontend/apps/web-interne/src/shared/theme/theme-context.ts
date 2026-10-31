import { createContext, useContext } from 'react'

export type ThemePreference = 'light' | 'dark' | 'system'
export type ResolvedTheme = 'light' | 'dark'

export interface ThemeContextValue {
  /** Choix de l'utilisateur (persiste). */
  readonly theme: ThemePreference
  /** Theme effectivement applique. */
  readonly resolvedTheme: ResolvedTheme
  setTheme: (theme: ThemePreference) => void
  /** Bascule clair/sombre (fixe un choix explicite). */
  toggleTheme: () => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)

/**
 * Theme courant et actions de changement.
 * @throws Error hors d'un ThemeProvider
 */
export function useTheme(): ThemeContextValue {
  const value = useContext(ThemeContext)
  if (!value) throw new Error('useTheme doit etre utilise dans un <ThemeProvider>.')
  return value
}
