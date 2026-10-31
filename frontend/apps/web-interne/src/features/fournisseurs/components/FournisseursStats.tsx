import type { StatutFournisseur } from '@/shared/domain/labels'
import { CountTabs, type CountTabItem } from '@/shared/ui/count-tabs'
import { useStatistiquesFournisseurs } from '../hooks/useFournisseursQueries'

interface Props {
  value: StatutFournisseur | undefined
  onChange: (statut: StatutFournisseur | undefined) => void
}

/** Compteurs par statut au-dessus de la liste (filtre segmente ; masques en cas d'erreur). */
export function FournisseursStats({ value, onChange }: Props) {
  const query = useStatistiquesFournisseurs()
  if (query.isError) return null
  const s = query.data
  const items: CountTabItem<StatutFournisseur>[] = [
    { value: null, label: 'Tous', count: s?.total },
    { value: 'ACTIF', label: 'Actifs', count: s?.actifs },
    { value: 'EN_ATTENTE_VALIDATION', label: 'À valider', count: s?.enAttente },
    { value: 'SUSPENDU', label: 'Suspendus', count: s?.suspendus },
    { value: 'BLACKLISTE', label: 'Blacklistés', count: s?.blacklistes },
  ]
  return (
    <CountTabs
      label="Statut"
      items={items}
      value={value ?? null}
      loading={query.isPending}
      onChange={(next) => {
        onChange(next ?? undefined)
      }}
    />
  )
}
