/**
 * Fabrique de cles TanStack Query hierarchiques pour un module.
 * Permet d'invalider finement : `queryClient.invalidateQueries({ queryKey: besoinKeys.lists() })`.
 *
 * @param scope identifiant unique du module (ex. "besoins")
 * @example
 * export const besoinKeys = createQueryKeys('besoins')
 * besoinKeys.list({ page: 0, statut: 'EN_ATTENTE' }) // ['besoins', 'list', { page: 0, statut: 'EN_ATTENTE' }]
 * besoinKeys.detail(id)                              // ['besoins', 'detail', id]
 * besoinKeys.custom('historique', id)                // ['besoins', 'historique', id]
 */
export function createQueryKeys<const S extends string>(scope: S) {
  return {
    all: [scope] as const,
    lists: () => [scope, 'list'] as const,
    list: <P extends object>(params: P) => [scope, 'list', params] as const,
    details: () => [scope, 'detail'] as const,
    detail: (id: string) => [scope, 'detail', id] as const,
    custom: <const R extends readonly unknown[]>(...parts: R) => [scope, ...parts] as const,
  }
}
