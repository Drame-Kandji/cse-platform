import axios, { AxiosError, type AxiosInstance } from 'axios'

export type PageResponse<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number; last: boolean }
export type ProblemDetail = { detail?: string; title?: string; code?: string }
export const unwrapList = <T>(data: T[] | PageResponse<T>): T[] => Array.isArray(data) ? data : (data?.content ?? [])
export const toPage = <T>(data: T[] | PageResponse<T>) => Array.isArray(data) ? { content: data, page: 0, size: data.length, totalElements: data.length, totalPages: data.length ? 1 : 0, last: true } : data
export const stripEmoji = (value: string) => value.replace(/\p{Extended_Pictographic}/gu, '').replace(/\s{2,}/g, ' ').trim()
export const problemMessage = (error: unknown) => {
  const response = (error as AxiosError<ProblemDetail>).response
  return stripEmoji(response?.data?.detail || response?.data?.title || 'Une erreur est survenue. Réessayez.')
}
export const createHttp = (token: () => string | undefined, onUnauthorized: () => void): AxiosInstance => {
  const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api' })
  http.interceptors.request.use((config) => { const value = token(); if (value) config.headers.Authorization = `Bearer ${value}`; return config })
  http.interceptors.response.use((response) => response, (error: AxiosError) => { if (error.response?.status === 401) onUnauthorized(); return Promise.reject(error) })
  return http
}
