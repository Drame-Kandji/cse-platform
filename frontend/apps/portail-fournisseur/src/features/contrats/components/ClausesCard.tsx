import { useTranslation } from 'react-i18next'
import { formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { InfoList } from '@/components/InfoList'
import type { Contrat } from '@/domain/types'

/** Clauses contractuelles figees a la signature CSE. */
export function ClausesCard({ clauses }: { clauses: Contrat['clauses'] }) {
  const { t } = useTranslation('portail')
  const textes = [
    { label: t('contrats.clauses.livraison'), value: clauses.conditionsLivraison },
    { label: t('contrats.clauses.maintenance'), value: clauses.conditionsMaintenance },
  ].filter((c): c is { label: string; value: string } => Boolean(c.value))
  const chiffres = [
    {
      label: t('contrats.clauses.garantie'),
      value: t('ao.garantieMois', { n: clauses.garantieMois ?? 0 }),
      hidden: clauses.garantieMois == null,
    },
    {
      label: t('contrats.clauses.penalite'),
      value: t('contrats.clauses.parJour', {
        pct: formatNumber(clauses.penaliteRetardPctParJour, { decimals: 2 }),
      }),
      hidden: clauses.penaliteRetardPctParJour == null,
    },
  ]
  if (textes.length === 0 && chiffres.every((c) => c.hidden)) return null
  return (
    <Card>
      <CardHeader title={t('contrats.clauses.title')} />
      <CardContent className="grid gap-4 pt-4">
        <InfoList items={chiffres} />
        {textes.map((c) => (
          <div key={c.label} className="grid gap-1 text-sm">
            <p className="text-muted-foreground">{c.label}</p>
            <p className="whitespace-pre-line">{c.value}</p>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
