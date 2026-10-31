import { Truck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { EmptyState } from '@/shared/ui/empty-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { useMaterielsAffectes } from '../hooks/useBesoins'

/** Liens vers les materiels affectes au besoin (libelles charges depuis ms-suivi). */
export function MaterielsAffectes({ ids }: { ids: readonly string[] }) {
  const { t } = useTranslation('besoins')
  const queries = useMaterielsAffectes(ids)
  if (ids.length === 0) {
    return <EmptyState size="compact" icon={<Truck aria-hidden />} title={t('detail.materielsVide')} />
  }
  return (
    <ul className="grid gap-2">
      {ids.map((id, index) => {
        const query = queries[index]
        const materiel = query?.data
        return (
          <li key={id}>
            <Link
              to={to.materielDetail(id)}
              className="flex items-center gap-3 rounded-lg border border-border p-3 transition-colors hover:border-border-strong hover:bg-muted/40"
            >
              <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground">
                <Truck className="size-4" aria-hidden />
              </span>
              {query?.isPending ? (
                <Skeleton className="h-4 w-40" />
              ) : (
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium">
                    {materiel?.designation ?? id.slice(0, 8)}
                  </span>
                  {materiel && (
                    <span className="block font-mono text-xs text-muted-foreground">{materiel.code}</span>
                  )}
                </span>
              )}
              {materiel && <StatusBadge kind="etatMateriel" value={materiel.etat} />}
            </Link>
          </li>
        )
      })}
    </ul>
  )
}
