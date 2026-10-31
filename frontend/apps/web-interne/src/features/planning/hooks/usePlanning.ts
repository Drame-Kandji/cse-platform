import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { CATEGORIE_MATERIEL, type CategorieMateriel } from '@/shared/domain/labels'
import { toDate, toIsoDate } from '@/shared/lib/format'
import { affectationKeys, suiviApi } from '@/shared/suivi'
import { decaler, periodeDe, ZOOMS, type Periode, type Zoom } from '../lib/layout'
import { grouperPlanning } from '../lib/rows'

export interface PlanningState {
  readonly zoom: Zoom
  readonly ancre: Date
  readonly periode: Periode
  readonly chantierId: string | null
  readonly categorie: CategorieMateriel | null
}

/**
 * Etat du planning synchronise avec l'URL (?vue=mois&date=2026-10-30&chantier=…&categorie=…) :
 * lien partageable, bouton Retour coherent.
 */
export function usePlanningState() {
  const [params, setParams] = useSearchParams()
  const vue = params.get('vue')
  const zoom: Zoom = ZOOMS.includes(vue as Zoom) ? (vue as Zoom) : 'mois'
  const dateParam = params.get('date')
  const chantierId = params.get('chantier')
  const categorieParam = params.get('categorie')
  const categorie = CATEGORIE_MATERIEL.find((value) => value === categorieParam) ?? null

  const state = useMemo<PlanningState>(() => {
    const ancre = toDate(dateParam) ?? new Date()
    return { zoom, ancre, periode: periodeDe(zoom, ancre), chantierId, categorie }
  }, [zoom, dateParam, chantierId, categorie])

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
    state,
    setZoom: (value: Zoom) => {
      update({ vue: value === 'mois' ? null : value })
    },
    naviguer: (sens: number) => {
      update({ date: toIsoDate(decaler(state.zoom, state.ancre, sens)) ?? null })
    },
    aujourdhui: () => {
      update({ date: null })
    },
    setChantier: (value: string | null) => {
      update({ chantier: value })
    },
    setCategorie: (value: CategorieMateriel | null) => {
      update({ categorie: value })
    },
  }
}

/** Affectations de la periode (GET /api/affectations/planning), groupees par chantier et materiel. */
export function usePlanning(state: PlanningState) {
  const params = {
    du: toIsoDate(state.periode.du) ?? '',
    au: toIsoDate(state.periode.au) ?? '',
    chantierId: state.chantierId ?? undefined,
    categorie: state.categorie ?? undefined,
  }
  const query = useQuery({
    queryKey: affectationKeys.custom('planning', params),
    queryFn: ({ signal }) => suiviApi.planning(params, signal),
    placeholderData: keepPreviousData,
  })
  const groupes = useMemo(() => grouperPlanning(query.data ?? []), [query.data])
  return { ...query, groupes }
}
