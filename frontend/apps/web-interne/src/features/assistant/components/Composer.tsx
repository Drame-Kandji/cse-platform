import { ArrowUp } from 'lucide-react'
import { forwardRef, useState, type KeyboardEvent } from 'react'
import { cn } from '@/shared/lib/cn'
import { IconButton } from '@/shared/ui/button'
import { QUESTION_MAX_LENGTH } from '../types'

export interface ComposerProps {
  onSend: (question: string) => void
  disabled?: boolean
}

/** Zone de saisie : Entree pour envoyer, Maj+Entree pour un saut de ligne, compteur 1000 caracteres. */
export const Composer = forwardRef<HTMLTextAreaElement, ComposerProps>(function Composer(
  { onSend, disabled },
  ref,
) {
  const [value, setValue] = useState('')
  const length = value.length
  const canSend = value.trim().length > 0 && length <= QUESTION_MAX_LENGTH && !disabled

  const submit = () => {
    if (!canSend) return
    onSend(value)
    setValue('')
  }
  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault()
      submit()
    }
  }

  return (
    <form
      className="grid gap-1.5"
      onSubmit={(event) => {
        event.preventDefault()
        submit()
      }}
    >
      <div className="flex items-end gap-2">
        <label htmlFor="assistant-question" className="sr-only">
          Votre question
        </label>
        <textarea
          ref={ref}
          id="assistant-question"
          rows={2}
          value={value}
          maxLength={QUESTION_MAX_LENGTH}
          onChange={(event) => {
            setValue(event.target.value)
          }}
          onKeyDown={onKeyDown}
          placeholder="Écrivez votre question…"
          aria-describedby="assistant-question-aide"
          className="scrollbar-thin max-h-40 min-h-11 flex-1 resize-none rounded-xl border border-input bg-surface px-3 py-2.5 text-sm placeholder:text-muted-foreground focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
        />
        <IconButton
          type="submit"
          variant="primary"
          label="Envoyer"
          icon={<ArrowUp />}
          disabled={!canSend}
          tooltip={false}
          className="size-11 rounded-xl"
        />
      </div>
      <p id="assistant-question-aide" className="flex justify-between gap-2 text-2xs text-muted-foreground">
        <span className="hidden sm:inline">Entrée pour envoyer, Maj + Entrée pour aller à la ligne</span>
        <span
          className={cn(
            'ml-auto tabular-nums',
            length >= QUESTION_MAX_LENGTH * 0.9 && 'text-warning-emphasis',
          )}
          aria-live="polite"
        >
          {length}/{QUESTION_MAX_LENGTH}
        </span>
      </p>
    </form>
  )
})
