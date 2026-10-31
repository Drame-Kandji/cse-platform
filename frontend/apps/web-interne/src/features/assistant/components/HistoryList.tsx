import { MessagesSquare, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { formatRelative } from '@/shared/lib/format'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import { IconButton } from '@/shared/ui/button'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { useHistorique, useSupprimerConversation } from '../hooks/useAssistant'
import type { ConversationResume } from '../types'

export interface HistoryListProps {
  activeId?: string
  onOpen: (id: string) => void
  /** Appele quand la conversation ouverte est supprimee. */
  onDeleted: (id: string) => void
}

/** Historique des conversations : reprise et suppression (avec confirmation). */
export function HistoryList({ activeId, onOpen, onDeleted }: HistoryListProps) {
  const query = useHistorique(true)
  const supprimer = useSupprimerConversation()
  const [toDelete, setToDelete] = useState<ConversationResume | null>(null)

  if (query.isPending) {
    return (
      <ul className="grid gap-3" aria-label="Chargement de l'historique">
        {[0, 1, 2, 3].map((index) => (
          <li key={index} className="grid gap-1.5">
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-3 w-full" />
          </li>
        ))}
      </ul>
    )
  }
  if (query.isError) {
    return <ErrorState size="compact" error={query.error} onRetry={() => void query.refetch()} />
  }
  const items = query.data.content
  if (items.length === 0) {
    return (
      <EmptyState
        size="compact"
        icon={<MessagesSquare aria-hidden />}
        title="Aucune conversation"
        description="Vos échanges avec l'assistant seront conservés ici."
      />
    )
  }
  return (
    <>
      <ul className="grid gap-1.5" aria-label="Conversations précédentes">
        {items.map((item) => (
          <li
            key={item.id}
            className="group flex items-start gap-1 rounded-xl border border-border bg-surface transition-colors hover:border-border-strong data-[active=true]:border-primary/40 data-[active=true]:bg-primary/5"
            data-active={item.id === activeId}
          >
            <button
              type="button"
              onClick={() => {
                onOpen(item.id)
              }}
              className="grid min-w-0 flex-1 gap-0.5 rounded-xl px-3.5 py-2.5 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <span className="truncate text-sm font-medium">{item.titre}</span>
              <span className="line-clamp-2 text-xs text-muted-foreground">{item.dernierMessage}</span>
              <span className="text-2xs text-muted-foreground">
                {item.nombreMessages} message(s) · {formatRelative(item.updatedAt)}
              </span>
            </button>
            <IconButton
              label={`Supprimer « ${item.titre} »`}
              icon={<Trash2 />}
              size="icon-sm"
              className="m-1.5 text-muted-foreground hover:text-danger-emphasis"
              onClick={() => {
                setToDelete(item)
              }}
            />
          </li>
        ))}
      </ul>
      <ConfirmDialog
        open={toDelete !== null}
        onOpenChange={(open) => {
          if (!open) setToDelete(null)
        }}
        tone="danger"
        title="Supprimer cette conversation ?"
        description={`« ${toDelete?.titre ?? ''} » sera définitivement supprimée de votre historique.`}
        confirmLabel="Supprimer"
        onConfirm={async () => {
          if (!toDelete) return
          const id = toDelete.id
          // L'erreur est deja notifiee par le toast global des mutations.
          await supprimer.mutateAsync(id).then(
            () => {
              onDeleted(id)
            },
            () => undefined,
          )
        }}
      />
    </>
  )
}
