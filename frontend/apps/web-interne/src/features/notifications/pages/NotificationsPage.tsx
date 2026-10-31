import { BellOff, CheckCheck } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { PAGE_SIZE_OPTIONS } from '@/shared/api/types'
import { DataTablePagination } from '@/shared/ui/data-table'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { ProgressBar, Skeleton } from '@/shared/ui/skeleton'
import { Tabs, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { NotificationItem } from '../components/NotificationItem'
import { useNotifications, useToutLire, useUnreadCount } from '../hooks/useNotifications'
import { useOpenNotification } from '../hooks/useOpenNotification'

const FILTRES = [
  { value: 'toutes', label: 'Toutes' },
  { value: 'non-lues', label: 'Non lues' },
  { value: 'lues', label: 'Lues' },
] as const
type Filtre = (typeof FILTRES)[number]['value']

const DEFAULT_SIZE = 20

function parseFiltre(value: string | null): Filtre {
  return FILTRES.find((filtre) => filtre.value === value)?.value ?? 'toutes'
}

/** Centre de notifications : filtres lue / non lue, pagination, ouverture du lien. */
export function Component() {
  const [params, setParams] = useSearchParams()
  const filtre = parseFiltre(params.get('filtre'))
  const page = Math.max(0, (Number(params.get('page')) || 1) - 1)
  const size = (PAGE_SIZE_OPTIONS as readonly number[]).includes(Number(params.get('size')))
    ? Number(params.get('size'))
    : DEFAULT_SIZE
  const lue = filtre === 'toutes' ? undefined : filtre === 'lues'
  const query = useNotifications({ lue, page, size })
  const unread = useUnreadCount().data?.count ?? 0
  const toutLire = useToutLire()
  const open = useOpenNotification()
  const data = query.data

  const update = (changes: Record<string, string | null>) => {
    setParams(
      (current) => {
        const next = new URLSearchParams(current)
        for (const [key, value] of Object.entries(changes)) {
          if (value === null) next.delete(key)
          else next.set(key, value)
        }
        return next
      },
      { replace: true },
    )
  }

  return (
    <>
      <PageHeader
        title="Notifications"
        actions={
          <Button
            variant="secondary"
            leftIcon={<CheckCheck />}
            disabled={unread === 0}
            loading={toutLire.isPending}
            onClick={() => {
              toutLire.mutate()
            }}
          >
            Tout marquer comme lu
          </Button>
        }
      />
      <Tabs
        value={filtre}
        onValueChange={(value) => {
          update({ filtre: value === 'toutes' ? null : value, page: null })
        }}
      >
        <TabsList aria-label="Filtrer les notifications">
          {FILTRES.map((item) => (
            <TabsTrigger key={item.value} value={item.value}>
              {item.label}
              {item.value === 'non-lues' && unread > 0 && (
                <span className="rounded-full bg-primary px-1.5 text-2xs font-semibold leading-4 text-primary-foreground">
                  {unread}
                </span>
              )}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>
      <Card className="mt-5 overflow-hidden">
        <ProgressBar active={query.isFetching && !query.isPending} />
        {query.isPending ? (
          <ul className="grid gap-5 p-5" aria-label="Chargement">
            {[0, 1, 2, 3, 4].map((index) => (
              <li key={index} className="flex gap-3">
                <Skeleton className="size-8 rounded-lg" />
                <div className="grid flex-1 gap-1.5">
                  <Skeleton className="h-4 w-1/2" />
                  <Skeleton className="h-3 w-3/4" />
                </div>
              </li>
            ))}
          </ul>
        ) : query.isError ? (
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={<BellOff aria-hidden />}
            title={filtre === 'non-lues' ? 'Tout est lu' : 'Aucune notification'}
            description={
              filtre === 'non-lues'
                ? 'Vous avez traité toutes vos notifications.'
                : 'Les événements qui vous concernent apparaîtront ici en temps réel.'
            }
          />
        ) : (
          <ul className="divide-y divide-border">
            {data.content.map((notification) => (
              <li key={notification.id}>
                <NotificationItem notification={notification} onOpen={open} />
              </li>
            ))}
          </ul>
        )}
        {data && data.totalElements > Math.min(...PAGE_SIZE_OPTIONS) && (
          <DataTablePagination
            pageIndex={page}
            pageSize={size}
            totalElements={data.totalElements}
            pageCount={data.totalPages}
            onPageChange={(next) => {
              update({ page: next > 0 ? String(next + 1) : null })
            }}
            onPageSizeChange={(next) => {
              update({ size: next === DEFAULT_SIZE ? null : String(next), page: null })
            }}
          />
        )}
      </Card>
    </>
  )
}
