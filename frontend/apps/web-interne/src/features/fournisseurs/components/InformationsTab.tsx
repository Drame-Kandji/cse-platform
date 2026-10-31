import { AlertTriangle, FileText, Landmark } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { labelOf } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { EMPTY_VALUE, formatDate, formatPercent } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import type { FournisseurDetail } from '../api/types'
import { AddDocumentDialog } from './AddDocumentDialog'
import { SpecialitesChips } from './SpecialitesChips'

function Info({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid content-start gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-sm text-foreground">{children ?? EMPTY_VALUE}</dd>
    </div>
  )
}

/** Onglet "Informations" : identite legale, contact, banque et documents administratifs. */
export function InformationsTab({ fournisseur: f }: { fournisseur: FournisseurDetail }) {
  const { t } = useTranslation('fournisseurs')
  const taux = f.tauxLivraisonALHeure
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader title={t('detail.legal')} />
        <CardContent>
          <dl className="grid gap-4 sm:grid-cols-2">
            <Info label="Raison sociale">{f.raisonSociale}</Info>
            <Info label="NINEA">{f.ninea}</Info>
            <Info label="RCCM">{f.rccm}</Info>
            <Info label="Inscription">{formatDate(f.dateInscription)}</Info>
            <Info label="Adresse">{[f.adresse, f.ville, f.pays].filter(Boolean).join(', ')}</Info>
            <Info label="Livraison à l'heure">{taux == null ? null : formatPercent(taux / 100)}</Info>
            <div className="sm:col-span-2">
              <Info label="Spécialités">
                <SpecialitesChips specialites={f.specialites} className="mt-1" />
              </Info>
            </div>
            <div className="sm:col-span-2">
              <Info label={t('detail.certifications')}>
                {f.certifications.length ? f.certifications.join(', ') : null}
              </Info>
            </div>
          </dl>
        </CardContent>
      </Card>
      <div className="grid content-start gap-4">
        <Card>
          <CardHeader title={t('detail.contact')} />
          <CardContent>
            <dl className="grid gap-4 sm:grid-cols-2">
              <Info label="Interlocuteur">
                {f.contactNom ? `${f.contactNom}${f.contactFonction ? ` (${f.contactFonction})` : ''}` : null}
              </Info>
              <Info label="E-mail">{f.email}</Info>
              <Info label="Téléphone">{f.telephone}</Info>
            </dl>
          </CardContent>
        </Card>
        {f.coordonneesBancaires && (
          <Card>
            <CardHeader
              title={t('detail.banque')}
              description={t('detail.banqueHint')}
              icon={<Landmark aria-hidden />}
            />
            <CardContent>
              <dl className="grid gap-4 sm:grid-cols-2">
                <Info label="Banque">{f.coordonneesBancaires.banque}</Info>
                <Info label="IBAN">
                  <span className="font-mono">{f.coordonneesBancaires.iban}</span>
                </Info>
              </dl>
            </CardContent>
          </Card>
        )}
      </div>
      <Card className="lg:col-span-2">
        <CardHeader
          title={t('detail.documents')}
          icon={<FileText aria-hidden />}
          action={
            <Can roles={access.achatsGestion}>
              <AddDocumentDialog fournisseurId={f.id} />
            </Can>
          }
        />
        <CardContent>
          {f.documents.length === 0 ? (
            <EmptyState size="compact" title={t('detail.documentsEmpty')} />
          ) : (
            <ul className="grid gap-2 sm:grid-cols-2">
              {f.documents.map((doc) => (
                <li
                  key={doc.id}
                  className={cn(
                    'flex items-center justify-between gap-3 rounded-lg border p-3',
                    doc.expire ? 'border-danger/40 bg-danger/5' : 'border-border',
                  )}
                >
                  <div className="grid min-w-0">
                    <span className="truncate text-sm font-medium">{doc.libelle}</span>
                    <span className="text-xs text-muted-foreground">
                      {labelOf('typeDocument', doc.type)}
                      {doc.dateExpiration ? ` · expire le ${formatDate(doc.dateExpiration)}` : ''}
                    </span>
                  </div>
                  {doc.expire && (
                    <Badge tone="danger">
                      <AlertTriangle aria-hidden />
                      {t('detail.expire')}
                    </Badge>
                  )}
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
