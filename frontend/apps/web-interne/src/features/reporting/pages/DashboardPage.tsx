import { BarChart3 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { paths } from '@/app/routes'
import { Can } from '@/shared/auth/Can'
import { useCurrentUser } from '@/shared/auth/context'
import { ROLE_LABELS } from '@/shared/domain/labels'
import { formatDate } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { PageHeader } from '@/shared/ui/page-header'
import { AdminHome } from '../components/AdminHome'
import { PeriodSelector } from '../components/PeriodSelector'
import { ProfilDashboard } from '../components/ProfilDashboard'
import { ProfilSwitcher } from '../components/ProfilSwitcher'
import { useReportingFilters } from '../hooks/useReportingFilters'
import { greeting } from '../lib/greeting'
import { PROFIL_LABELS } from '../lib/profils'
import { queuesFor } from '../lib/queues'

/** Tableau de bord par role : vue strategique (DG), operationnelle (RM), chantier (CHEF), financiere (DAF), achats (RA). */
export function Component() {
  const user = useCurrentUser()
  const filters = useReportingFilters()
  const firstName = user.name.split(' ')[0] ?? user.name
  const role = user.roles[0]
  const queues = queuesFor(user.roles)
  const { profil, period } = filters

  return (
    <>
      <PageHeader
        title={`${greeting()} ${firstName}`}
        meta={role && <Badge tone={ROLE_LABELS[role].tone}>{ROLE_LABELS[role].label}</Badge>}
        description={
          profil
            ? `${PROFIL_LABELS[profil].vue} du ${formatDate(period.du)} au ${formatDate(period.au)}.`
            : `Nous sommes le ${formatDate(new Date(), 'EEEE d MMMM yyyy')}. Accédez rapidement aux outils d'administration.`
        }
        actions={
          profil && (
            <>
              <ProfilSwitcher profils={filters.allowed} value={profil} onChange={filters.setProfil} />
              <PeriodSelector value={period} onChange={filters.setPeriod} />
              <Can roles={access.rapports}>
                <Button asChild variant="secondary">
                  <Link to={paths.rapports}>
                    <BarChart3 aria-hidden />
                    Rapports
                  </Link>
                </Button>
              </Can>
            </>
          )
        }
      />
      {profil ? <ProfilDashboard profil={profil} period={period} queues={queues} /> : <AdminHome />}
    </>
  )
}
