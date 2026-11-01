import { QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { createQueryClient } from '@/shared/api/query-client'
import { AuthProvider } from '@/shared/auth/AuthProvider'
import { createKeycloakDriver } from '@/shared/auth/drivers/keycloak'
import { env } from '@/shared/config/env'
import { ThemeProvider } from '@/shared/theme/ThemeProvider'
import { Toaster } from '@/shared/ui/toaster'
import { TooltipProvider } from '@/shared/ui/tooltip'
import { messageErreur } from '@/domain/errors'

/** Le portail est toujours authentifie par Keycloak (client `cse-portail-fournisseur`). */
const keycloakDriver = () => createKeycloakDriver(env.keycloak)

/** Fournisseurs globaux : theme, cache serveur (messages d'erreur du portail), session, toasts. */
export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => createQueryClient({ errorMessage: messageErreur }))
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <TooltipProvider delayDuration={300} skipDelayDuration={150}>
          <AuthProvider driverFactory={keycloakDriver}>{children}</AuthProvider>
          <Toaster />
        </TooltipProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}
