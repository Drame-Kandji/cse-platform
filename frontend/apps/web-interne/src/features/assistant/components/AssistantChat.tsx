import { ArrowLeft, History, MessageSquarePlus } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Button } from '@/shared/ui/button'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton, Spinner } from '@/shared/ui/skeleton'
import { useSuggestions, type AssistantChatState } from '../hooks/useAssistant'
import { Composer } from './Composer'
import { HistoryList } from './HistoryList'
import { MessageBubble } from './MessageBubble'

export interface AssistantChatProps {
  /** Etat de conversation (`useAssistantChat()`), detenu par le parent pour survivre a la fermeture. */
  chat: AssistantChatState
  /** Prenom affiche dans l'accueil. */
  firstName?: string
  /** Appele lors d'une navigation depuis une source (ex. fermer le panneau sur mobile). */
  onNavigate?: () => void
}

function TypingIndicator() {
  return (
    <li className="flex items-center" aria-live="polite">
      <span className="flex items-center gap-1 rounded-2xl rounded-tl-sm border border-border bg-surface-sunken px-4 py-3">
        <span className="sr-only">L'assistant rédige une réponse…</span>
        {[0, 150, 300].map((delay) => (
          <span
            key={delay}
            aria-hidden
            className="size-1.5 animate-bounce rounded-full bg-muted-foreground"
            style={{ animationDelay: `${delay}ms` }}
          />
        ))}
      </span>
    </li>
  )
}

function Suggestions({ onPick }: { onPick: (question: string) => void }) {
  const query = useSuggestions()
  if (query.isPending) {
    return (
      <div className="grid gap-2">
        {[0, 1, 2].map((index) => (
          <Skeleton key={index} className="h-10 w-full rounded-xl" />
        ))}
      </div>
    )
  }
  const suggestions = query.data ?? []
  if (suggestions.length === 0) return null
  return (
    <div className="grid gap-2">
      <p className="text-xs text-muted-foreground">Suggestions</p>
      <ul className="grid gap-2">
        {suggestions.map((suggestion) => (
          <li key={suggestion}>
            <button
              type="button"
              onClick={() => {
                onPick(suggestion)
              }}
              className="w-full rounded-lg border border-border bg-surface px-3.5 py-2.5 text-left text-sm transition-colors hover:border-primary/40 hover:bg-primary/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {suggestion}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

/**
 * Conversation avec l'assistant IA : fil de messages, suggestions de depart, historique,
 * indicateur de saisie, mode degrade et erreurs en ligne.
 */
export function AssistantChat({ chat, firstName, onNavigate }: AssistantChatProps) {
  const [view, setView] = useState<'chat' | 'historique'>('chat')
  const composerRef = useRef<HTMLTextAreaElement>(null)
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (view === 'chat') composerRef.current?.focus()
  }, [view])
  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'end', behavior: 'smooth' })
  }, [chat.messages.length, chat.pending])

  const empty = chat.messages.length === 0

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex items-center justify-between gap-2 border-b border-border px-3 py-2">
        {view === 'historique' ? (
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<ArrowLeft />}
            onClick={() => {
              setView('chat')
            }}
          >
            Retour à la conversation
          </Button>
        ) : (
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<History />}
            onClick={() => {
              setView('historique')
            }}
          >
            Historique
          </Button>
        )}
        <Button
          variant="ghost"
          size="sm"
          leftIcon={<MessageSquarePlus />}
          disabled={empty && view === 'chat'}
          onClick={() => {
            chat.reset()
            setView('chat')
          }}
        >
          Nouvelle conversation
        </Button>
      </div>

      {view === 'historique' ? (
        <div className="scrollbar-thin min-h-0 flex-1 overflow-y-auto px-4 py-4">
          <HistoryList
            activeId={chat.conversationId}
            onOpen={(id) => {
              setView('chat')
              void chat.open(id)
            }}
            onDeleted={(id) => {
              if (id === chat.conversationId) chat.reset()
            }}
          />
        </div>
      ) : (
        <>
          <div className="scrollbar-thin min-h-0 flex-1 overflow-y-auto px-4 py-4" aria-live="polite">
            {chat.loadingConversation ? (
              <Spinner label="Chargement de la conversation…" />
            ) : chat.loadError ? (
              <ErrorState size="compact" error={chat.loadError} />
            ) : empty ? (
              <div className="grid gap-6">
                <p className="text-base font-medium">
                  Bonjour{firstName ? ` ${firstName}` : ''}, que souhaitez-vous savoir ?
                </p>
                <Suggestions
                  onPick={(question) => {
                    chat.send(question)
                  }}
                />
              </div>
            ) : (
              <ol className="grid gap-5" aria-label="Conversation">
                {chat.messages.map((message) => (
                  <MessageBubble
                    key={message.id}
                    message={message}
                    onNavigate={onNavigate}
                    onRetry={(failed) => {
                      chat.send(failed.contenu, failed.id)
                    }}
                  />
                ))}
                {chat.pending && <TypingIndicator />}
              </ol>
            )}
            <div ref={endRef} />
          </div>
          <div className="pb-safe border-t border-border px-4 py-3">
            <Composer ref={composerRef} onSend={chat.send} disabled={chat.pending} />
          </div>
        </>
      )}
    </div>
  )
}
