import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

/**
 * Filtre mono-valeur synchronise avec un parametre d'URL (?mstatut=EN_COURS) ; l'API n'accepte
 * qu'une valeur par parametre. Remet a zero la page du tableau associe (`pageKey`).
 */
export function useEnumParam<V extends string>(key: string, allowed: readonly V[], pageKey: string) {
  const [params, setParams] = useSearchParams()
  const raw = params.get(key)
  const value = raw && (allowed as readonly string[]).includes(raw) ? (raw as V) : undefined
  const setValue = useCallback(
    (next: V | undefined) => {
      setParams(
        (current) => {
          const updated = new URLSearchParams(current)
          if (next) updated.set(key, next)
          else updated.delete(key)
          updated.delete(pageKey)
          return updated
        },
        { replace: true },
      )
    },
    [key, pageKey, setParams],
  )
  return [value, setValue] as const
}
