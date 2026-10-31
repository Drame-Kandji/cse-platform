import { labelOf, type EtatMateriel } from '@/shared/domain/labels'
import { CountTabs } from '@/shared/ui/count-tabs'
import type { EtatCount } from '../hooks/useParcQueries'

export interface EtatSummaryProps {
  counts: readonly EtatCount[]
  total: number
  loading: boolean
  selected: EtatMateriel | null
  onToggle: (etat: EtatMateriel | null) => void
}

/** Compteurs par etat (filtres courants), en texte : un clic filtre la liste, re-clic = tous. */
export function EtatSummary({ counts, total, loading, selected, onToggle }: EtatSummaryProps) {
  return (
    <CountTabs<EtatMateriel>
      label="Répartition par état"
      loading={loading}
      value={selected}
      onChange={onToggle}
      items={[
        { value: null, label: 'Tous', count: total },
        ...counts.map(({ etat, count }) => ({ value: etat, label: labelOf('etatMateriel', etat), count })),
      ]}
    />
  )
}
