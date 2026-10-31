import { MessageSquare } from 'lucide-react'
import { Stepper } from '@/shared/ui/stepper'
import { Timeline, type TimelineItem } from '@/shared/ui/timeline'
import { Demo, Section } from './Section'

const STEPS = [
  { id: 'saisie', label: 'Expression', description: 'Chef de chantier' },
  { id: 'validation', label: 'Validation', description: 'Responsable Matériel' },
  { id: 'achat', label: 'Achat', description: "Appel d'offres" },
  { id: 'livraison', label: 'Livraison', description: 'Réception chantier' },
]

const hoursAgo = (hours: number) => new Date(Date.now() - hours * 3600_000)
const EVENTS: TimelineItem[] = [
  {
    id: '4',
    title: 'Passage en achat',
    description: "Appel d'offres AO-2026-031 publié auprès de 6 fournisseurs.",
    date: hoursAgo(2),
    actor: 'Ibrahima Fall · Resp. Achats',
    tone: 'primary',
  },
  {
    id: '3',
    title: 'Commentaire',
    description: 'Aucun compacteur disponible sur la période demandée.',
    date: hoursAgo(20),
    actor: 'Ibrahima Fall · Resp. Matériel',
    icon: <MessageSquare />,
  },
  {
    id: '2',
    title: 'Besoin validé',
    date: hoursAgo(22),
    actor: 'Ibrahima Fall · Resp. Matériel',
    tone: 'success',
  },
  {
    id: '1',
    title: 'Besoin créé',
    description: '2 compacteurs vibrants — priorité urgente.',
    date: hoursAgo(50),
    actor: 'Mamadou Diop · Chef de chantier',
    tone: 'warning',
  },
]

/** Stepper (workflow) et Timeline (historique). */
export function WorkflowSection() {
  return (
    <Section id="workflow" title="Workflow & historique">
      <Demo title="Stepper — étape courante : Achat">
        <Stepper steps={STEPS} current={2} />
      </Demo>
      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-2">
        <Demo title="Stepper — besoin rejeté à la validation">
          <Stepper steps={STEPS.slice(0, 2)} current={1} errorIndex={1} />
        </Demo>
        <Demo title="Timeline">
          <Timeline items={EVENTS} />
        </Demo>
      </div>
    </Section>
  )
}
