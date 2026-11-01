import { ChevronRight, CircleCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { formatDate } from '@/shared/lib/format'
import { Card, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { Skeleton } from '@/shared/ui/skeleton'
import type { AFaire } from '../lib/a-faire'

/** Liste « À faire » : une ligne par action, lien vers l'ecran concerne. */
export function AFaireList({ items, loading }: { items: readonly AFaire[]; loading: boolean }) {
  const { t } = useTranslation('portail')
  return (
    <Card>
      <CardHeader
        title={t('accueil.todo')}
        action={
          !loading && items.length > 0 ? (
            <span className="text-sm tabular-nums text-muted-foreground">{items.length}</span>
          ) : undefined
        }
      />
      <div className="px-2 pb-2 pt-3">
        {loading ? (
          <div className="grid gap-2 px-3 pb-3">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : items.length === 0 ? (
          <EmptyState size="compact" icon={<CircleCheck aria-hidden />} title={t('accueil.nothing')} />
        ) : (
          <ul className="divide-y divide-border">
            {items.map((item) => (
              <li key={item.id}>
                <Link
                  to={item.lien}
                  className="flex items-center gap-3 rounded-lg px-3 py-3 transition-colors hover:bg-muted"
                >
                  <span className="grid min-w-0 flex-1 gap-0.5">
                    <span
                      className={cn(
                        'text-sm font-medium',
                        item.type === 'FACTURE_REJETEE' ? 'text-danger-emphasis' : 'text-foreground',
                      )}
                    >
                      {item.titre}
                    </span>
                    <span className="truncate text-xs text-muted-foreground">{item.detail}</span>
                  </span>
                  {item.echeance && (
                    <span className="hidden shrink-0 text-xs tabular-nums text-muted-foreground sm:inline">
                      {formatDate(item.echeance)}
                    </span>
                  )}
                  <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Card>
  )
}
