import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useLocation } from 'react-router-dom'
import { chatbotApi } from '../../api/endpoints'
import { getApiErrorMessage } from '../../api/client'
import { cn } from '../../lib/cn'
import { Icon } from '../ui/Icon'
import { useToast } from '../ui/Toast'

const SUGGESTIONS = [
  'Quelles pelles sont disponibles au parc ?',
  'Où en sont les besoins en attente ?',
  'État du contrat CTR-2026-001 ?',
]

export function ChatbotWidget() {
  const location = useLocation()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [open, setOpen] = useState(false)
  const [question, setQuestion] = useState('')
  const endRef = useRef<HTMLDivElement | null>(null)

  const history = useQuery({
    queryKey: ['chatbot', 'historique'],
    queryFn: chatbotApi.historique,
    enabled: open,
  })

  const send = useMutation({
    mutationFn: (value: string) => chatbotApi.query(value),
    onSuccess: () => {
      setQuestion('')
      void queryClient.invalidateQueries({ queryKey: ['chatbot', 'historique'] })
    },
    onError: (error) => toast.error('Assistant indisponible', getApiErrorMessage(error)),
  })

  const entries = useMemo(() => (history.data ?? []).slice(0, 8), [history.data])

  useEffect(() => {
    if (open) endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [entries.length, open, send.isPending])

  if (location.pathname.startsWith('/chatbot')) return null

  function submit(value: string) {
    const trimmed = value.trim()
    if (!trimmed || send.isPending) return
    send.mutate(trimmed)
  }

  return (
    <>
      {open ? (
        <div className="fixed bottom-24 right-4 z-[950] flex max-h-[min(620px,calc(100vh-8rem))] w-[calc(100vw-2rem)] max-w-md animate-slide-up flex-col overflow-hidden rounded-2xl border border-ink-200 bg-white shadow-float sm:right-6">
          <div className="flex items-center gap-3 bg-brand-950 px-4 py-3 text-white">
            <span className="grid h-9 w-9 place-items-center rounded-full bg-accent-500 text-brand-950">
              <Icon name="bot" size={20} />
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold">Assistant IA CSE</p>
              <p className="flex items-center gap-1.5 text-[11px] text-brand-200">
                <span className="h-1.5 w-1.5 rounded-full bg-success-500" />
                ms-chatbot · connecté aux référentiels
              </p>
            </div>
            <button type="button" onClick={() => setOpen(false)} className="rounded-lg p-1.5 text-brand-200 hover:bg-white/10" aria-label="Fermer l’assistant">
              <Icon name="x" size={18} />
            </button>
          </div>

          <div className="flex-1 space-y-3 overflow-y-auto bg-ink-100/40 px-4 py-4">
            <div className="rounded-xl rounded-tl-sm border border-ink-200 bg-white px-3 py-2.5 text-xs leading-relaxed text-ink-600">
              Bonjour. Je peux consulter le parc, les besoins, les achats et les fournisseurs CSE. Posez votre question ou choisissez une suggestion.
            </div>
            {history.isLoading ? <p className="text-center text-xs text-ink-500">Chargement de l’historique…</p> : null}
            {entries.map((entry) => (
              <div key={entry.id} className="space-y-2">
                <div className="ml-8 rounded-xl rounded-tr-sm bg-brand-900 px-3 py-2 text-xs leading-relaxed text-white">
                  {entry.question}
                </div>
                <div className="mr-2 rounded-xl rounded-tl-sm border border-ink-200 bg-white px-3 py-2.5">
                  <span className="inline-flex rounded-full bg-accent-50 px-2 py-0.5 text-[9px] font-bold uppercase tracking-wide text-accent-700">
                    {entry.intentionDetectee.replaceAll('_', ' ')}
                  </span>
                  <p className="mt-1.5 text-xs leading-relaxed text-ink-600">{entry.reponse}</p>
                </div>
              </div>
            ))}
            {send.isPending ? (
              <div className="mr-2 flex items-center gap-2 rounded-xl border border-ink-200 bg-white px-3 py-2.5 text-xs text-ink-500">
                <span className="h-3 w-3 animate-spin rounded-full border-2 border-brand-200 border-t-brand-700" />
                Analyse de votre question…
              </div>
            ) : null}
            <div ref={endRef} />
          </div>

          <div className="border-t border-ink-100 bg-white px-3 pb-3 pt-2">
            <div className="mb-2 flex gap-1.5 overflow-x-auto">
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  onClick={() => submit(suggestion)}
                  className="shrink-0 rounded-full border border-ink-200 px-2.5 py-1 text-[10px] font-medium text-ink-600 transition hover:border-brand-300 hover:bg-brand-50"
                >
                  {suggestion}
                </button>
              ))}
            </div>
            <form
              onSubmit={(event) => {
                event.preventDefault()
                submit(question)
              }}
              className="flex items-center gap-2"
            >
              <input
                value={question}
                onChange={(event) => setQuestion(event.target.value)}
                placeholder="Posez votre question…"
                className="input h-9 text-xs"
                aria-label="Question à l’assistant"
              />
              <button
                type="submit"
                disabled={!question.trim() || send.isPending}
                className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-brand-900 text-white disabled:opacity-40"
                aria-label="Envoyer"
              >
                <Icon name="send" size={16} />
              </button>
            </form>
          </div>
        </div>
      ) : null}

      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className={cn(
          'fixed bottom-5 right-4 z-[940] flex h-14 items-center gap-2 rounded-full bg-brand-900 px-4 text-white shadow-float transition hover:bg-brand-800 sm:right-6',
          open && 'bg-accent-500 text-brand-950 hover:bg-accent-400',
        )}
        aria-label={open ? 'Fermer l’assistant IA' : 'Ouvrir l’assistant IA'}
      >
        <Icon name={open ? 'x' : 'bot'} size={22} />
        <span className="hidden text-sm font-semibold sm:inline">{open ? 'Fermer' : 'Assistant IA'}</span>
        {!open ? <span className="absolute -right-0.5 -top-0.5 h-3 w-3 rounded-full border-2 border-white bg-accent-500" /> : null}
      </button>
    </>
  )
}
