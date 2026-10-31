import { AlertCircle } from 'lucide-react'
import { useId, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import {
  useController,
  type Control,
  type ControllerFieldState,
  type ControllerRenderProps,
  type FieldPath,
  type FieldValues,
} from 'react-hook-form'
import { cn } from '@/shared/lib/cn'

/** Props d'accessibilite a etaler sur le controle (Input, Select...). */
export interface FieldControlProps {
  id: string
  'aria-invalid': boolean | undefined
  'aria-describedby': string | undefined
  'aria-required': boolean | undefined
}

export interface FieldProps {
  label: ReactNode
  hint?: ReactNode
  error?: string
  required?: boolean
  /** Affiche la mention "Facultatif" a cote du libelle. */
  optional?: boolean
  /** Element a droite du libelle (lien d'aide, compteur). */
  labelAction?: ReactNode
  id?: string
  className?: string
  children: (control: FieldControlProps) => ReactNode
}

/**
 * Champ de formulaire presentational : libelle, aide, erreur, ids ARIA relies.
 * @example <Field label="Designation" error={errors.designation?.message}>{(p) => <Input {...p} {...register('designation')} />}</Field>
 */
export function Field({
  label,
  hint,
  error,
  required,
  optional,
  labelAction,
  id,
  className,
  children,
}: FieldProps) {
  const { t } = useTranslation()
  const generated = useId()
  const controlId = id ?? generated
  const hintId = hint ? `${controlId}-hint` : undefined
  const errorId = error ? `${controlId}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
  return (
    <div className={cn('grid gap-1.5', className)}>
      <div className="flex items-center justify-between gap-2">
        <label htmlFor={controlId} className="text-sm font-medium text-foreground">
          {label}
          {required && (
            <span className="ml-0.5 text-danger-emphasis" aria-hidden>
              *
            </span>
          )}
          {/* Espace explicite : nom accessible « Garantie (mois) Facultatif » et non « …)Facultatif ». */}
          {optional && (
            <>
              {' '}
              <span className="ml-1 text-xs font-normal text-muted-foreground">{t('states.optional')}</span>
            </>
          )}
        </label>
        {labelAction}
      </div>
      {children({
        id: controlId,
        'aria-invalid': error ? true : undefined,
        'aria-describedby': describedBy,
        'aria-required': required || undefined,
      })}
      {hint && !error && (
        <p id={hintId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
      {error && (
        <p
          id={errorId}
          role="alert"
          className="flex items-center gap-1 text-xs font-medium text-danger-emphasis"
        >
          <AlertCircle className="size-3.5 shrink-0" aria-hidden />
          {error}
        </p>
      )}
    </div>
  )
}

export interface FormFieldProps<
  TValues extends FieldValues,
  TName extends FieldPath<TValues>,
  TTransformed extends FieldValues = TValues,
> extends Omit<FieldProps, 'error' | 'children'> {
  /** `form.control` ; supporte useForm<Entree, unknown, Sortie> (schema zod avec transformation). */
  control: Control<TValues, unknown, TTransformed>
  name: TName
  children: (args: {
    field: ControllerRenderProps<TValues, TName>
    fieldState: ControllerFieldState
    control: FieldControlProps
  }) => ReactNode
}

/**
 * Champ relie a react-hook-form (via useController) : l'erreur zod s'affiche seule.
 * @example
 * <FormField control={form.control} name="priorite" label="Priorite" required>
 *   {({ field, control }) => <Select {...control} options={optionsOf('priorite')} value={field.value} onValueChange={field.onChange} />}
 * </FormField>
 */
export function FormField<
  TValues extends FieldValues,
  TName extends FieldPath<TValues>,
  TTransformed extends FieldValues = TValues,
>({ control, name, children, ...fieldProps }: FormFieldProps<TValues, TName, TTransformed>) {
  const { field, fieldState } = useController<TValues, TName, TTransformed>({ control, name })
  return (
    <Field {...fieldProps} error={fieldState.error?.message}>
      {(controlProps) => children({ field, fieldState, control: controlProps })}
    </Field>
  )
}
