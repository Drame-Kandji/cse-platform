import { useQuery } from '@tanstack/react-query'
import { useCurrentUser } from '@/shared/auth/context'
import { isRole } from '@/shared/auth/claims'
import { ROLE_LABELS, describeStatus } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDate, formatMoney } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/avatar'
import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Skeleton } from '@/shared/ui/skeleton'
import { profilApi, profilKeys } from '../api/profil.api'
import { InfoItem } from '../components/InfoItem'

/** Page "Mon profil" : identite, role, attributs metier et chantiers rattaches (`GET /api/me`). */
export function Component() {
  const session = useCurrentUser()
  const query = useQuery({
    queryKey: profilKeys.custom('me'),
    queryFn: ({ signal }) => profilApi.moi(signal),
  })
  const chantierIds = query.data?.chantierIds ?? []
  const chantiers = useQuery({
    queryKey: profilKeys.custom('chantiers'),
    queryFn: ({ signal }) => profilApi.chantiers(signal),
    enabled: chantierIds.length > 0,
  })

  if (query.isPending) {
    return (
      <>
        <PageHeader title="Mon profil" />
        <div className="grid max-w-4xl gap-6">
          <Skeleton className="h-14 w-72 rounded-xl" />
          <Skeleton className="h-48 rounded-xl" />
        </div>
      </>
    )
  }
  if (query.isError) {
    return (
      <>
        <PageHeader title="Mon profil" />
        <Card>
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        </Card>
      </>
    )
  }

  const profil = query.data
  const fullName = `${profil.prenom} ${profil.nom}`.trim() || session.name
  const role = isRole(profil.role) ? ROLE_LABELS[profil.role] : describeStatus('role', profil.role)
  const chantiersRattaches = chantierIds.map(
    (id) =>
      chantiers.data?.content.find((chantier) => chantier.id === id) ?? { id, code: '', nom: id, statut: '' },
  )

  return (
    <>
      <PageHeader title="Mon profil" />
      <div className="grid max-w-4xl gap-6">
        <div className="flex items-center gap-4">
          <Avatar name={fullName} size="lg" className="size-14 text-base" />
          <div className="grid min-w-0 gap-1">
            <p className="truncate text-lg font-semibold tracking-tight">{fullName}</p>
            <p className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
              {role.label}
              {!profil.actif && <Badge size="sm">Compte désactivé</Badge>}
            </p>
          </div>
        </div>

        <Card>
          <CardContent className="pt-5">
            <dl className="grid gap-5 sm:grid-cols-2">
              <InfoItem label="Adresse e-mail">{profil.email}</InfoItem>
              <InfoItem label="Téléphone">{profil.telephone ?? EMPTY_VALUE}</InfoItem>
              <InfoItem label="Double authentification (OTP)">
                {profil.otpConfigure ? 'Configurée' : 'Non configurée'}
              </InfoItem>
              <InfoItem label="Membre depuis">{formatDate(profil.createdAt)}</InfoItem>
              {profil.perimetreParc && <InfoItem label="Périmètre du parc">{profil.perimetreParc}</InfoItem>}
              {profil.seuilDelegation != null && (
                <InfoItem label="Seuil de délégation">{formatMoney(profil.seuilDelegation)}</InfoItem>
              )}
              {profil.budgetGere != null && (
                <InfoItem label="Budget géré">{formatMoney(profil.budgetGere)}</InfoItem>
              )}
            </dl>
          </CardContent>
        </Card>

        {(profil.role === 'CHEF_CHANTIER' || chantierIds.length > 0) && (
          <Card>
            <CardHeader title="Chantiers rattachés" />
            <CardContent>
              {chantierIds.length === 0 ? (
                <p className="text-sm text-muted-foreground">Aucun chantier rattaché</p>
              ) : chantiers.isPending ? (
                <Skeleton className="h-16 w-full" />
              ) : (
                <ul className="divide-y divide-border">
                  {chantiersRattaches.map((chantier) => (
                    <li key={chantier.id} className="flex items-baseline justify-between gap-3 py-2.5">
                      <span className="text-sm font-medium">{chantier.nom}</span>
                      {chantier.code && (
                        <span className="font-mono text-xs text-muted-foreground">{chantier.code}</span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </>
  )
}
