import { useMutation, useQueryClient } from '@tanstack/react-query'
import { saveBlob } from '@/shared/api'
import { appelsOffresApi } from '../api/appels-offres.api'
import { appelOffreKeys, besoinAAcheterKeys } from '../api/appels-offres.keys'
import type {
  AppelOffre,
  AppelOffreCreation,
  DecisionApprobation,
  EvaluationOffre,
  OffreSaisie,
  TypeProcesVerbal,
} from '../api/types'

/** Rafraichit tout ce qui depend d'un appel d'offres (detail, offres, comparatif, listes). */
function useRefresh(id: string) {
  const queryClient = useQueryClient()
  return (ao?: AppelOffre) => {
    if (ao) queryClient.setQueryData(appelOffreKeys.detail(id), ao)
    void queryClient.invalidateQueries({ queryKey: appelOffreKeys.detail(id) })
    void queryClient.invalidateQueries({ queryKey: appelOffreKeys.custom('offres', id) })
    void queryClient.invalidateQueries({ queryKey: appelOffreKeys.custom('comparatif', id) })
    void queryClient.invalidateQueries({ queryKey: appelOffreKeys.lists() })
  }
}

/** Publication d'un appel d'offres (statut PUBLIE, invites verifies ACTIF : R14). */
export function useCreerAppelOffre() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: AppelOffreCreation) => appelsOffresApi.creer(body),
    meta: { successMessage: "Appel d'offres publié", silentError: true },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: appelOffreKeys.lists() })
      void queryClient.invalidateQueries({ queryKey: besoinAAcheterKeys.all })
    },
  })
}

/** Saisie d'une offre recue hors portail (RA ; refusee apres la date limite : R02). */
export function useSaisirOffre(id: string) {
  const refresh = useRefresh(id)
  return useMutation({
    mutationFn: (body: OffreSaisie) => appelsOffresApi.saisirOffre(id, body),
    meta: { successMessage: 'Offre enregistrée', silentError: true },
    onSuccess: () => {
      refresh()
    },
  })
}

/** Cloture de l'appel d'offres (leve l'anonymat des offres). */
export function useCloturerAppelOffre(id: string) {
  const refresh = useRefresh(id)
  return useMutation({
    mutationFn: () => appelsOffresApi.cloturer(id),
    meta: { successMessage: "Appel d'offres clôturé : les offres sont désormais nominatives" },
    onSuccess: refresh,
  })
}

/** Notation des criteres manuels d'une offre (0..100). */
export function useEvaluerOffre(id: string) {
  const refresh = useRefresh(id)
  return useMutation({
    mutationFn: ({ offreId, notes }: { offreId: string; notes: EvaluationOffre }) =>
      appelsOffresApi.evaluerOffre(id, offreId, notes),
    meta: { successMessage: 'Évaluation enregistrée' },
    onSuccess: () => {
      refresh()
    },
  })
}

/** Proposition d'attribution motivee (RA). */
export function useAttribuer(id: string) {
  const refresh = useRefresh(id)
  return useMutation({
    mutationFn: (body: { offreId: string; justification: string }) => appelsOffresApi.attribuer(id, body),
    meta: { successMessage: 'Attribution proposée : en attente des validations' },
    onSuccess: refresh,
  })
}

/**
 * Decision sur l'attribution (R11) : co-validation RM ou approbation DG.
 * Un refus supprime l'attribution ; toutes approbations OK => ATTRIBUE + contrat BROUILLON.
 */
export function useDecisionAttribution(id: string, niveau: 'coValidation' | 'approbationDg') {
  const refresh = useRefresh(id)
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: DecisionApprobation) =>
      niveau === 'coValidation' ? appelsOffresApi.coValider(id, body) : appelsOffresApi.approuver(id, body),
    meta: { successMessage: 'Décision enregistrée' },
    onSuccess: (ao) => {
      refresh(ao)
      void queryClient.invalidateQueries({ queryKey: ['contrats'] })
    },
  })
}

/** Telechargement d'un proces-verbal PDF (OUVERTURE ou EVALUATION). */
export function useTelechargerPv(id: string, reference: string) {
  return useMutation({
    mutationFn: async (type: TypeProcesVerbal) => {
      const file = await appelsOffresApi.procesVerbal(id, reference, type)
      saveBlob(file.blob, file.filename)
    },
  })
}
