import { forwardRef, type InputHTMLAttributes, type ReactNode, type TextareaHTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

/** Classes communes aux champs de saisie (reutilisables par Select, Combobox, DatePicker). */
export const fieldClasses =
  'w-full rounded-lg border border-input bg-surface text-sm text-foreground shadow-xs transition-colors placeholder:text-muted-foreground/80 hover:border-border-strong focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30 focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:bg-muted disabled:opacity-60 aria-[invalid=true]:border-danger aria-[invalid=true]:focus-visible:ring-danger/30'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  /** Icone a gauche (recherche, devise...). */
  leftIcon?: ReactNode
  /** Element a droite (unite, bouton effacer...). */
  rightSlot?: ReactNode
  /** Classes du conteneur (largeur) quand une icone ou un slot est present. */
  containerClassName?: string
}

/** Champ texte. Utiliser dans un <Field>/<FormField> pour le libelle et les erreurs. */
export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, leftIcon, rightSlot, containerClassName, type = 'text', ...props },
  ref,
) {
  const input = (
    <input
      ref={ref}
      type={type}
      className={cn(fieldClasses, 'h-10 px-3 sm:h-9', leftIcon && 'pl-9', rightSlot && 'pr-10', className)}
      {...props}
    />
  )
  if (!leftIcon && !rightSlot) return input
  return (
    <div className={cn('relative w-full', containerClassName)}>
      {leftIcon && (
        <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground [&_svg]:size-4">
          {leftIcon}
        </span>
      )}
      {input}
      {rightSlot && (
        <span className="absolute right-1.5 top-1/2 flex -translate-y-1/2 items-center">{rightSlot}</span>
      )}
    </div>
  )
})

/** Zone de texte multi-lignes. */
export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function Textarea({ className, rows = 4, ...props }, ref) {
    return (
      <textarea
        ref={ref}
        rows={rows}
        className={cn(fieldClasses, 'min-h-20 px-3 py-2', className)}
        {...props}
      />
    )
  },
)
