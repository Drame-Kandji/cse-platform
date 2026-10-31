import { ChevronRight } from 'lucide-react'
import { Fragment } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useMatches } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { isRouteHandle } from '../route-handle'

/** Fil d'Ariane derive des `handle.crumb` des routes actives (dernier element seul sur mobile). */
export function Breadcrumbs() {
  const { t } = useTranslation('navigation')
  const { t: tc } = useTranslation()
  const crumbs = useMatches().flatMap((match) =>
    isRouteHandle(match.handle) && match.handle.crumb
      ? [{ id: match.id, path: match.pathname, label: t(match.handle.crumb) }]
      : [],
  )
  if (crumbs.length === 0) return null
  return (
    <nav aria-label={tc('shell.breadcrumb')} className="min-w-0">
      <ol className="flex min-w-0 items-center gap-1.5 text-sm">
        {crumbs.map((crumb, index) => {
          const last = index === crumbs.length - 1
          return (
            <Fragment key={crumb.id}>
              {index > 0 && (
                <ChevronRight
                  className="hidden size-3.5 shrink-0 text-muted-foreground/70 md:block"
                  aria-hidden
                />
              )}
              <li className={cn('min-w-0', !last && 'hidden md:block')}>
                {last ? (
                  <span aria-current="page" className="block truncate font-medium text-foreground">
                    {crumb.label}
                  </span>
                ) : (
                  <Link
                    to={crumb.path}
                    className="block truncate rounded text-muted-foreground transition-colors hover:text-foreground"
                  >
                    {crumb.label}
                  </Link>
                )}
              </li>
            </Fragment>
          )
        })}
      </ol>
    </nav>
  )
}
