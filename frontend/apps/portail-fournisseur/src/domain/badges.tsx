import { Badge, type BadgeProps } from '@/shared/ui/badge'
import { describeContrat, RESULTAT_OFFRE_LABELS } from './labels'
import type { ResultatOffre } from './types'

type Props = Omit<BadgeProps, 'tone' | 'children'>

/** Statut d'un contrat vu par le fournisseur (« À signer », « À expédier »...). */
export function ContratBadge({ statut, ...props }: Props & { statut: string }) {
  const descriptor = describeContrat(statut)
  return (
    <Badge tone={descriptor.tone} data-status={statut} {...props}>
      {descriptor.label}
    </Badge>
  )
}

/** Resultat d'une offre : En cours, Retenue, Non retenue. */
export function ResultatBadge({ resultat, ...props }: Props & { resultat: ResultatOffre }) {
  const descriptor = RESULTAT_OFFRE_LABELS[resultat]
  return (
    <Badge tone={descriptor.tone} data-status={resultat} {...props}>
      {descriptor.label}
    </Badge>
  )
}
