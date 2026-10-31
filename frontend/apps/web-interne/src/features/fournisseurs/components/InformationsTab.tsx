import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { labelOf } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDate, formatPercent } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import type { FournisseurDetail } from '../api/types'
import { AddDocumentDialog } from './AddDocumentDialog'

function Info({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid content-start gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-sm text-foreground">{children ?? EMPTY_VALUE}</dd>
    </div>
  )
}

/** Onglet "Informations" : faits essentiels (identite, contact, banque) et documents administratifs. */
export function InformationsTab({ fournisseur: f }: { fournisseur: FournisseurDetail }) {
  const { t } = useTranslation('fournisseurs')
  const taux = f.tauxLivraisonALHeure
  return (
    <div className="grid gap-4">
      <Card>
        <CardContent className="grid gap-5">
          <dl className="grid grid-cols-2 gap-4 md:grid-cols-4">
            <Info label="NINEA">{f.ninea}</Info>
            <Info label="RCCM">{f.rccm}</Info>
            <Info label="Inscription">{formatDate(f.dateInscription)}</Info>
            <Info label="Livraison à l'heure">{taux == null ? null : formatPercent(taux / 100)}</Info>
            <Info label="Interlocuteur">
              {f.contactNom ? `${f.contactNom}${f.contactFonction ? ` (${f.contactFonction})` : ''}` : null}
            </Info>
            <Info label="E-mail">
              <span className="break-all">{f.email}</span>
            </Info>
            <Info label="Téléphone">{f.telephone}</Info>
            <Info label="Adresse">{[f.adresse, f.ville, f.pays].filter(Boolean).join(', ')}</Info>
            <Info label="Spécialités">
              {f.specialites.map((s) => labelOf('categorieMateriel', s)).join(', ') || null}
            </Info>
            <div className="col-span-2 md:col-span-3">
              <Info label={t('detail.certifications')}>
                {f.certifications.length ? f.certifications.join(', ') : null}
              </Info>
            </div>
          </dl>
          {f.coordonneesBancaires && (
            <div className="grid gap-3 border-t border-border pt-4" title={t('detail.banqueHint')}>
              <h3 className="text-sm font-medium">{t('detail.banque')}</h3>
              <dl className="grid grid-cols-2 gap-4 md:grid-cols-4">
                <Info label="Banque">{f.coordonneesBancaires.banque}</Info>
                <div className="md:col-span-2">
                  <Info label="IBAN">
                    <span className="break-all">{f.coordonneesBancaires.iban}</span>
                  </Info>
                </div>
              </dl>
            </div>
          )}
        </CardContent>
      </Card>
      <Card>
        <CardHeader
          title={t('detail.documents')}
          action={
            <Can roles={access.achatsGestion}>
              <AddDocumentDialog fournisseurId={f.id} />
            </Can>
          }
        />
        {f.documents.length === 0 ? (
          <CardContent>
            <EmptyState size="compact" title={t('detail.documentsEmpty')} />
          </CardContent>
        ) : (
          <ul className="divide-y divide-border border-t border-border">
            {f.documents.map((doc) => (
              <li key={doc.id} className="flex items-center justify-between gap-3 px-4 py-3 sm:px-5">
                <div className="grid min-w-0">
                  <span className="truncate text-sm font-medium">{doc.libelle}</span>
                  <span className="text-xs text-muted-foreground">
                    {labelOf('typeDocument', doc.type)}
                    {doc.dateExpiration ? ` · expire le ${formatDate(doc.dateExpiration)}` : ''}
                  </span>
                </div>
                {doc.expire && (
                  <span className="text-xs font-medium text-danger-emphasis">{t('detail.expire')}</span>
                )}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
