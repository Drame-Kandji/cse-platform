import { http } from '@/shared/api/http'
import type { PageParams, PageResponse } from '@/shared/api/types'
import type { Conversation, ConversationResume, Question, Reponse } from '../types'

export const assistantApi = {
  poser: (body: Question) => http.post<Reponse>('/chatbot/query', body),
  historique: (params: PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<ConversationResume>>('/chatbot/historique', { query: { ...params }, signal }),
  conversation: (id: string, signal?: AbortSignal) =>
    http.get<Conversation>(`/chatbot/historique/${encodeURIComponent(id)}`, { signal }),
  supprimer: (id: string) => http.del(`/chatbot/historique/${encodeURIComponent(id)}`),
  suggestions: (signal?: AbortSignal) => http.get<string[]>('/chatbot/suggestions', { signal }),
}
