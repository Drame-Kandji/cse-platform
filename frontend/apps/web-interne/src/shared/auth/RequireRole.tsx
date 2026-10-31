import type { ReactNode } from 'react'
import { Outlet } from 'react-router-dom'
import type { Role } from '@/shared/domain/labels'
import { useAuth } from './context'
import { ForbiddenView } from './ForbiddenView'

export interface RequireRoleProps {
  /** Roles autorises (au moins un). Liste vide = tout utilisateur connecte. */
  roles: readonly Role[]
  /** Contenu protege ; a defaut, rend l'<Outlet /> (route parente). */
  children?: ReactNode
  /** Rendu si refuse (403 par defaut). */
  fallback?: ReactNode
}

/**
 * Garde de route par role. L'URL est conservee et une page 403 est affichee.
 * La securite reelle reste cote API (@PreAuthorize) : ceci n'est que de l'ergonomie.
 * @example { element: <RequireRole roles={access.finance} />, children: [...] }
 */
export function RequireRole({ roles, children, fallback }: RequireRoleProps) {
  const { status, hasAnyRole } = useAuth()
  if (status !== 'authenticated') return null
  if (!hasAnyRole(roles)) return <>{fallback ?? <ForbiddenView roles={roles} />}</>
  return <>{children ?? <Outlet />}</>
}
