/**
 * Acces tolerant au localStorage (navigation privee, quotas, SSR).
 * Toute preference locale (theme, sidebar) passe par ces helpers.
 */
export const storage = {
  get(key: string): string | null {
    try {
      return window.localStorage.getItem(key)
    } catch {
      return null
    }
  },
  set(key: string, value: string): void {
    try {
      window.localStorage.setItem(key, value)
    } catch {
      // Stockage indisponible : la preference reste en memoire pour la session.
    }
  },
  remove(key: string): void {
    try {
      window.localStorage.removeItem(key)
    } catch {
      // Stockage indisponible : rien a nettoyer.
    }
  },
}

/** Cles de stockage centralisees pour eviter les collisions. */
export const STORAGE_KEYS = {
  theme: 'cse.theme',
  sidebarCollapsed: 'cse.sidebar.collapsed',
  devSession: 'cse.dev.session',
} as const
