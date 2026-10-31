import { describeStatus, type StatusKind, type StatusValue } from '@/shared/domain/labels'
import { Badge, type BadgeProps } from './badge'

export interface StatusBadgeProps<K extends StatusKind> extends Omit<BadgeProps, 'tone' | 'children'> {
  /** Enumeration metier (cle de STATUS_MAPS). */
  kind: K
  /** Valeur brute renvoyee par l'API (ex. "EN_ATTENTE"). */
  value: StatusValue<K> | (string & {})
}

/**
 * Badge d'etat pilote par le referentiel central `shared/domain/labels.ts` : pastille discrete,
 * grise par defaut, bleue si actif / en cours, rouge si erreur ou critique (sans point colore).
 * @example <StatusBadge kind="statutBesoin" value={besoin.statut} />
 */
export function StatusBadge<K extends StatusKind>({
  kind,
  value,
  dot = false,
  ...props
}: StatusBadgeProps<K>) {
  const descriptor = describeStatus(kind, value)
  return (
    <Badge tone={descriptor.tone} dot={dot} title={descriptor.description} data-status={value} {...props}>
      {descriptor.label}
    </Badge>
  )
}
