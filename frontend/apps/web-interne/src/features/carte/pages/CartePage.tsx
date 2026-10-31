import { List } from 'lucide-react'
import { lazy, Suspense, useState } from 'react'
import { Link } from 'react-router-dom'
import { paths } from '@/app/routes'
import { Button } from '@/shared/ui/button'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { ProgressBar, Skeleton } from '@/shared/ui/skeleton'
import { CartePanel, type CarteFiltresState } from '../components/CartePanel'
import { useCarte } from '../hooks/useCarte'

const CarteMap = lazy(() => import('../components/CarteMap').then((module) => ({ default: module.CarteMap })))

/** Carte du parc : chantiers et engins geolocalises, filtres par categorie et etat. */
export function Component() {
  const [filtres, setFiltres] = useState<CarteFiltresState>({ categorie: null, etat: null })
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const query = useCarte({ categorie: filtres.categorie ?? undefined, etat: filtres.etat ?? undefined })

  return (
    <>
      <PageHeader
        title="Carte du parc"
        actions={
          <Button variant="secondary" leftIcon={<List />} asChild>
            <Link to={paths.materiels}>Vue liste</Link>
          </Button>
        }
      />
      <div className="relative isolate flex h-[calc(100dvh-var(--topbar-height)-13rem)] min-h-[28rem] overflow-hidden rounded-xl border border-border bg-surface md:h-[calc(100dvh-var(--topbar-height)-11rem)]">
        <ProgressBar
          active={query.isFetching && !query.isPending}
          className="absolute inset-x-0 top-0 z-[1001]"
        />
        <div className="relative min-w-0 flex-1">
          {query.isError && !query.data ? (
            <ErrorState error={query.error} onRetry={() => void query.refetch()} />
          ) : (
            <Suspense fallback={<Skeleton className="size-full rounded-none" />}>
              <CarteMap
                groupes={query.groupes}
                selectedKey={selectedKey}
                onSelect={setSelectedKey}
                onPopupClose={(key) => {
                  setSelectedKey((current) => (current === key ? null : current))
                }}
              />
            </Suspense>
          )}
        </div>
        <CartePanel
          groupes={query.groupes}
          loading={query.isPending}
          filtres={filtres}
          onFiltresChange={(next) => {
            setSelectedKey(null)
            setFiltres(next)
          }}
          selectedKey={selectedKey}
          onSelect={setSelectedKey}
        />
      </div>
    </>
  )
}
