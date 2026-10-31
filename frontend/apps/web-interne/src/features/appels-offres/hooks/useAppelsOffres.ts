import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api'
import type { CategorieMateriel } from '@/shared/domain/labels'
import { appelsOffresApi } from '../api/appels-offres.api'
import { appelOffreKeys, besoinAAcheterKeys } from '../api/appels-offres.keys'
import type { AppelOffre, AppelOffreFiltres } from '../api/types'

/** Liste paginee des appels d'offres (filtres statut, type, recherche). */
export function useAppelsOffres(params: AppelOffreFiltres & PageParams) {
  return useQuery({
    queryKey: appelOffreKeys.list(params),
    queryFn: ({ signal }) => appelsOffresApi.lister(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Detail d'un appel d'offres. */
export function useAppelOffre(id: string) {
  return useQuery({
    queryKey: appelOffreKeys.detail(id),
    queryFn: ({ signal }) => appelsOffresApi.detail(id, signal),
  })
}

/** Besoins valides orientes vers l'achat (RA, RM). */
export function useBesoinsAAcheter(params: PageParams, enabled = true) {
  return useQuery({
    queryKey: besoinAAcheterKeys.list(params),
    queryFn: ({ signal }) => appelsOffresApi.besoinsAAcheter(params, signal),
    placeholderData: keepPreviousData,
    enabled,
  })
}

/** Fournisseurs ACTIFS invitables pour une specialite (R14). */
export function useFournisseursInvitables(specialite: CategorieMateriel | undefined) {
  return useQuery({
    queryKey: appelOffreKeys.custom('invitables', specialite),
    queryFn: ({ signal }) => appelsOffresApi.fournisseursInvitables(specialite ?? 'MATERIEL_ROULANT', signal),
    enabled: specialite !== undefined,
  })
}

/** Offres d'un appel d'offres (anonymisees tant qu'il est PUBLIE, R10). */
export function useOffres(id: string, enabled = true) {
  return useQuery({
    queryKey: appelOffreKeys.custom('offres', id),
    queryFn: ({ signal }) => appelsOffresApi.offres(id, signal),
    enabled,
  })
}

/** Contrat genere pour un AO attribue (null si introuvable). */
export function useContratLie(ao: Pick<AppelOffre, 'id' | 'objet'>, enabled: boolean) {
  return useQuery({
    queryKey: appelOffreKeys.custom('contrat', ao.id),
    queryFn: ({ signal }) => appelsOffresApi.contratLie(ao, signal),
    enabled,
  })
}

/** Comparatif multicritere des offres. */
export function useComparatif(id: string, enabled = true) {
  return useQuery({
    queryKey: appelOffreKeys.custom('comparatif', id),
    queryFn: ({ signal }) => appelsOffresApi.comparatif(id, signal),
    enabled,
  })
}
