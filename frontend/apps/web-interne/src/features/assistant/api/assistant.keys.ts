import { createQueryKeys } from '@/shared/api/query-keys'

export const assistantKeys = createQueryKeys('chatbot')
export const suggestionsKey = assistantKeys.custom('suggestions')
