import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { getErrorMessage, isApiError } from './errors'

declare module '@tanstack/react-query' {
  interface Register {
    mutationMeta: {
      /** Message de succes affiche en toast apres la mutation. */
      successMessage?: string
      /** Desactive le toast d'erreur global (erreur geree localement, ex. formulaire). */
      silentError?: boolean
    }
    queryMeta: {
      /** Affiche un toast si la requete echoue alors que des donnees sont deja en cache. */
      toastOnBackgroundError?: boolean
    }
  }
}

/** Ne jamais reessayer une erreur 4xx ; au plus 2 nouvelles tentatives sinon. */
export function shouldRetry(failureCount: number, error: unknown): boolean {
  if (isApiError(error) && error.isClientError) return false
  return failureCount < 2
}

/** Cree le QueryClient de l'application avec les regles globales CSE. */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({
      onError: (error, query) => {
        if (query.meta?.toastOnBackgroundError && query.state.data !== undefined) {
          toast.error(getErrorMessage(error))
        }
      },
    }),
    mutationCache: new MutationCache({
      onSuccess: (_data, _variables, _context, mutation) => {
        const message = mutation.meta?.successMessage
        if (message) toast.success(message)
      },
      onError: (error, _variables, _context, mutation) => {
        if (mutation.meta?.silentError) return
        toast.error(getErrorMessage(error))
      },
    }),
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        gcTime: 5 * 60_000,
        retry: shouldRetry,
        refetchOnWindowFocus: false,
      },
      mutations: { retry: false },
    },
  })
}
