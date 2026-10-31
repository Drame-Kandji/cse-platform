import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useCurrentUser } from '@/shared/auth/context'
import { periodFromParams, periodToParams, type PeriodPreset } from '../lib/period'
import { profilsAutorises, resolveProfil } from '../lib/profils'
import type { Profil } from '../types'

/**
 * Filtres du reporting synchronises avec l'URL (`vue`, `periode`, `du`, `au`, `chantier`) :
 * liens partageables et bouton Retour. La vue est bornee aux profils autorises du role.
 */
export function useReportingFilters() {
  const user = useCurrentUser()
  const [params, setParams] = useSearchParams()
  const allowed = useMemo(() => profilsAutorises(user.roles), [user.roles])
  const profil = resolveProfil(params.get('vue'), allowed)
  const periodKey = params.toString()
  // eslint-disable-next-line react-hooks/exhaustive-deps -- recalcule seulement si l'URL change
  const period = useMemo(() => periodFromParams(params), [periodKey])
  const chantierId = params.get('chantier') ?? undefined

  const update = useCallback(
    (changes: Record<string, string | null>) => {
      setParams(
        (current) => {
          const next = new URLSearchParams(current)
          for (const [key, value] of Object.entries(changes)) {
            if (value === null) next.delete(key)
            else next.set(key, value)
          }
          return next
        },
        { replace: true },
      )
    },
    [setParams],
  )

  return {
    allowed,
    profil,
    period,
    chantierId,
    setProfil: (next: Profil) => {
      update({ vue: next === allowed[0] ? null : next })
    },
    setPeriod: (next: { preset: PeriodPreset; du?: string; au?: string }) => {
      update(periodToParams(next))
    },
    setChantier: (next: string | null) => {
      update({ chantier: next })
    },
    update,
  }
}
