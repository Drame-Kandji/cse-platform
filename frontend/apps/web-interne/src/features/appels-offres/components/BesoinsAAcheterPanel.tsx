import { ExternalLink, Gavel, PackageSearch } from 'lucide-react'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { Can } from '@/shared/auth'
import { labelOf } from '@/shared/domain/labels'
import { formatNumber, formatRelative } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { BesoinAAcheter } from '../api/types'
import { useBesoinsAAcheter } from '../hooks/useAppelsOffres'

/** Besoins valides orientes vers l'achat, avec lancement d'un AO pre-rempli. */
export function BesoinsAAcheterPanel({ onLancer }: { onLancer: (besoin: BesoinAAcheter) => void }) {
  const query = useBesoinsAAcheter({ page: 0, size: 10 })
  const besoins = query.data?.content ?? []
  const enAttente = besoins.filter((b) => !b.appelOffreId).length
  return (
    <Card>
      <CardHeader
        icon={<PackageSearch aria-hidden />}
        title="Besoins à acheter"
        description={
          query.data
            ? `${formatNumber(query.data.totalElements)} besoin(s) validé(s) orienté(s) vers l'achat, dont ${enAttente} sans appel d'offres`
            : "Besoins validés par le Responsable Matériel et orientés vers l'achat"
        }
      />
      <CardContent className="pt-4">
        {query.isPending ? (
          <div className="grid gap-2">
            {[0, 1, 2].map((index) => (
              <Skeleton key={index} className="h-14 w-full" />
            ))}
          </div>
        ) : query.isError ? (
          <ErrorState error={query.error} onRetry={() => void query.refetch()} size="compact" />
        ) : besoins.length === 0 ? (
          <EmptyState
            size="compact"
            title="Aucun besoin en attente d'achat"
            description="Les besoins validés sans matériel disponible apparaîtront ici."
          />
        ) : (
          <ul className="divide-y divide-border">
            {besoins.map((besoin) => (
              <li
                key={besoin.besoinId}
                className="flex flex-col gap-2 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-center"
              >
                <div className="min-w-0 flex-1">
                  <p className="flex flex-wrap items-center gap-2 text-sm font-medium">
                    <span className="font-mono text-xs text-muted-foreground">{besoin.reference}</span>
                    <span className="truncate">{besoin.designation}</span>
                    <StatusBadge kind="priorite" value={besoin.priorite} size="sm" />
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatNumber(besoin.quantite)} × {labelOf('categorieMateriel', besoin.categorie)} ·{' '}
                    {besoin.chantierNom} · reçu {formatRelative(besoin.dateReception)}
                  </p>
                </div>
                {besoin.appelOffreId ? (
                  <Button asChild variant="ghost" size="sm">
                    <Link to={to.appelOffreDetail(besoin.appelOffreId)}>
                      <ExternalLink aria-hidden />
                      Voir l&apos;appel d&apos;offres
                    </Link>
                  </Button>
                ) : (
                  <Can roles={access.achatsGestion}>
                    <Button
                      variant="secondary"
                      size="sm"
                      leftIcon={<Gavel />}
                      onClick={() => {
                        onLancer(besoin)
                      }}
                      aria-label={`Lancer un appel d'offres pour ${besoin.reference}`}
                    >
                      Lancer un AO
                    </Button>
                  </Can>
                )}
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
