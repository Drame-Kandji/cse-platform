import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { useMaterielsAffectes } from '../hooks/useBesoins'

/** Liens vers les materiels affectes au besoin (libelles charges depuis ms-suivi). */
export function MaterielsAffectes({ ids }: { ids: readonly string[] }) {
  const { t } = useTranslation('besoins')
  const queries = useMaterielsAffectes(ids)
  if (ids.length === 0) {
    return <p className="text-sm text-muted-foreground">{t('detail.materielsVide')}</p>
  }
  return (
    <ul className="grid divide-y divide-border">
      {ids.map((id, index) => {
        const query = queries[index]
        const materiel = query?.data
        return (
          <li key={id}>
            <Link
              to={to.materielDetail(id)}
              className="-mx-2 flex items-center gap-3 rounded-lg px-2 py-2.5 transition-colors hover:bg-muted/60"
            >
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
