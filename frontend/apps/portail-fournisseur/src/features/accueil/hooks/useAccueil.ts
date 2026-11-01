import { useMemo } from 'react'
import { useAppelsOffres, useContrats, useFactures, useMesOffres } from '@/api/queries'
import { contratsActifs } from '@/domain/contrats'
import { totauxFactures } from '@/domain/factures'
import { resultatOffre } from '@/domain/offres'
import { deriverAFaire, type AFaire } from '../lib/a-faire'

export interface Accueil {
  readonly aFaire: readonly AFaire[]
  readonly kpis: {
    offresEnCours: number
    contratsActifs: number
    montantFacture: number
    montantPaye: number
  }
  readonly isLoading: boolean
  /** Premiere erreur bloquante (hors 404 deja convertis en listes vides). */
  readonly error: unknown
  refetch: () => void
}

/** Donnees de l'accueil : liste « À faire » et 4 indicateurs, derives des listes du portail. */
export function useAccueil(): Accueil {
  const appelsOffres = useAppelsOffres()
  const offres = useMesOffres()
  const contrats = useContrats()
  const factures = useFactures()
  const queries = [appelsOffres, offres, contrats, factures]

  const data = useMemo(() => {
    const sources = {
      appelsOffres: appelsOffres.data?.content ?? [],
      contrats: contrats.data?.content ?? [],
      factures: factures.data?.content ?? [],
    }
    const totaux = totauxFactures(sources.factures)
    return {
      aFaire: deriverAFaire(sources),
      kpis: {
        offresEnCours: (offres.data?.content ?? []).filter((o) => resultatOffre(o) === 'EN_COURS').length,
        contratsActifs: contratsActifs(sources.contrats),
        montantFacture: totaux.facture,
        montantPaye: totaux.paye,
      },
    }
  }, [appelsOffres.data, offres.data, contrats.data, factures.data])

  return {
    ...data,
    isLoading: queries.some((q) => q.isPending),
    error: queries.find((q) => q.error)?.error ?? null,
    refetch: () => {
      queries.forEach((q) => void q.refetch())
    },
  }
}
