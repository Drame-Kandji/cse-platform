import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { besoinsApi } from '../api/besoins.api'
import { besoinKeys } from '../api/besoins.keys'
import type { Besoin, BesoinModification, BesoinPayload, ValidationBesoin } from '../api/types'

/** Met a jour le detail en cache et invalide listes, statistiques, historique et consolidation. */
function synchroniser(queryClient: QueryClient, besoin: Besoin) {
  queryClient.setQueryData(besoinKeys.detail(besoin.id), besoin)
  void queryClient.invalidateQueries({ queryKey: besoinKeys.lists() })
  void queryClient.invalidateQueries({ queryKey: besoinKeys.custom('statistiques') })
  void queryClient.invalidateQueries({ queryKey: besoinKeys.custom('historique', besoin.id) })
  void queryClient.invalidateQueries({ queryKey: besoinKeys.custom('consolidation') })
}

/** Creation d'un besoin (erreurs gerees par le formulaire : champs serveur). */
export function useCreerBesoin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: BesoinPayload) => besoinsApi.creer(body),
    meta: { successMessage: 'Besoin enregistré', silentError: true },
    onSuccess: (besoin) => {
      synchroniser(queryClient, besoin)
    },
  })
}

/** Modification d'un besoin EN_ATTENTE par son demandeur (R05). */
export function useModifierBesoin(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: BesoinModification) => besoinsApi.modifier(id, body),
    meta: { successMessage: 'Besoin modifié', silentError: true },
    onSuccess: (besoin) => {
      synchroniser(queryClient, besoin)
    },
  })
}

/** Validation par le Responsable Matériel (R01) avec choix optionnel des materiels. */
export function useValiderBesoin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, ...body }: ValidationBesoin & { id: string }) => besoinsApi.valider(id, body),
    meta: { successMessage: 'Besoin validé' },
    onSuccess: (besoin) => {
      synchroniser(queryClient, besoin)
      void queryClient.invalidateQueries({ queryKey: ['materiels'] })
    },
  })
}

/** Rejet motive par le Responsable Matériel. */
export function useRejeterBesoin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, motif }: { id: string; motif: string }) => besoinsApi.rejeter(id, motif),
    meta: { successMessage: 'Besoin rejeté' },
    onSuccess: (besoin) => {
      synchroniser(queryClient, besoin)
    },
  })
}

/** Relance de l'orientation d'un besoin VALIDE resté en attente (ms-suivi indisponible). */
export function useOrienterBesoin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => besoinsApi.orienter(id),
    meta: { successMessage: 'Orientation relancée' },
    onSuccess: (besoin) => {
      synchroniser(queryClient, besoin)
      void queryClient.invalidateQueries({ queryKey: ['materiels'] })
    },
  })
}
