import { useMutation, useQueryClient } from '@tanstack/react-query'
import { affectationKeys, chantierKeys, materielKeys } from '@/shared/suivi'
import { materielsApi, type MaterielCreation, type MaterielModification } from '../api/materiels.api'

/** Invalide la fiche, les listes du parc et le planning apres une action sur un materiel. */
function useInvalidateParc() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: materielKeys.all }),
      queryClient.invalidateQueries({ queryKey: affectationKeys.all }),
      queryClient.invalidateQueries({ queryKey: chantierKeys.all }),
    ])
}

/** Creation d'un materiel (RM) ; erreurs de champs gerees par le formulaire. */
export function useCreerMateriel() {
  const invalidate = useInvalidateParc()
  return useMutation({
    mutationFn: (body: MaterielCreation) => materielsApi.creer(body),
    meta: { successMessage: 'Matériel enregistré', silentError: true },
    onSuccess: invalidate,
  })
}

/** Modification des champs descriptifs (verrou optimiste `version`). */
export function useModifierMateriel(id: string) {
  const invalidate = useInvalidateParc()
  return useMutation({
    mutationFn: (body: MaterielModification) => materielsApi.modifier(id, body),
    meta: { successMessage: 'Fiche mise à jour', silentError: true },
    onSuccess: invalidate,
  })
}

/** Confirmation de reception sur chantier (affectation -> EN_COURS). */
export function useConfirmerReception(id: string) {
  const invalidate = useInvalidateParc()
  return useMutation({
    mutationFn: (body: { affectationId?: string; observations?: string }) =>
      materielsApi.confirmerReception(id, body),
    meta: { successMessage: 'Réception confirmée' },
    onSuccess: invalidate,
  })
}

/** Liberation du materiel (affectation TERMINEE, materiel DISPONIBLE). */
export function useLibererMateriel(id: string) {
  const invalidate = useInvalidateParc()
  return useMutation({
    mutationFn: (body: { dateFinReelle?: string; commentaire?: string }) => materielsApi.liberer(id, body),
    meta: { successMessage: 'Matériel libéré' },
    onSuccess: invalidate,
  })
}

/** Releve du compteur horaire. */
export function useReleverHeures(id: string) {
  const invalidate = useInvalidateParc()
  return useMutation({
    mutationFn: (heuresFonctionnement: number) => materielsApi.releverHeures(id, { heuresFonctionnement }),
    meta: { successMessage: 'Relevé enregistré', silentError: true },
    onSuccess: invalidate,
  })
}

/** Changement d'etat manuel DISPONIBLE <-> EN_MAINTENANCE (jamais REFORME). */
export function useChangerEtat(id: string) {
  const invalidate = useInvalidateParc()
  return useMutation({
    mutationFn: (body: { etat: 'DISPONIBLE' | 'EN_MAINTENANCE'; motif: string }) =>
      materielsApi.changerEtat(id, body),
    meta: { successMessage: 'État du matériel modifié' },
    onSuccess: invalidate,
  })
}

/** Demande de reforme (RM), soumise a la Direction Generale (R13). */
export function useDemanderReforme(id: string) {
  const invalidate = useInvalidateParc()
  return useMutation({
    mutationFn: (motif: string) => materielsApi.demanderReforme(id, { motif }),
    meta: { successMessage: 'Demande de réforme transmise à la Direction Générale' },
    onSuccess: invalidate,
  })
}

/** Decision de la Direction Generale sur une demande de reforme (R13, terminal si approuvee). */
export function useValiderReforme(id: string) {
  const invalidate = useInvalidateParc()
  return useMutation({
    mutationFn: (body: { approuve: boolean; commentaire?: string }) => materielsApi.validerReforme(id, body),
    onSuccess: invalidate,
  })
}
