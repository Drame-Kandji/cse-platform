import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  CATEGORIE_MATERIEL,
  ETAT_MATERIEL,
  type CategorieMateriel,
  type EtatMateriel,
} from '@/shared/domain/labels'
import type { MaterielFiltres } from '@/shared/suivi'

/** Filtres du parc tels qu'edites dans l'interface (etat et categorie : une valeur ou "Tous"). */
export interface MaterielFiltresState {
  readonly etat: EtatMateriel | null
  readonly categorie: CategorieMateriel | null
  readonly chantierId: string | null
  readonly q: string
  readonly maintenanceDue: boolean
}

function pick<V extends string>(value: string | null, allowed: readonly V[]): V | null {
  return allowed.find((item) => item === value) ?? null
}

/** Convertit l'etat d'interface en parametres d'API (valeurs vides omises). */
export function toApiFiltres(state: MaterielFiltresState): MaterielFiltres {
  return {
    etat: state.etat ?? undefined,
    categorie: state.categorie ?? undefined,
    chantierId: state.chantierId ?? undefined,
    q: state.q.trim() || undefined,
    maintenanceDue: state.maintenanceDue ? true : undefined,
  }
}

const URL_KEYS = {
  etat: 'etat',
  categorie: 'categorie',
  chantierId: 'chantier',
  q: 'q',
} as const

/**
 * Filtres du parc synchronises avec l'URL (?etat=…&categorie=…&chantier=…&due=1&q=…).
 * Toute modification revient a la premiere page.
 */
export function useMaterielFiltres() {
  const [params, setParams] = useSearchParams()
  const etat = pick(params.get('etat'), ETAT_MATERIEL)
  const categorie = pick(params.get('categorie'), CATEGORIE_MATERIEL)
  const chantierId = params.get('chantier')
  const q = params.get('q') ?? ''
  const maintenanceDue = params.get('due') === '1'

  const state = useMemo<MaterielFiltresState>(
    () => ({ etat, categorie, chantierId, q, maintenanceDue }),
    [etat, categorie, chantierId, q, maintenanceDue],
  )

  const update = useCallback(
    (changes: Partial<MaterielFiltresState>) => {
      setParams(
        (current) => {
          const next = new URLSearchParams(current)
          next.delete('page')
          for (const [field, key] of Object.entries(URL_KEYS)) {
            if (!(field in changes)) continue
            const value = changes[field as keyof typeof URL_KEYS]
            if (value) next.set(key, value)
            else next.delete(key)
          }
          if (changes.maintenanceDue !== undefined) {
            if (changes.maintenanceDue) next.set('due', '1')
            else next.delete('due')
          }
          return next
        },
        { replace: true },
      )
    },
    [setParams],
  )

  const active =
    state.etat !== null ||
    state.categorie !== null ||
    state.chantierId !== null ||
    state.q !== '' ||
    state.maintenanceDue
  const reset = () => {
    update({ etat: null, categorie: null, chantierId: null, q: '', maintenanceDue: false })
  }
  return { state, update, reset, active, api: useMemo(() => toApiFiltres(state), [state]) }
}
