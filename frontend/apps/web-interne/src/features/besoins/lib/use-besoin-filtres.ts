import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  CATEGORIE_MATERIEL,
  PRIORITE,
  STATUT_BESOIN,
  type CategorieMateriel,
  type Priorite,
  type StatutBesoin,
} from '@/shared/domain/labels'
import type { BesoinFiltres } from '../api/types'

type FiltreKey = 'statut' | 'priorite' | 'categorie' | 'chantierId' | 'q' | 'du' | 'au'

function parseEnum<V extends string>(raw: string | null, allowed: readonly V[]): V | undefined {
  return raw && (allowed as readonly string[]).includes(raw) ? (raw as V) : undefined
}

const empty = (value: string | null) => value ?? undefined

/**
 * Filtres de la liste des besoins, synchronises avec l'URL (?statut=EN_ATTENTE&q=pelle).
 * Toute modification remet la pagination a la premiere page.
 */
export function useBesoinFiltres() {
  const [params, setParams] = useSearchParams()
  const statut = params.get('statut')
  const priorite = params.get('priorite')
  const categorie = params.get('categorie')
  const chantierId = params.get('chantierId')
  const q = params.get('q')
  const du = params.get('du')
  const au = params.get('au')

  const filtres = useMemo<BesoinFiltres>(
    () => ({
      statut: parseEnum<StatutBesoin>(statut, STATUT_BESOIN),
      priorite: parseEnum<Priorite>(priorite, PRIORITE),
      categorie: parseEnum<CategorieMateriel>(categorie, CATEGORIE_MATERIEL),
      chantierId: empty(chantierId),
      q: empty(q),
      du: empty(du),
      au: empty(au),
    }),
    [statut, priorite, categorie, chantierId, q, du, au],
  )

  const update = useCallback(
    (changes: Partial<Record<FiltreKey, string | undefined>>) => {
      setParams(
        (current) => {
          const next = new URLSearchParams(current)
          for (const [key, value] of Object.entries(changes)) {
            if (value) next.set(key, value)
            else next.delete(key)
          }
          next.delete('page')
          return next
        },
        { replace: true },
      )
    },
    [setParams],
  )

  const actifs = [
    filtres.statut,
    filtres.priorite,
    filtres.categorie,
    filtres.chantierId,
    filtres.q,
    filtres.du,
  ].filter(Boolean).length

  const reset = useCallback(() => {
    update({
      statut: undefined,
      priorite: undefined,
      categorie: undefined,
      chantierId: undefined,
      q: undefined,
      du: undefined,
      au: undefined,
    })
  }, [update])

  return { filtres, update, reset, actifs }
}
