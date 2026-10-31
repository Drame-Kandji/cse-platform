import { Download, Plus, Settings, Trash2 } from 'lucide-react'
import { STATUS_MAPS, type StatusKind } from '@/shared/domain/labels'
import { Avatar } from '@/shared/ui/avatar'
import { Badge } from '@/shared/ui/badge'
import { Button, IconButton } from '@/shared/ui/button'
import { Kbd } from '@/shared/ui/kbd'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Demo, Section } from './Section'

const KINDS = Object.keys(STATUS_MAPS) as StatusKind[]
const TONES = ['neutral', 'primary', 'info', 'success', 'warning', 'danger', 'accent'] as const

/** Boutons, badges, statuts metier, avatars. */
export function ActionsSection() {
  return (
    <Section id="actions" title="Actions & étiquettes">
      <Demo title="Boutons">
        <div className="grid gap-4">
          <div className="flex flex-wrap items-center gap-3">
            <Button leftIcon={<Plus />}>Nouveau besoin</Button>
            <Button variant="secondary" leftIcon={<Download />}>
              Exporter
            </Button>
            <Button variant="ghost">Annuler</Button>
            <Button variant="subtle">Subtil</Button>
            <Button variant="accent">Accent</Button>
            <Button variant="danger" leftIcon={<Trash2 />}>
              Supprimer
            </Button>
            <Button variant="link">Lien</Button>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <Button size="sm">Petit</Button>
            <Button size="md">Moyen</Button>
            <Button size="lg">Grand</Button>
            <Button loading>Enregistrement</Button>
            <Button disabled>Désactivé</Button>
            <IconButton label="Paramètres" icon={<Settings />} variant="secondary" />
            <IconButton label="Supprimer" icon={<Trash2 />} />
          </div>
        </div>
      </Demo>
      <Demo title="Badges">
        <div className="grid gap-3">
          <div className="flex flex-wrap gap-2">
            {TONES.map((tone) => (
              <Badge key={tone} tone={tone}>
                {tone}
              </Badge>
            ))}
          </div>
          <div className="flex flex-wrap gap-2">
            {TONES.map((tone) => (
              <Badge key={tone} tone={tone} variant="solid">
                {tone}
              </Badge>
            ))}
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline">Contour</Badge>
            <Badge tone="success" dot size="sm">
              Petit
            </Badge>
            <span className="text-sm text-muted-foreground">
              Raccourci <Kbd>Ctrl</Kbd> <Kbd>K</Kbd>
            </span>
          </div>
        </div>
      </Demo>
      <Demo title="StatusBadge — référentiel shared/domain/labels.ts">
        <div className="grid gap-4">
          {KINDS.map((kind) => (
            <div key={kind} className="grid gap-2 sm:grid-cols-[11rem_1fr] sm:items-center">
              <span className="font-mono text-xs text-muted-foreground">{kind}</span>
              <div className="flex flex-wrap gap-2">
                {Object.keys(STATUS_MAPS[kind]).map((value) => (
                  <StatusBadge key={value} kind={kind} value={value} />
                ))}
              </div>
            </div>
          ))}
        </div>
      </Demo>
      <Demo title="Avatars">
        <div className="flex items-center gap-3">
          <Avatar name="Moussa Ndiaye" size="sm" />
          <Avatar name="Fatou Sarr" />
          <Avatar name="Ibrahima Fall" size="lg" />
          <Avatar name="Aminata Diallo" />
          <Avatar name="Cheikh Ba" />
        </div>
      </Demo>
    </Section>
  )
}
