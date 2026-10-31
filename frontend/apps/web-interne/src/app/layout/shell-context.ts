import { createContext, useContext } from 'react'

export interface ShellContextValue {
  readonly sidebarCollapsed: boolean
  toggleSidebar: () => void
  readonly mobileNavOpen: boolean
  setMobileNavOpen: (open: boolean) => void
  readonly paletteOpen: boolean
  setPaletteOpen: (open: boolean) => void
  readonly assistantOpen: boolean
  setAssistantOpen: (open: boolean) => void
}

export const ShellContext = createContext<ShellContextValue | null>(null)

/**
 * Etat de la coquille (sidebar, palette, assistant) : ouvrir la palette ou l'assistant depuis une page.
 * @throws Error hors de l'AppShell
 */
export function useShell(): ShellContextValue {
  const value = useContext(ShellContext)
  if (!value) throw new Error('useShell doit etre utilise dans <AppShell>.')
  return value
}
