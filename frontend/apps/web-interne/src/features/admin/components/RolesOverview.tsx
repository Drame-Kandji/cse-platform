import { describeStatus } from '@/shared/domain/labels'
import { CountTabs } from '@/shared/ui/count-tabs'
import { useRoles } from '../hooks/useUtilisateurs'

/** Libelles courts des compteurs (le libelle complet reste dans le tableau). */
const COURT: Readonly<Record<string, string>> = {
  ADMIN_SYSTEME: 'Admin',
  DIRECTION_GENERALE: 'DG',
  DAF: 'DAF',
  RESPONSABLE_ACHATS: 'Achats',
  RESPONSABLE_MATERIEL: 'Matériel',
  CHEF_CHANTIER: 'Chefs de chantier',
  FOURNISSEUR: 'Fournisseurs',
}

export interface RolesOverviewProps {
  /** Role actuellement filtre. */
  selected?: string
  onSelect: (role: string | undefined) => void
}

/** Compteurs par role au-dessus de la liste des comptes ; un clic filtre le tableau. */
export function RolesOverview({ selected, onSelect }: RolesOverviewProps) {
  const query = useRoles()
  const roles = query.data ?? []
  const total = roles.reduce((sum, role) => sum + role.nombreUtilisateurs, 0)
  return (
    <CountTabs
      label="Rôles"
      loading={query.isPending}
      value={selected ?? null}
      onChange={(value) => {
        onSelect(value ?? undefined)
      }}
      items={[
        { value: null, label: 'Tous', count: query.data ? total : undefined },
        ...roles.map((role) => ({
          value: role.code,
          label: COURT[role.code] ?? describeStatus('role', role.code).label,
          count: role.nombreUtilisateurs,
        })),
      ]}
      className="mb-0"
    />
  )
}
