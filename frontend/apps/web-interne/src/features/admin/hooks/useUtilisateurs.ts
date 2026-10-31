import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api/types'
import { chantiersApi, utilisateursApi } from '../api/admin.api'
import { roleKeys, utilisateurKeys } from '../api/admin.keys'
import type { UtilisateurFiltres } from '../types'

/** Liste paginee des comptes. */
export function useUtilisateurs(params: UtilisateurFiltres & PageParams) {
  return useQuery({
    queryKey: utilisateurKeys.list(params),
    queryFn: ({ signal }) => utilisateursApi.lister(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Roles avec effectifs et exigence OTP. */
export function useRoles() {
  return useQuery({ queryKey: roleKeys.all, queryFn: ({ signal }) => utilisateursApi.roles(signal) })
}

/** Chantiers proposes au rattachement d'un chef de chantier. */
export function useChantiersOptions(enabled: boolean) {
  return useQuery({
    queryKey: utilisateurKeys.custom('chantiers'),
    queryFn: ({ signal }) => chantiersApi.lister(signal),
    enabled,
    staleTime: 5 * 60_000,
  })
}

function useInvalidateUsers() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: utilisateurKeys.lists() }),
      queryClient.invalidateQueries({ queryKey: roleKeys.all }),
    ])
}

/** Creation d'un compte (erreurs de champs gerees par le formulaire). */
export function useCreerUtilisateur() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: utilisateursApi.creer,
    meta: { successMessage: 'Utilisateur créé', silentError: true },
    onSuccess: invalidate,
  })
}

/** Modification d'un compte. */
export function useModifierUtilisateur() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: utilisateursApi.modifier,
    meta: { successMessage: 'Utilisateur mis à jour', silentError: true },
    onSuccess: invalidate,
  })
}

/** Activation / desactivation d'un compte. */
export function useChangerStatut() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: utilisateursApi.changerStatut,
    meta: { successMessage: 'Statut du compte mis à jour' },
    onSuccess: invalidate,
  })
}

/** Reinitialisation du mot de passe (temporaire). */
export function useReinitialiserMotDePasse() {
  return useMutation({
    mutationFn: utilisateursApi.reinitialiserMotDePasse,
    meta: { successMessage: 'Mot de passe temporaire défini' },
  })
}
