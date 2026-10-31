import {
  AlertOctagon,
  ArrowRightLeft,
  Archive,
  Gauge,
  Gavel,
  MoreHorizontal,
  Pencil,
  PackageCheck,
  Undo2,
  Wrench,
} from 'lucide-react'
import { useState } from 'react'
import { useCurrentUser } from '@/shared/auth/context'
import type { MaterielDetail } from '@/shared/domain/types'
import { AffecterDialog, SignalerPanneDialog } from '@/shared/suivi'
import { Button } from '@/shared/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'
import { Tooltip } from '@/shared/ui/tooltip'
import { materielPermissions } from '../../lib/permissions'
import { MaterielFormDialog } from '../MaterielFormDialog'
import { HeuresDialog } from './HeuresDialog'
import { LifecycleDialogs, type LifecycleDialog } from './LifecycleDialogs'
import { ReformeDecisionDialog } from './ReformeDecisionDialog'

type DialogKey = LifecycleDialog | 'affecter' | 'heures' | 'panne' | 'modifier' | 'decision'

/** Barre d'actions de la fiche engin, filtree par role, chantier et etat. */
export function MaterielActions({ materiel }: { materiel: MaterielDetail }) {
  const user = useCurrentUser()
  const can = materielPermissions(materiel, user)
  const [dialog, setDialog] = useState<DialogKey | null>(null)
  const openChange = (key: DialogKey) => (open: boolean) => {
    setDialog(open ? key : null)
  }
  const secondary = [
    can.liberer && { key: 'liberer' as const, label: 'Libérer', icon: <Undo2 /> },
    can.releverHeures && { key: 'heures' as const, label: "Relevé d'heures", icon: <Gauge /> },
    can.changerEtat && {
      key: 'etat' as const,
      label: materiel.etat === 'EN_MAINTENANCE' ? 'Remettre en service' : 'Mettre en maintenance',
      icon: <Wrench />,
    },
    can.signalerPanne && { key: 'panne' as const, label: 'Signaler une panne', icon: <AlertOctagon /> },
    can.modifier && { key: 'modifier' as const, label: 'Modifier la fiche', icon: <Pencil /> },
  ].filter((item) => item !== false)

  return (
    <>
      {can.confirmerReception && (
        <Button
          leftIcon={<PackageCheck />}
          onClick={() => {
            setDialog('reception')
          }}
        >
          Confirmer la réception
        </Button>
      )}
      {can.validerReforme && (
        <Button
          variant="danger"
          leftIcon={<Gavel />}
          onClick={() => {
            setDialog('decision')
          }}
        >
          Décider de la réforme
        </Button>
      )}
      {can.affecter &&
        (can.affecterBloque ? (
          <Tooltip content={can.affecterBloque}>
            <span className="inline-flex rounded-lg">
              <Button
                variant="secondary"
                leftIcon={<ArrowRightLeft />}
                disabled
                aria-describedby="affecter-bloque"
              >
                Affecter
              </Button>
              <span id="affecter-bloque" className="sr-only">
                {can.affecterBloque}
              </span>
            </span>
          </Tooltip>
        ) : (
          <Button
            variant={can.confirmerReception ? 'secondary' : 'primary'}
            leftIcon={<ArrowRightLeft />}
            onClick={() => {
              setDialog('affecter')
            }}
          >
            Affecter
          </Button>
        ))}
      {(secondary.length > 0 || can.demanderReforme) && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="secondary" size="icon" aria-label="Actions" title="Actions">
              <MoreHorizontal aria-hidden />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            {secondary.map((item) => (
              <DropdownMenuItem
                key={item.key}
                icon={item.icon}
                onSelect={() => {
                  setDialog(item.key)
                }}
              >
                {item.label}
              </DropdownMenuItem>
            ))}
            {can.demanderReforme && (
              <>
                {secondary.length > 0 && <DropdownMenuSeparator />}
                <DropdownMenuItem
                  destructive
                  icon={<Archive />}
                  onSelect={() => {
                    setDialog('reforme')
                  }}
                >
                  Demander la réforme
                </DropdownMenuItem>
              </>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      )}

      <AffecterDialog
        open={dialog === 'affecter'}
        onOpenChange={openChange('affecter')}
        materiel={materiel}
      />
      <SignalerPanneDialog open={dialog === 'panne'} onOpenChange={openChange('panne')} materiel={materiel} />
      <MaterielFormDialog
        open={dialog === 'modifier'}
        onOpenChange={openChange('modifier')}
        materiel={materiel}
      />
      <HeuresDialog
        open={dialog === 'heures'}
        onOpenChange={openChange('heures')}
        materielId={materiel.id}
        compteurActuel={materiel.heuresFonctionnement}
      />
      <ReformeDecisionDialog
        open={dialog === 'decision'}
        onOpenChange={openChange('decision')}
        materielId={materiel.id}
        motif={materiel.motifReforme}
      />
      <LifecycleDialogs
        materiel={materiel}
        open={
          dialog === 'reception' || dialog === 'liberer' || dialog === 'etat' || dialog === 'reforme'
            ? dialog
            : null
        }
        onClose={() => {
          setDialog(null)
        }}
      />
    </>
  )
}
