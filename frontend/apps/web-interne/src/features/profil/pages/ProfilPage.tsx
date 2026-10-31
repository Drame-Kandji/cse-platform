import { useQuery } from '@tanstack/react-query'
import { Building2, KeyRound, Mail, Phone, ShieldCheck, ShieldAlert } from 'lucide-react'
import { useCurrentUser } from '@/shared/auth/context'
import { isRole } from '@/shared/auth/claims'
import { ROLE_LABELS, describeStatus } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDate, formatMoney } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/avatar'
import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
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
        <div className="grid gap-6 lg:grid-cols-3">
          <Skeleton className="h-64 rounded-xl" />
          <Skeleton className="h-64 rounded-xl lg:col-span-2" />
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
      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="flex flex-col items-center gap-4 p-6 text-center">
          <Avatar name={fullName} size="lg" className="size-16 text-lg" />
          <div className="grid gap-1">
            <p className="text-lg font-semibold tracking-tight">{fullName}</p>
            <p className="text-sm text-muted-foreground">{profil.email}</p>
          </div>
          <div className="flex flex-wrap justify-center gap-1.5">
            <Badge tone={role.tone}>{role.label}</Badge>
            <Badge tone={profil.actif ? 'success' : 'neutral'} dot>
              {profil.actif ? 'Compte actif' : 'Compte désactivé'}
            </Badge>
          </div>
          {role.description && <p className="text-sm text-muted-foreground">{role.description}</p>}
        </Card>

        <div className="grid gap-6 lg:col-span-2">
          <Card>
            <CardHeader title="Coordonnées et sécurité" />
            <CardContent>
              <dl className="grid gap-5 sm:grid-cols-2">
                <InfoItem label="Adresse e-mail" icon={<Mail />}>
                  {profil.email}
                </InfoItem>
                <InfoItem label="Téléphone" icon={<Phone />}>
                  {profil.telephone ?? EMPTY_VALUE}
                </InfoItem>
                <InfoItem
                  label="Double authentification (OTP)"
                  icon={profil.otpConfigure ? <ShieldCheck /> : <ShieldAlert />}
                >
                  {profil.otpConfigure ? 'Configurée' : 'Non configurée'}
                </InfoItem>
                <InfoItem label="Membre depuis" icon={<KeyRound />}>
                  {formatDate(profil.createdAt)}
                </InfoItem>
              </dl>
            </CardContent>
          </Card>

          {(profil.perimetreParc ?? profil.seuilDelegation ?? profil.budgetGere) != null && (
            <Card>
              <CardHeader title="Attributs du rôle" />
              <CardContent>
                <dl className="grid gap-5 sm:grid-cols-3">
                  {profil.perimetreParc && (
                    <InfoItem label="Périmètre du parc">{profil.perimetreParc}</InfoItem>
                  )}
                  {profil.seuilDelegation != null && (
                    <InfoItem label="Seuil de délégation">{formatMoney(profil.seuilDelegation)}</InfoItem>
                  )}
                  {profil.budgetGere != null && (
                    <InfoItem label="Budget géré">{formatMoney(profil.budgetGere)}</InfoItem>
                  )}
                </dl>
              </CardContent>
            </Card>
          )}

          {(profil.role === 'CHEF_CHANTIER' || chantierIds.length > 0) && (
            <Card>
              <CardHeader
                title="Chantiers rattachés"
                description="Vos besoins et indicateurs sont filtrés sur ces chantiers."
                icon={<Building2 />}
              />
              <CardContent>
                {chantierIds.length === 0 ? (
                  <EmptyState
                    size="compact"
                    title="Aucun chantier rattaché"
                    description="Contactez l'administrateur pour être rattaché à vos chantiers."
                  />
                ) : chantiers.isPending ? (
                  <Skeleton className="h-16 w-full" />
                ) : (
                  <ul className="grid gap-2 sm:grid-cols-2">
                    {chantiersRattaches.map((chantier) => (
                      <li
                        key={chantier.id}
                        className="rounded-lg border border-border bg-surface-sunken px-3.5 py-2.5"
                      >
                        <p className="text-sm font-medium">{chantier.nom}</p>
                        {chantier.code && (
                          <p className="font-mono text-xs text-muted-foreground">
                            {chantier.code}
                            {chantier.region ? ` · ${chantier.region}` : ''}
                          </p>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </>
  )
}
