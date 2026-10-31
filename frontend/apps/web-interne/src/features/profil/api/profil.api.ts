import { createQueryKeys } from '@/shared/api/query-keys'
import { http } from '@/shared/api/http'
import type { PageResponse } from '@/shared/api/types'
import type { ChantierResume, MonProfil } from '../types'

export const profilKeys = createQueryKeys('profil')

export const profilApi = {
  moi: (signal?: AbortSignal) => http.get<MonProfil>('/me', { signal }),
  chantiers: (signal?: AbortSignal) =>
    http.get<PageResponse<ChantierResume>>('/chantiers', { query: { size: 50, sort: 'nom,asc' }, signal }),
}
