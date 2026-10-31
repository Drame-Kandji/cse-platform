import { http } from '@/shared/api/http'
import type { PageParams, PageResponse } from '@/shared/api/types'
import type {
  AuditFiltres,
  AuditLog,
  AuditStatistiques,
  EtatPlateforme,
  RoleInfo,
  TypeEngin,
  TypeEnginSaisie,
  Utilisateur,
  UtilisateurCreation,
  UtilisateurFiltres,
  UtilisateurModification,
} from '../types'

const userPath = (id: string) => `/admin/utilisateurs/${encodeURIComponent(id)}`

/** Comptes et roles (ms-auth, ADMIN). */
export const utilisateursApi = {
  lister: (params: UtilisateurFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Utilisateur>>('/admin/utilisateurs', { query: { ...params }, signal }),
  creer: (body: UtilisateurCreation) => http.post<Utilisateur>('/admin/utilisateurs', body),
  modifier: ({ id, ...body }: UtilisateurModification & { id: string }) =>
    http.put<Utilisateur>(userPath(id), body),
  changerStatut: ({ id, actif }: { id: string; actif: boolean }) =>
    http.patch<Utilisateur>(`${userPath(id)}/statut`, { actif }),
  reinitialiserMotDePasse: ({ id, motDePasseTemporaire }: { id: string; motDePasseTemporaire: string }) =>
    http.post<undefined>(
      `${userPath(id)}/reinitialiser-mot-de-passe`,
      { motDePasseTemporaire },
      { responseType: 'void' },
    ),
  roles: (signal?: AbortSignal) => http.get<RoleInfo[]>('/admin/roles', { signal }),
}

/** Referentiel des types d'engins (ms-suivi ; ecriture ADMIN). */
export const typesEnginsApi = {
  lister: (signal?: AbortSignal) => http.get<TypeEngin[]>('/referentiels/types-engins', { signal }),
  creer: (body: TypeEnginSaisie) => http.post<TypeEngin>('/referentiels/types-engins', body),
  modifier: ({ id, ...body }: TypeEnginSaisie & { id: string }) =>
    http.put<TypeEngin>(`/referentiels/types-engins/${encodeURIComponent(id)}`, body),
}

/** Journal d'audit (ms-audit, ADMIN). */
export const auditApi = {
  lister: (params: AuditFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<AuditLog>>('/audit', { query: { ...params }, signal }),
  statistiques: (signal?: AbortSignal) => http.get<AuditStatistiques>('/audit/statistiques', { signal }),
}

/** Sante des services vue par la passerelle. */
export const plateformeApi = {
  etat: (signal?: AbortSignal) => http.get<EtatPlateforme>('/plateforme/etat', { signal }),
}

/** Chantier (selection multiple des chefs de chantier). */
export interface ChantierOption {
  readonly id: string
  readonly code: string
  readonly nom: string
  readonly statut: string
}

export const chantiersApi = {
  lister: (signal?: AbortSignal) =>
    http.get<PageResponse<ChantierOption>>('/chantiers', { query: { size: 50, sort: 'nom,asc' }, signal }),
}
