import { Copy } from 'lucide-react'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { describeStatus } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDateTime } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { IconButton } from '@/shared/ui/button'
import { Sheet, SheetContent } from '@/shared/ui/sheet'
import type { AuditLog } from '../types'

function Row({ label, children, copy }: { label: string; children: ReactNode; copy?: string | null }) {
  return (
    <div className="grid gap-1 border-b border-border py-3 last:border-0">
      <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
      <dd className="flex items-start justify-between gap-2 break-all text-sm">
        <span className="min-w-0">{children}</span>
        {copy && (
          <IconButton
            label={`Copier ${label.toLowerCase()}`}
            icon={<Copy />}
            size="icon-sm"
            className="-my-1.5"
            onClick={() => {
              void navigator.clipboard.writeText(copy).then(
                () => toast.success('Copié'),
                () => toast.error('Copie impossible'),
              )
            }}
          />
        )}
      </dd>
    </div>
  )
}

/** Detail d'une entree du journal d'audit (identifiants copiables pour la correlation). */
export function AuditDetailSheet({ log, onClose }: { log: AuditLog | null; onClose: () => void }) {
  return (
    <Sheet
      open={log !== null}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      {log && (
        <SheetContent
          title={`${log.action} · ${log.entite}`}
          description={formatDateTime(log.occurredAt)}
          size="md"
        >
          {log.resume && (
            <p className="mb-2 rounded-lg bg-surface-sunken px-3 py-2.5 text-sm">{log.resume}</p>
          )}
          <dl>
            <Row label="Service">
              <Badge size="sm">{log.service}</Badge>
            </Row>
            <Row label="Action">
              <span className="font-mono text-xs">{log.action}</span>
            </Row>
            <Row label="Entité">{log.entite}</Row>
            <Row label="Identifiant de l'entité" copy={log.entiteId}>
              <span className="font-mono text-xs">{log.entiteId}</span>
            </Row>
            <Row label="Utilisateur">{log.utilisateurNom ?? log.utilisateurId ?? 'Système'}</Row>
            <Row label="Rôles">
              {log.roles.length ? (
                <span className="flex flex-wrap gap-1">
                  {log.roles.map((role) => (
                    <Badge key={role} size="sm" tone={describeStatus('role', role).tone}>
                      {describeStatus('role', role).label}
                    </Badge>
                  ))}
                </span>
              ) : (
                EMPTY_VALUE
              )}
            </Row>
            <Row label="Horodatage">{formatDateTime(log.occurredAt)}</Row>
            <Row label="Identifiant d'événement" copy={log.eventId}>
              <span className="font-mono text-xs">{log.eventId}</span>
            </Row>
            <Row label="Corrélation" copy={log.correlationId}>
              <span className="font-mono text-xs">{log.correlationId ?? EMPTY_VALUE}</span>
            </Row>
          </dl>
        </SheetContent>
      )}
    </Sheet>
  )
}
