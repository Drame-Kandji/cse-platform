import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { fournisseursApi } from '../api/fournisseurs.api'
import { fournisseurKeys, litigeKeys } from '../api/fournisseurs.keys'
import type {
  DecisionBlacklistage,
  DocumentSaisie,
  EvaluationSaisie,
  FournisseurModification,
  FournisseurSaisie,
  LigneCatalogueSaisie,
  LitigeActionSaisie,
  LitigeSaisie,
} from '../api/types'

function useInvalidateFournisseur(id?: string) {
  const queryClient = useQueryClient()
  return async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: fournisseurKeys.lists() }),
      queryClient.invalidateQueries({ queryKey: fournisseurKeys.custom('statistiques') }),
      id ? queryClient.invalidateQueries({ queryKey: fournisseurKeys.detail(id) }) : Promise.resolve(),
    ])
  }
}

/** Creation d'un fournisseur (RA) ; erreurs gerees par le formulaire. */
export function useCreerFournisseur() {
  const invalidate = useInvalidateFournisseur()
  return useMutation({
    mutationFn: (body: FournisseurSaisie) => fournisseursApi.creer(body),
    meta: { successMessage: 'Fournisseur enregistré', silentError: true },
    onSuccess: invalidate,
  })
}

/** Modification d'un fournisseur (RA, verrou optimiste `version`). */
export function useModifierFournisseur(id: string) {
  const invalidate = useInvalidateFournisseur(id)
  return useMutation({
    mutationFn: (body: FournisseurModification) => fournisseursApi.modifier(id, body),
    meta: { successMessage: 'Fournisseur mis à jour', silentError: true },
    onSuccess: invalidate,
  })
}

export type ActionStatut =
  | { type: 'valider' }
  | { type: 'suspendre'; motif: string }
  | { type: 'reactiver' }
  | { type: 'demanderBlacklistage'; motif: string }
  | { type: 'deciderBlacklistage'; decision: DecisionBlacklistage }

const SUCCES: Record<ActionStatut['type'], string> = {
  valider: 'Inscription validée : un e-mail a été envoyé au fournisseur pour définir son mot de passe',
  suspendre: 'Fournisseur suspendu',
  reactiver: 'Fournisseur réactivé',
  demanderBlacklistage: 'Demande de blacklistage transmise à la Direction Générale',
  deciderBlacklistage: 'Décision de blacklistage enregistrée',
}

/** Changements de statut d'un fournisseur (validation, suspension, blacklistage en 2 etapes R09). */
export function useActionStatutFournisseur(id: string) {
  const invalidate = useInvalidateFournisseur(id)
  return useMutation({
    mutationFn: (action: ActionStatut) => {
      switch (action.type) {
        case 'valider':
          return fournisseursApi.validerInscription(id)
        case 'suspendre':
          return fournisseursApi.suspendre(id, action.motif)
        case 'reactiver':
          return fournisseursApi.reactiver(id)
        case 'demanderBlacklistage':
          return fournisseursApi.demanderBlacklistage(id, action.motif)
        case 'deciderBlacklistage':
          return fournisseursApi.deciderBlacklistage(id, action.decision)
      }
    },
    onSuccess: async (_data, action) => {
      toast.success(SUCCES[action.type])
      await invalidate()
    },
  })
}

/** Ajout d'un document administratif (RA). */
export function useAjouterDocument(id: string) {
  const invalidate = useInvalidateFournisseur(id)
  return useMutation({
    mutationFn: (body: DocumentSaisie) => fournisseursApi.ajouterDocument(id, body),
    meta: { successMessage: 'Document ajouté' },
    onSuccess: invalidate,
  })
}

/** Evaluation d'un fournisseur (RA, RM) : le score est recalcule cote serveur (R07). */
export function useEvaluerFournisseur(id: string) {
  const queryClient = useQueryClient()
  const invalidate = useInvalidateFournisseur(id)
  return useMutation({
    mutationFn: (body: EvaluationSaisie) => fournisseursApi.evaluer(id, body),
    meta: { successMessage: 'Évaluation enregistrée' },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: fournisseurKeys.custom('evaluations', id) })
      await invalidate()
    },
  })
}

/** Ajout ou mise a jour d'une ligne de catalogue (RA, historise le prix). */
export function useEnregistrerLigneCatalogue(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: LigneCatalogueSaisie) => fournisseursApi.enregistrerLigne(id, body),
    meta: { successMessage: 'Ligne de catalogue enregistrée' },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: fournisseurKeys.custom('catalogue', id) })
      await queryClient.invalidateQueries({ queryKey: fournisseurKeys.custom('prix', id) })
    },
  })
}

/** Ouverture d'un litige (RA, RM). */
export function useOuvrirLitige(id: string) {
  const queryClient = useQueryClient()
  const invalidate = useInvalidateFournisseur(id)
  return useMutation({
    mutationFn: (body: LitigeSaisie) => fournisseursApi.ouvrirLitige(id, body),
    meta: { successMessage: 'Litige ouvert' },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: litigeKeys.lists() })
      await invalidate()
    },
  })
}

/** Action sur un litige (RA) : suivi, resolution ou classement. */
export function useActionLitige(fournisseurId: string) {
  const queryClient = useQueryClient()
  const invalidate = useInvalidateFournisseur(fournisseurId)
  return useMutation({
    mutationFn: ({ litigeId, body }: { litigeId: string; body: LitigeActionSaisie }) =>
      fournisseursApi.agirLitige(litigeId, body),
    meta: { successMessage: 'Litige mis à jour' },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: litigeKeys.lists() })
      await invalidate()
    },
  })
}
