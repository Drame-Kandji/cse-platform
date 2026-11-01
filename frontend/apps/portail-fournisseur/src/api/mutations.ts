import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'
import type {
  Contrat,
  FactureDepot,
  LigneCatalogueSaisie,
  OffreDepot,
  ProfilModification,
} from '@/domain/types'
import { appelOffreKeys, contratKeys, entrepriseKeys, factureKeys, offreKeys } from './keys'
import { portailApi } from './portail.api'

/** Remplace le contrat en cache par la reponse du serveur et rafraichit les listes. */
function majContrat(queryClient: QueryClient, contrat: Contrat) {
  queryClient.setQueryData(contratKeys.detail(contrat.id), contrat)
  return queryClient.invalidateQueries({ queryKey: contratKeys.lists() })
}

/** Depot d'une offre (erreurs gerees par le dialogue : DATE_LIMITE_DEPASSEE, 409...). */
export function useDeposerOffre(appelOffreId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: OffreDepot) => portailApi.deposerOffre(appelOffreId, body),
    meta: { successMessage: 'Offre déposée', silentError: true },
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: offreKeys.all }),
        queryClient.invalidateQueries({ queryKey: appelOffreKeys.all }),
      ]),
  })
}

/** Signature du contrat par le fournisseur (EN_ATTENTE_SIGNATURE -> SIGNE). */
export function useSignerContrat(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => portailApi.signer(id),
    meta: { successMessage: 'Contrat signé' },
    onSuccess: (contrat) => majContrat(queryClient, contrat),
  })
}

/** Refus de signature avec motif (retour en BROUILLON cote CSE). */
export function useRefuserContrat(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (motif: string) => portailApi.refuser(id, motif),
    meta: { successMessage: 'Refus envoyé au service achats' },
    onSuccess: (contrat) => majContrat(queryClient, contrat),
  })
}

/** Declaration d'expedition (ENGAGEMENT_VALIDE -> EN_LIVRAISON). */
export function useExpedierContrat(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: { dateExpedition: string; commentaire?: string }) => portailApi.expedier(id, body),
    meta: { successMessage: 'Expédition déclarée' },
    onSuccess: (contrat) => majContrat(queryClient, contrat),
  })
}

/** Jalon de livraison atteint (contrat EN_LIVRAISON). */
export function useJalonAtteint(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ jalonId, dateReelle }: { jalonId: string; dateReelle: string }) =>
      portailApi.jalonAtteint(id, jalonId, dateReelle),
    meta: { successMessage: 'Jalon enregistré' },
    onSuccess: (contrat) => majContrat(queryClient, contrat),
  })
}

/** Depot d'une facture (erreurs de champs gerees par le dialogue). */
export function useDeposerFacture() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: FactureDepot) => portailApi.deposerFacture(body),
    meta: { successMessage: 'Facture déposée', silentError: true },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: factureKeys.all }),
  })
}

/** Mise a jour des coordonnees de l'entreprise (verrou optimiste `version`). */
export function useModifierProfil() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ProfilModification) => portailApi.modifierProfil(body),
    meta: { successMessage: 'Profil enregistré', silentError: true },
    onSuccess: (profil) => {
      queryClient.setQueryData(entrepriseKeys.custom('profil'), profil)
    },
  })
}

/** Ajout ou modification d'une ligne de catalogue (upsert par reference). */
export function usePublierLigne() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: LigneCatalogueSaisie) => portailApi.publierLigne(body),
    meta: { successMessage: 'Catalogue mis à jour', silentError: true },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: entrepriseKeys.custom('catalogue') }),
  })
}
