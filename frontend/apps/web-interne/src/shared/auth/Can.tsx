import type { ReactNode } from 'react'
import type { Role } from '@/shared/domain/labels'
import { useAuth } from './context'

/**
 * Affiche son contenu seulement si l'utilisateur a l'un des roles.
 * @example <Can roles={['RESPONSABLE_MATERIEL']}><Button>Valider</Button></Can>
 */
export function Can({
  roles,
  children,
  fallback = null,
}: {
  roles: readonly Role[]
  children: ReactNode
  fallback?: ReactNode
}) {
  const { hasAnyRole } = useAuth()
  return <>{hasAnyRole(roles) ? children : fallback}</>
}
