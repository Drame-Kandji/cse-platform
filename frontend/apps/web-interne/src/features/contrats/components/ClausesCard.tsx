import { FileText, Pencil } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { EMPTY_VALUE, formatNumber } from '@/shared/lib/format'
import { Button, Card, CardContent, CardHeader } from '@/shared/ui'
import type { Contrat } from '../api/types'
import { ClausesDialog } from './ClausesDialog'

/** Clauses contractuelles ; modifiables par le RA tant que le contrat est BROUILLON. */
export function ClausesCard({ contrat, editable }: { contrat: Contrat; editable: boolean }) {
  const { t } = useTranslation('contrats')
  const [open, setOpen] = useState(false)
  const c = contrat.clauses
  const rows = [
    {
      label: t('clauses.garantie'),
      value: typeof c.garantieMois === 'number' ? `${formatNumber(c.garantieMois)} mois` : EMPTY_VALUE,
    },
    {
      label: t('clauses.penalite'),
      value:
        typeof c.penaliteRetardPctParJour === 'number'
          ? `${formatNumber(c.penaliteRetardPctParJour, { decimals: 2 })} %`
          : EMPTY_VALUE,
    },
    { label: t('clauses.livraison'), value: c.conditionsLivraison ?? EMPTY_VALUE },
    { label: t('clauses.maintenance'), value: c.conditionsMaintenance ?? EMPTY_VALUE },
  ]
  return (
    <Card>
      <CardHeader
        icon={<FileText aria-hidden />}
        title={t('clauses.title')}
        action={
          editable ? (
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<Pencil />}
              onClick={() => {
                setOpen(true)
              }}
            >
              {t('clauses.edit')}
            </Button>
          ) : null
        }
      />
      <CardContent>
        <dl className="grid gap-3 text-sm">
          {rows.map((row) => (
            <div key={row.label} className="grid gap-0.5">
              <dt className="text-xs text-muted-foreground">{row.label}</dt>
              <dd className="whitespace-pre-line text-foreground">{row.value}</dd>
            </div>
          ))}
        </dl>
      </CardContent>
      {editable && <ClausesDialog contrat={contrat} open={open} onOpenChange={setOpen} />}
    </Card>
  )
}
