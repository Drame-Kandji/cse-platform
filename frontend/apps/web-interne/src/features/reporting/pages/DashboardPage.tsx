import { useCurrentUser } from '@/shared/auth/context'
import { PageHeader } from '@/shared/ui/page-header'
import { AdminHome } from '../components/AdminHome'
import { PeriodSelector } from '../components/PeriodSelector'
import { ProfilDashboard } from '../components/ProfilDashboard'
import { ProfilSwitcher } from '../components/ProfilSwitcher'
import { useReportingFilters } from '../hooks/useReportingFilters'
import { greeting } from '../lib/greeting'
import { queuesFor } from '../lib/queues'

/** Tableau de bord par role : salutation + periode, 4 KPI, un graphique, la liste "A traiter". */
export function Component() {
  const user = useCurrentUser()
  const filters = useReportingFilters()
  const firstName = user.name.split(' ')[0] ?? user.name
  const queues = queuesFor(user.roles)
  const { profil, period } = filters

  return (
    <>
      <PageHeader
        title={`${greeting()} ${firstName}`}
        actions={
          profil && (
            <>
              <ProfilSwitcher profils={filters.allowed} value={profil} onChange={filters.setProfil} />
              <PeriodSelector value={period} onChange={filters.setPeriod} />
            </>
          )
        }
      />
      {profil ? <ProfilDashboard profil={profil} period={period} queues={queues} /> : <AdminHome />}
    </>
  )
}
