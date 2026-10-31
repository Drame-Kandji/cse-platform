import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api'
import { chantiersApi } from '../api/chantiers.api'
import { chantierKeys } from '../api/chantiers.keys'
import type { Chantier, ChantierFiltres, ChantierModification, ChantierPayload } from '../api/types'

/** Liste paginee des chantiers (un chef ne recoit que les siens). */
export function useChantiers(params: ChantierFiltres & PageParams) {
  return useQuery({
    queryKey: chantierKeys.list(params),
    queryFn: ({ signal }) => chantiersApi.lister(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Detail d'un chantier (panneau lateral). */
export function useChantier(id: string | null) {
  return useQuery({
    queryKey: chantierKeys.detail(id ?? ''),
    queryFn: ({ signal }) => chantiersApi.detail(id ?? '', signal),
    enabled: id !== null,
  })
}

/** Affectations en cours d'un chantier. */
export function useAffectationsChantier(id: string | null) {
  return useQuery({
    queryKey: ['affectations', 'list', { chantierId: id, statut: 'EN_COURS' }] as const,
    queryFn: ({ signal }) => chantiersApi.affectations(id ?? '', signal),
    enabled: id !== null,
  })
}

/** Comptes chefs de chantier (selection reservee a l'ADMIN). */
export function useChefsChantier(enabled: boolean) {
  return useQuery({
    queryKey: chantierKeys.custom('chefs'),
    queryFn: ({ signal }) => chantiersApi.chefs(signal),
    enabled,
    staleTime: 5 * 60_000,
  })
}

function useSynchroniser() {
  const queryClient = useQueryClient()
  return (chantier: Chantier) => {
    queryClient.setQueryData(chantierKeys.detail(chantier.id), chantier)
    void queryClient.invalidateQueries({ queryKey: chantierKeys.lists() })
  }
}

/** Creation d'un chantier (RM, ADMIN) ; erreurs de champs gerees par le formulaire. */
export function useCreerChantier() {
  const synchroniser = useSynchroniser()
  return useMutation({
    mutationFn: (body: ChantierPayload) => chantiersApi.creer(body),
    meta: { successMessage: 'Chantier créé', silentError: true },
    onSuccess: synchroniser,
  })
}

/** Modification d'un chantier (verrou optimiste `version`). */
export function useModifierChantier() {
  const synchroniser = useSynchroniser()
  return useMutation({
    mutationFn: ({ id, ...body }: ChantierModification & { id: string }) => chantiersApi.modifier(id, body),
    meta: { successMessage: 'Chantier modifié', silentError: true },
    onSuccess: synchroniser,
  })
}

/** Cloture d'un chantier (RM) : refusee (422) tant que des materiels y sont affectes. */
export function useTerminerChantier() {
  const synchroniser = useSynchroniser()
  return useMutation({
    mutationFn: (id: string) => chantiersApi.terminer(id),
    meta: { successMessage: 'Chantier terminé' },
    onSuccess: synchroniser,
  })
}
