import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useState } from 'react'
import { getErrorMessage } from '@/shared/api/errors'
import { createCorrelationId } from '@/shared/api/http'
import { assistantApi } from '../api/assistant.api'
import { assistantKeys, suggestionsKey } from '../api/assistant.keys'
import type { ChatMessage, Conversation } from '../types'

const HISTORY_PARAMS = { page: 0, size: 20 } as const

/** Questions types proposees selon le role (cache long : elles changent rarement). */
export function useSuggestions() {
  return useQuery({
    queryKey: suggestionsKey,
    queryFn: ({ signal }) => assistantApi.suggestions(signal),
    staleTime: 10 * 60_000,
  })
}

/** Historique des conversations (20 plus recentes). */
export function useHistorique(enabled: boolean) {
  return useQuery({
    queryKey: assistantKeys.list(HISTORY_PARAMS),
    queryFn: ({ signal }) => assistantApi.historique(HISTORY_PARAMS, signal),
    enabled,
  })
}

/** Suppression d'une conversation de l'historique. */
export function useSupprimerConversation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: assistantApi.supprimer,
    meta: { successMessage: 'Conversation supprimée' },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: assistantKeys.lists() }),
  })
}

function toChatMessages(conversation: Conversation): ChatMessage[] {
  return conversation.messages.map((message) => ({
    id: message.id,
    role: message.role,
    contenu: message.contenu,
    sources: message.sources,
    horodatage: message.horodatage,
  }))
}

/**
 * Etat d'une conversation avec l'assistant : envoi des questions, fil de messages,
 * reprise d'une conversation de l'historique. Les erreurs sont rattachees a la question.
 */
export function useAssistantChat() {
  const queryClient = useQueryClient()
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [conversationId, setConversationId] = useState<string | undefined>()
  const [loadingConversation, setLoadingConversation] = useState(false)
  const [loadError, setLoadError] = useState<unknown>(null)

  const mutation = useMutation({
    mutationFn: assistantApi.poser,
    meta: { silentError: true },
  })

  const send = useCallback(
    (question: string, retryOf?: string) => {
      const texte = question.trim()
      if (!texte || mutation.isPending) return
      const localId = retryOf ?? `local-${createCorrelationId()}`
      setMessages((current) => {
        const withoutRetried = current.filter((message) => message.id !== localId)
        return [
          ...withoutRetried,
          {
            id: localId,
            role: 'UTILISATEUR',
            contenu: texte,
            sources: [],
            horodatage: new Date().toISOString(),
          },
        ]
      })
      mutation.mutate(
        { question: texte, ...(conversationId ? { conversationId } : {}) },
        {
          onSuccess: (reponse) => {
            setConversationId(reponse.conversationId)
            setMessages((current) => [
              ...current,
              {
                id: reponse.messageId,
                role: 'ASSISTANT',
                contenu: reponse.reponse,
                sources: reponse.sources,
                horodatage: reponse.horodatage,
                modeDegrade: reponse.modeDegrade,
              },
            ])
            void queryClient.invalidateQueries({ queryKey: assistantKeys.lists() })
          },
          onError: (error) => {
            setMessages((current) =>
              current.map((message) =>
                message.id === localId ? { ...message, erreur: getErrorMessage(error) } : message,
              ),
            )
          },
        },
      )
    },
    [conversationId, mutation, queryClient],
  )

  const reset = useCallback(() => {
    setMessages([])
    setConversationId(undefined)
    setLoadError(null)
    mutation.reset()
  }, [mutation])

  const open = useCallback(
    async (id: string) => {
      setLoadingConversation(true)
      setLoadError(null)
      try {
        const conversation = await assistantApi.conversation(id)
        queryClient.setQueryData(assistantKeys.detail(id), conversation)
        setConversationId(conversation.id)
        setMessages(toChatMessages(conversation))
      } catch (error) {
        setLoadError(error)
      } finally {
        setLoadingConversation(false)
      }
    },
    [queryClient],
  )

  return {
    messages,
    conversationId,
    pending: mutation.isPending,
    loadingConversation,
    loadError,
    send,
    reset,
    open,
  }
}

/** Etat retourne par `useAssistantChat`. */
export type AssistantChatState = ReturnType<typeof useAssistantChat>
