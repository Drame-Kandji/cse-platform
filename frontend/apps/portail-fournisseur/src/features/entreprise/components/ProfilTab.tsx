import { Pencil } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { labelOf } from '@/shared/domain/labels'
import { formatDate, formatNumber, formatPercent } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { InfoList } from '@/components/InfoList'
import type { ProfilFournisseur } from '@/domain/types'
import { ProfilDialog } from './ProfilDialog'

/** Coordonnees (modifiables) et performance de l'entreprise. */
export function ProfilTab({ profil }: { profil: ProfilFournisseur }) {
  const { t } = useTranslation('portail')
  const [open, setOpen] = useState(false)
  const contact = [profil.contactNom, profil.contactFonction].filter(Boolean).join(' · ')
  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <Card className="lg:col-span-2">
        <CardHeader
          title={t('entreprise.coordonnees')}
          action={
            <Button
              size="sm"
              variant="secondary"
              leftIcon={<Pencil />}
              onClick={() => {
                setOpen(true)
              }}
            >
              {t('entreprise.modifier')}
            </Button>
          }
        />
        <CardContent className="pt-4">
          <InfoList
            items={[
              {
                label: t('entreprise.fields.adresse'),
                value: [profil.adresse, profil.ville, profil.pays].filter(Boolean).join(', '),
              },
              { label: t('entreprise.fields.telephone'), value: profil.telephone, hidden: !profil.telephone },
              { label: t('entreprise.fields.email'), value: profil.email },
              { label: t('entreprise.contact'), value: contact, hidden: !contact },
              { label: t('entreprise.fields.rccm'), value: profil.rccm, hidden: !profil.rccm },
              {
                label: t('entreprise.fields.specialites'),
                value: profil.specialites.map((s) => labelOf('categorieMateriel', s)).join(', '),
              },
              {
                label: t('entreprise.fields.certifications'),
                value: profil.certifications.join(', '),
                hidden: profil.certifications.length === 0,
              },
              {
                label: t('entreprise.fields.banque'),
                value: profil.coordonneesBancaires?.banque,
                hidden: !profil.coordonneesBancaires,
              },
            ]}
          />
        </CardContent>
      </Card>
      <Card className="content-start">
        <CardHeader title={t('entreprise.performance')} />
        <CardContent className="grid gap-4 pt-3">
          <p className="text-3xl font-semibold tabular-nums tracking-tight">
            {formatNumber(profil.scorePerformance, { decimals: 1 })}
            <span className="text-base font-normal text-muted-foreground"> / 100</span>
          </p>
          <InfoList
            items={[
              { label: t('entreprise.evaluations'), value: formatNumber(profil.nombreEvaluations) },
              {
                label: t('entreprise.ponctualite'),
                value: formatPercent((profil.tauxLivraisonALHeure ?? 0) / 100),
                hidden: profil.tauxLivraisonALHeure == null,
              },
              { label: t('entreprise.inscription'), value: formatDate(profil.dateInscription) },
            ]}
          />
          {profil.motifStatut && <p className="text-sm text-muted-foreground">{profil.motifStatut}</p>}
        </CardContent>
      </Card>
      <ProfilDialog profil={profil} open={open} onOpenChange={setOpen} />
    </div>
  )
}
