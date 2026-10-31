import type { FieldPath, FieldValues, UseFormSetError } from 'react-hook-form'
import { isApiError } from '@/shared/api/errors'

/**
 * Reporte les erreurs de champs d'un ProblemDetail (400/422) sur un formulaire react-hook-form.
 * @returns vrai si au moins une erreur a ete appliquee (sinon afficher un toast/ErrorState)
 * @example onError: (error) => { if (!applyServerErrors(error, form.setError)) toast.error(getErrorMessage(error)) }
 */
export function applyServerErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
): boolean {
  if (!isApiError(error) || error.fieldErrors.length === 0) return false
  error.fieldErrors.forEach((fieldError, index) => {
    setError(
      fieldError.field as FieldPath<T>,
      { type: 'server', message: fieldError.message },
      { shouldFocus: index === 0 },
    )
  })
  return true
}
