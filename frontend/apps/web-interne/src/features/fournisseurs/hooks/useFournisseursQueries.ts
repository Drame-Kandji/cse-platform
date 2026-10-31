import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api'
import type { CategorieMateriel } from '@/shared/domain/labels'
import { fournisseursApi } from '../api/fournisseurs.api'
import { fournisseurKeys, litigeKeys } from '../api/fournisseurs.keys'
import type { FournisseurFiltres } from '../api/types'

/** Liste paginee des fournisseurs (filtres statut, specialite, recherche). */
export function useFournisseurs(params: FournisseurFiltres & PageParams) {
  return useQuery({
    queryKey: fournisseurKeys.list(params),
    queryFn: ({ signal }) => fournisseursApi.lister(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Detail d'un fournisseur (documents, coordonnees bancaires si autorise). */
export function useFournisseur(id: string) {
  return useQuery({
    queryKey: fournisseurKeys.detail(id),
    queryFn: ({ signal }) => fournisseursApi.detail(id, signal),
  })
}

/** Indicateurs globaux des fournisseurs. */
export function useStatistiquesFournisseurs() {
  return useQuery({
    queryKey: fournisseurKeys.custom('statistiques'),
    queryFn: ({ signal }) => fournisseursApi.statistiques(signal),
  })
}

/** Classement des fournisseurs actifs, filtrable par categorie. */
export function useClassement(categorie: CategorieMateriel | undefined) {
  return useQuery({
    queryKey: fournisseurKeys.custom('classement', categorie ?? null),
    queryFn: ({ signal }) => fournisseursApi.classement(categorie, signal),
    placeholderData: keepPreviousData,
  })
}

/** Comparaison des prix catalogue entre fournisseurs. */
export function useComparaisonPrix(params: { categorie?: CategorieMateriel; q?: string }) {
  return useQuery({
    queryKey: fournisseurKeys.custom('comparaison-prix', params),
    queryFn: ({ signal }) => fournisseursApi.comparaisonPrix(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Evaluations d'un fournisseur (historique du score). */
export function useEvaluations(id: string) {
  return useQuery({
    queryKey: fournisseurKeys.custom('evaluations', id),
    queryFn: ({ signal }) => fournisseursApi.evaluations(id, signal),
  })
}

/** Catalogue d'un fournisseur. */
export function useCatalogue(id: string) {
  return useQuery({
    queryKey: fournisseurKeys.custom('catalogue', id),
    queryFn: ({ signal }) => fournisseursApi.catalogue(id, signal),
  })
}

/** Historique des prix d'une reference du catalogue (desactive sans reference). */
export function useHistoriquePrix(id: string, reference: string | undefined) {
  return useQuery({
    queryKey: fournisseurKeys.custom('prix', id, reference ?? null),
    queryFn: ({ signal }) => fournisseursApi.historiquePrix(id, reference, signal),
    enabled: reference !== undefined,
  })
}

/** Litiges d'un fournisseur (50 plus recents). */
export function useLitigesFournisseur(fournisseurId: string, enabled = true) {
  const params = { fournisseurId, page: 0, size: 50 }
  return useQuery({
    queryKey: litigeKeys.list(params),
    queryFn: ({ signal }) => fournisseursApi.litiges(params, signal),
    enabled,
  })
}
