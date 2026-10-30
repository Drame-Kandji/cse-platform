import axios, { AxiosError } from 'axios'
import type { PageResponse, ProblemDetail } from './types'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20_000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = window.localStorage.getItem('cse.access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** Déballe indifféremment un tableau brut ou un PageResponse<T> du backend. */
export function unwrapList<T>(data: T[] | PageResponse<T> | null | undefined): T[] {
  if (!data) return []
  return Array.isArray(data) ? data : (data.content ?? [])
}

const STATUS_MESSAGES: Record<number, string> = {
  400: 'La requête est invalide. Vérifiez les informations saisies.',
  401: 'Votre session a expiré. Reconnectez-vous.',
  403: "Vous n'avez pas les droits nécessaires pour cette action.",
  404: 'La ressource demandée est introuvable.',
  409: 'Cette opération entre en conflit avec une règle métier.',
  422: 'Les données ne respectent pas une règle de gestion.',
  500: 'Le service a rencontré une erreur interne.',
  503: 'Le service est momentanément indisponible.',
}

/** Traduit une erreur RFC 7807 (ProblemDetail) en message lisible en français. */
export function getApiErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ProblemDetail | string | undefined
    if (typeof data === 'string' && data.trim()) return data
    if (data && typeof data === 'object') {
      if (data.detail) return data.detail
      if (data.title) return data.title
      if (data.code) return `Règle métier ${data.code} : opération refusée.`
    }
    if (error.code === 'ECONNABORTED') return 'Le service met trop de temps à répondre.'
    if (!error.response) return 'Impossible de joindre la plateforme CSE. Vérifiez que le gateway est démarré sur le port 8080.'
    return STATUS_MESSAGES[error.response.status] ?? `Erreur inattendue (${error.response.status}).`
  }
  if (error instanceof Error && error.message) return error.message
  return 'Une erreur inattendue est survenue.'
}

export function getApiErrorCode(error: unknown): string | null {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ProblemDetail | undefined
    return data?.code ?? null
  }
  return null
}

export function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

export function addDaysIso(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}
