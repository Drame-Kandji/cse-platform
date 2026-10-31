import { paths } from '@/app/routes'
import type { Role, Tone } from '@/shared/domain/labels'
import type { QueueSource } from '../api/queues.api'

/** File de travail affichee dans la carte "A traiter". */
export interface QueueDef {
  readonly id: string
  readonly label: string
  readonly hint: string
  readonly source: QueueSource
  readonly href: string
  /** Ton quand la file n'est pas vide. */
  readonly tone: Tone
}

const withQuery = (path: string, query: Record<string, string>) =>
  `${path}?${new URLSearchParams(query).toString()}`

const QUEUES_BY_ROLE: Readonly<Partial<Record<Role, readonly QueueDef[]>>> = {
  RESPONSABLE_MATERIEL: [
    {
      id: 'rm-besoins',
      label: 'Besoins en attente de validation',
      hint: 'À valider ou rejeter (R01)',
      source: 'besoinsEnAttente',
      href: paths.validation,
      tone: 'warning',
    },
    {
      id: 'rm-pannes',
      label: 'Pannes signalées',
      hint: 'À prendre en charge',
      source: 'pannesSignalees',
      href: withQuery(paths.maintenances, { statut: 'SIGNALEE' }),
      tone: 'danger',
    },
  ],
  RESPONSABLE_ACHATS: [
    {
      id: 'ra-ao',
      label: "Appels d'offres publiés",
      hint: 'À clôturer à la date limite',
      source: 'aoPublies',
      href: withQuery(paths.appelsOffres, { statut: 'PUBLIE' }),
      tone: 'info',
    },
    {
      id: 'ra-attributions',
      label: 'Attributions à proposer',
      hint: "Appels d'offres clôturés",
      source: 'aoClotures',
      href: withQuery(paths.appelsOffres, { statut: 'CLOTURE' }),
      tone: 'warning',
    },
    {
      id: 'ra-besoins',
      label: 'Besoins à acheter',
      hint: 'Orientés vers les achats',
      source: 'besoinsAAcheter',
      href: paths.appelsOffres,
      tone: 'primary',
    },
  ],
  DAF: [
    {
      id: 'daf-engagements',
      label: 'Engagements à valider',
      hint: 'Contrats signés (R06)',
      source: 'engagementsEnAttente',
      href: paths.finance,
      tone: 'warning',
    },
    {
      id: 'daf-factures',
      label: 'Factures en retard',
      hint: 'Échéance dépassée',
      source: 'facturesEnRetard',
      href: paths.finance,
      tone: 'danger',
    },
  ],
  DIRECTION_GENERALE: [
    {
      id: 'dg-attributions',
      label: 'Attributions en cours',
      hint: 'Approbation au-delà du seuil (R11)',
      source: 'aoClotures',
      href: withQuery(paths.appelsOffres, { statut: 'CLOTURE' }),
      tone: 'warning',
    },
    {
      id: 'dg-engagements',
      label: 'Engagements en attente',
      hint: 'Validation DAF',
      source: 'engagementsEnAttente',
      href: paths.finance,
      tone: 'info',
    },
    {
      id: 'dg-besoins',
      label: 'Besoins critiques en attente',
      hint: 'Priorité critique',
      source: 'besoinsCritiquesEnAttente',
      href: withQuery(paths.besoins, { statut: 'EN_ATTENTE', priorite: 'CRITIQUE' }),
      tone: 'danger',
    },
  ],
  CHEF_CHANTIER: [
    {
      id: 'chef-receptions',
      label: 'Réceptions à confirmer',
      hint: 'Matériels livrés sur vos chantiers',
      source: 'receptionsAConfirmer',
      href: paths.materiels,
      tone: 'warning',
    },
    {
      id: 'chef-besoins',
      label: 'Mes besoins en attente',
      hint: 'En cours de validation',
      source: 'besoinsEnAttente',
      href: withQuery(paths.besoins, { statut: 'EN_ATTENTE' }),
      tone: 'info',
    },
  ],
}

/** Files "A traiter" des roles de l'utilisateur (dedoublonnees par source). */
export function queuesFor(roles: readonly Role[]): QueueDef[] {
  const seen = new Set<string>()
  return roles
    .flatMap((role) => QUEUES_BY_ROLE[role] ?? [])
    .filter((queue) => {
      if (seen.has(queue.source)) return false
      seen.add(queue.source)
      return true
    })
}
