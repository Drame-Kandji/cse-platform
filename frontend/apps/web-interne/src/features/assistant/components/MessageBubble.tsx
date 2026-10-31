import { CircleAlert, RotateCw, Sparkles, TriangleAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { formatDateTime } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { SOURCE_TYPE_LABELS, sourceLink } from '../lib/source-link'
import type { ChatMessage } from '../types'
import { Markdown } from './Markdown'

export interface MessageBubbleProps {
  message: ChatMessage
  /** Relance une question en erreur. */
  onRetry?: (message: ChatMessage) => void
  /** Appele lors d'un clic sur une source (ex. fermer le panneau sur mobile). */
  onNavigate?: () => void
}

/** Bulle de conversation : question de l'utilisateur ou reponse de l'assistant (markdown + sources). */
export function MessageBubble({ message, onRetry, onNavigate }: MessageBubbleProps) {
  if (message.role === 'UTILISATEUR') {
    return (
      <li className="flex flex-col items-end gap-1.5">
        <span className="sr-only">Vous :</span>
        <p className="max-w-[85%] whitespace-pre-wrap break-words rounded-2xl rounded-br-sm bg-primary px-4 py-2.5 text-sm text-primary-foreground">
          {message.contenu}
        </p>
        {message.erreur && (
          <div
            role="alert"
            className="flex max-w-[85%] flex-wrap items-center justify-end gap-2 text-xs text-danger-emphasis"
          >
            <CircleAlert className="size-3.5 shrink-0" aria-hidden />
            <span>{message.erreur}</span>
            {onRetry && (
              <Button
                variant="ghost"
                size="sm"
                leftIcon={<RotateCw />}
                onClick={() => {
                  onRetry(message)
                }}
              >
                Réessayer
              </Button>
            )}
          </div>
        )}
      </li>
    )
  }
  return (
    <li className="flex gap-3">
      <span className="grid size-8 shrink-0 place-items-center rounded-full bg-primary/10 text-primary-emphasis">
        <Sparkles className="size-4" aria-hidden />
      </span>
      <div className="grid min-w-0 max-w-[85%] gap-2">
        <span className="sr-only">Assistant :</span>
        {message.modeDegrade && (
          <p className="flex items-start gap-2 rounded-lg bg-warning/10 px-3 py-2 text-xs text-warning-emphasis">
            <TriangleAlert className="mt-px size-3.5 shrink-0" aria-hidden />
            <span>
              <strong className="font-semibold">Mode dégradé</strong> — l'IA générative est momentanément
              indisponible ; réponse construite à partir des données de la plateforme.
            </span>
          </p>
        )}
        <div className="rounded-2xl rounded-tl-sm border border-border bg-surface-sunken px-4 py-3 text-foreground">
          <Markdown source={message.contenu} />
        </div>
        {message.sources.length > 0 && (
          <ul className="flex flex-wrap gap-1.5" aria-label="Sources">
            {message.sources.map((source) => (
              <li key={`${source.type}-${source.id}`}>
                <Link
                  to={sourceLink(source)}
                  onClick={onNavigate}
                  className={cn(
                    'inline-flex max-w-full items-center gap-1.5 rounded-md border border-border bg-surface px-2 py-1 text-xs transition-colors hover:border-border-strong hover:bg-muted',
                  )}
                >
                  <span className="text-muted-foreground">{SOURCE_TYPE_LABELS[source.type]}</span>
                  <span className="truncate font-medium">{source.libelle}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
        <time dateTime={message.horodatage} className="text-2xs text-muted-foreground">
          {formatDateTime(message.horodatage)}
        </time>
      </div>
    </li>
  )
}
