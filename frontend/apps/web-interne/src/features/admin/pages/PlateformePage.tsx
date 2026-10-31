import { CircleCheck, RefreshCw, Server, ServerCrash, TriangleAlert } from 'lucide-react'
import { useState } from 'react'
import { cn } from '@/shared/lib/cn'
import { formatDateTime } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Skeleton } from '@/shared/ui/skeleton'
import { Switch } from '@/shared/ui/switch'
import { useEtatPlateforme } from '../hooks/useAudit'
import { ServiceCard } from '../components/ServiceCard'
import { latenceTone } from '../lib/plateforme'

const REFRESH_MS = 15_000

/** Etat de la plateforme : sante et latence de chaque service vus par la passerelle, rafraichi automatiquement. */
export function Component() {
  const [auto, setAuto] = useState(true)
  const query = useEtatPlateforme(auto ? REFRESH_MS : false)
  const services = [...(query.data?.services ?? [])].sort((a, b) =>
    a.statut === b.statut ? a.nom.localeCompare(b.nom) : a.statut === 'DOWN' ? -1 : 1,
  )
  const down = services.filter((service) => service.statut === 'DOWN').length
  const slow = services.filter((service) => latenceTone(service) === 'warning').length

  return (
    <>
      <PageHeader
        title="État de la plateforme"
        actions={
          <>
            <Switch
              checked={auto}
              onCheckedChange={setAuto}
              label="Actualisation auto (15 s)"
              className="order-last"
            />
            <Button
              variant="secondary"
              leftIcon={<RefreshCw className={cn(query.isFetching && 'animate-spin')} />}
              onClick={() => void query.refetch()}
              disabled={query.isFetching}
            >
              Actualiser
            </Button>
          </>
        }
      />
      {query.isPending ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }, (_, index) => (
            <Skeleton key={index} className="h-32 rounded-xl" />
          ))}
        </div>
      ) : query.isError && !query.data ? (
        <Card>
          <ErrorState
            error={query.error}
            onRetry={() => void query.refetch()}
            title="État de la plateforme indisponible"
          />
        </Card>
      ) : services.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Server aria-hidden />}
            title="Aucun service déclaré"
            description="La passerelle ne remonte aucun service."
          />
        </Card>
      ) : (
        <div className="grid gap-6">
          <div
            role="status"
            className={cn(
              'flex flex-wrap items-center gap-3 rounded-xl border px-4 py-3 text-sm',
              down > 0
                ? 'border-danger/30 bg-danger/10 text-danger-emphasis'
                : slow > 0
                  ? 'border-warning/30 bg-warning/10 text-warning-emphasis'
                  : 'border-success/30 bg-success/10 text-success-emphasis',
            )}
          >
            {down > 0 ? (
              <ServerCrash className="size-5" aria-hidden />
            ) : slow > 0 ? (
              <TriangleAlert className="size-5" aria-hidden />
            ) : (
              <CircleCheck className="size-5" aria-hidden />
            )}
            <span className="font-semibold">
              {down > 0
                ? `${down} service(s) indisponible(s) sur ${services.length}`
                : slow > 0
                  ? `Tous les services répondent, ${slow} avec une latence élevée`
                  : `Les ${services.length} services sont opérationnels`}
            </span>
            <span className="ml-auto text-xs opacity-80">
              Dernière mesure : {formatDateTime(query.dataUpdatedAt)}
            </span>
          </div>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4" aria-label="Services">
            {services.map((service) => (
              <li key={service.nom}>
                <ServiceCard service={service} />
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  )
}
