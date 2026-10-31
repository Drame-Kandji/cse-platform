import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { typesEnginsApi } from '../api/admin.api'
import { typeEnginKeys } from '../api/admin.keys'

/** Referentiel complet des types d'engins. */
export function useTypesEngins() {
  return useQuery({ queryKey: typeEnginKeys.all, queryFn: ({ signal }) => typesEnginsApi.lister(signal) })
}

/** Creation ou modification d'un type d'engin (selon la presence d'un id). */
export function useEnregistrerTypeEngin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: Parameters<typeof typesEnginsApi.creer>[0] & { id?: string }) => {
      const { id, ...body } = input
      return id ? typesEnginsApi.modifier({ id, ...body }) : typesEnginsApi.creer(body)
    },
    meta: { successMessage: "Type d'engin enregistré", silentError: true },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: typeEnginKeys.all }),
  })
}
