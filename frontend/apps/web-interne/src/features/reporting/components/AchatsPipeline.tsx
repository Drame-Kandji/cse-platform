import { formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import type { SectionAchats } from '../types'

/** Entonnoir des achats : AO publies, clotures, attribues, contrats en cours, retards. */
export function AchatsPipeline({ achats }: { achats: SectionAchats }) {
  const steps = [
    { label: 'AO publiés', value: achats.aoPublies },
    { label: 'AO clôturés', value: achats.aoClotures },
    { label: 'AO attribués', value: achats.aoAttribues },
    { label: 'Contrats en cours', value: achats.contratsEnCours },
  ]
  const max = Math.max(1, ...steps.map((step) => step.value))
  return (
    <Card className="flex h-full flex-col">
      <CardHeader
        title="Cycle d'approvisionnement"
      />
      <CardContent className="grid flex-1 content-start gap-3">
        {steps.map((step) => (
          <div
            key={step.label}
            className="grid grid-cols-[8rem_minmax(0,1fr)_3rem] items-center gap-3 text-sm"
          >
            <span className="text-muted-foreground">{step.label}</span>
            <div className="h-2.5 overflow-hidden rounded-full bg-muted" aria-hidden>
              <div
                className="h-full rounded-full bg-chart-1"
                style={{ width: `${(step.value / max) * 100}%` }}
              />
            </div>
            <span className="text-right font-semibold tabular-nums">{formatNumber(step.value)}</span>
          </div>
        ))}
        <p className="mt-1 flex items-center justify-between border-t border-border pt-3 text-sm">
          <span className="text-muted-foreground">Livraisons en retard</span>
          <span
            className={achats.livraisonsEnRetard > 0 ? 'font-semibold text-danger-emphasis' : 'font-semibold'}
          >
            {formatNumber(achats.livraisonsEnRetard)}
          </span>
        </p>
      </CardContent>
    </Card>
  )
}
