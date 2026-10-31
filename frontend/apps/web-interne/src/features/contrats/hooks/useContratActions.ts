import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { getErrorMessage, isApiError } from '@/shared/api'
import { contratsApi } from '../api/contrats.api'
import { contratKeys } from '../api/contrats.keys'
import type { ClausesRequete, ConfirmationLivraison, Contrat } from '../api/types'

/** Met a jour le cache du detail avec la reponse et invalide les listes. */
function useOnContrat() {
  const queryClient = useQueryClient()
  return (contrat: Contrat) => {
    queryClient.setQueryData(contratKeys.detail(contrat.id), contrat)
    void queryClient.invalidateQueries({ queryKey: contratKeys.lists() })
  }
}

/** Enregistrement des clauses et jalons d'un contrat BROUILLON. */
export function useEnregistrerClauses(id: string) {
  const onSuccess = useOnContrat()
  return useMutation({
    mutationFn: (body: ClausesRequete) => contratsApi.enregistrerClauses(id, body),
    meta: { successMessage: 'Clauses enregistrées' },
    onSuccess,
  })
}

/** Signature du contrat (cree l'engagement EN_ATTENTE cote DAF). */
export function useSignerContrat(id: string) {
  const onSuccess = useOnContrat()
  return useMutation({
    mutationFn: () => contratsApi.signer(id),
    meta: { successMessage: 'Contrat signé : engagement transmis au DAF' },
    onSuccess,
  })
}

/** Message affiche quand le serveur refuse le demarrage faute d'engagement valide (422, R06). */
export const MESSAGE_ENGAGEMENT_NON_VALIDE =
  "L'engagement budgétaire n'est pas encore validé par le DAF (R06) : la livraison ne peut pas démarrer."

/**
 * Demarrage de la livraison (exige un engagement valide, R06).
 * Sur 422 `ENGAGEMENT_NON_VALIDE`, affiche un message explicite et recharge le contrat.
 */
export function useDemarrerLivraison(id: string) {
  const onSuccess = useOnContrat()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => contratsApi.demarrerLivraison(id),
    meta: { successMessage: 'Livraison démarrée', silentError: true },
    onSuccess,
    onError: (error) => {
      if (isApiError(error) && error.code === 'ENGAGEMENT_NON_VALIDE') {
        toast.error(MESSAGE_ENGAGEMENT_NON_VALIDE)
        void queryClient.invalidateQueries({ queryKey: contratKeys.detail(id) })
        return
      }
      toast.error(getErrorMessage(error))
    },
  })
}

/** Marque un jalon comme atteint a la date donnee (yyyy-MM-dd). */
export function useMarquerJalon(id: string) {
  const onSuccess = useOnContrat()
  return useMutation({
    mutationFn: ({ jalonId, dateReelle }: { jalonId: string; dateReelle: string }) =>
      contratsApi.marquerJalon(id, jalonId, dateReelle),
    meta: { successMessage: 'Jalon marqué comme atteint' },
    onSuccess,
  })
}

/** Confirmation de la livraison (date reelle et bon de livraison). */
export function useConfirmerLivraison(id: string) {
  const onSuccess = useOnContrat()
  return useMutation({
    mutationFn: (body: ConfirmationLivraison) => contratsApi.confirmerLivraison(id, body),
    meta: { successMessage: 'Livraison confirmée' },
    onSuccess,
  })
}

/** Message affiche quand des factures du contrat ne sont pas encore payees (409). */
export const MESSAGE_FACTURES_EN_COURS = 'Des factures ne sont pas encore payées : clôture impossible.'

/** Cloture du contrat livre ; 409 `FACTURES_EN_COURS` donne un message explicite. */
export function useCloturerContrat(id: string) {
  const onSuccess = useOnContrat()
  return useMutation({
    mutationFn: () => contratsApi.cloturer(id),
    meta: { successMessage: 'Contrat clôturé', silentError: true },
    onSuccess,
    onError: (error) => {
      const factures = isApiError(error) && error.code === 'FACTURES_EN_COURS'
      toast.error(factures ? MESSAGE_FACTURES_EN_COURS : getErrorMessage(error))
    },
  })
}
