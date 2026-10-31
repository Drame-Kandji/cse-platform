import { send } from './http'
import type { QueryParams } from './types'

export interface DownloadedFile {
  readonly blob: Blob
  readonly filename: string
}

/** Extrait le nom de fichier d'un en-tete Content-Disposition (RFC 6266, filename* prioritaire). */
export function filenameFromDisposition(header: string | null, fallback: string): string {
  if (!header) return fallback
  const extended = /filename\*\s*=\s*(?:UTF-8'')?([^;]+)/i.exec(header)
  if (extended?.[1]) return decodeURIComponent(extended[1].trim().replace(/^"|"$/g, ''))
  const simple = /filename\s*=\s*"?([^";]+)"?/i.exec(header)
  return simple?.[1]?.trim() ?? fallback
}

/**
 * Telecharge un fichier binaire authentifie (export PDF/XLSX).
 * @param fallbackName nom utilise si le serveur n'expose pas Content-Disposition
 * @example const file = await downloadFile('/rapports/parc.xlsx', 'parc.xlsx'); saveBlob(file.blob, file.filename)
 */
export async function downloadFile(
  path: string,
  fallbackName: string,
  options: { query?: QueryParams; method?: 'GET' | 'POST'; body?: unknown; signal?: AbortSignal } = {},
): Promise<DownloadedFile> {
  const response = await send(path, { ...options, responseType: 'blob' })
  const filename = filenameFromDisposition(response.headers.get('content-disposition'), fallbackName)
  return { blob: await response.blob(), filename }
}

/**
 * Declenche l'enregistrement d'un Blob cote navigateur.
 * @example saveBlob(file.blob, file.filename)
 */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => {
    URL.revokeObjectURL(url)
  }, 1000)
}
