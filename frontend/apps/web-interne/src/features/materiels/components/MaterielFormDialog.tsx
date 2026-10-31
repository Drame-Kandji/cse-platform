import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { to } from '@/app/routes'
import { useQueryClient } from '@tanstack/react-query'
import { getErrorMessage, isApiError } from '@/shared/api/errors'
import type { Materiel } from '@/shared/domain/types'
import { applyServerErrors } from '@/shared/lib/forms'
import { toDate, toIsoDate } from '@/shared/lib/format'
import { InlineAlert, materielKeys } from '@/shared/suivi'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { useCreerMateriel, useModifierMateriel } from '../hooks/useMaterielActions'
import { materielSchema, type MaterielFormInput, type MaterielFormOutput } from '../schemas/materiel.schema'
import { MaterielFormFields } from './MaterielFormFields'

export interface MaterielFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Materiel a modifier ; absent = creation. */
  materiel?: Materiel
}

function defaults(materiel?: Materiel): MaterielFormInput {
  const text = (value: number | null | undefined) =>
    value === null || value === undefined ? '' : String(value)
  return {
    code: materiel?.code ?? '',
    immatriculation: materiel?.immatriculation ?? '',
    designation: materiel?.designation ?? '',
    categorie: materiel?.categorie ?? ('' as MaterielFormInput['categorie']),
    typeEnginId: materiel?.typeEnginId ?? null,
    marque: materiel?.marque ?? '',
    modele: materiel?.modele ?? '',
    tarifJournalier: text(materiel?.tarifJournalier),
    valeurAcquisition: text(materiel?.valeurAcquisition),
    dateAcquisition: toDate(materiel?.dateAcquisition),
    heuresFonctionnement: text(materiel?.heuresFonctionnement),
  }
}

/** Creation ou modification d'une fiche materiel (RM). */
export function MaterielFormDialog({ open, onOpenChange, materiel }: MaterielFormDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        size="lg"
        title={materiel ? 'Modifier la fiche' : 'Nouveau matériel'}
        description={
          materiel
            ? `${materiel.code} — ${materiel.designation}`
            : 'Enregistrement d’un matériel dans le parc.'
        }
      >
        {open && (
          <MaterielForm
            materiel={materiel}
            onDone={() => {
              onOpenChange(false)
            }}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function MaterielForm({ materiel, onDone }: { materiel?: Materiel; onDone: () => void }) {
  const navigate = useNavigate()
  const [error, setError] = useState<unknown>(null)
  const queryClient = useQueryClient()
  const creer = useCreerMateriel()
  const modifier = useModifierMateriel(materiel?.id ?? '')
  const pending = creer.isPending || modifier.isPending
  const form = useForm<MaterielFormInput, unknown, MaterielFormOutput>({
    resolver: zodResolver(materielSchema),
    defaultValues: defaults(materiel),
  })

  const submit = form.handleSubmit(async ({ dateAcquisition, heuresFonctionnement, ...values }) => {
    setError(null)
    const body = {
      ...values,
      tarifJournalier: values.tarifJournalier ?? 0,
      dateAcquisition: toIsoDate(dateAcquisition),
    }
    try {
      if (materiel) {
        await modifier.mutateAsync({ ...body, version: materiel.version })
        onDone()
      } else {
        const created = await creer.mutateAsync({ ...body, heuresFonctionnement })
        onDone()
        navigate(to.materielDetail(created.id))
      }
    } catch (cause) {
      if (!applyServerErrors(cause, form.setError)) setError(cause)
    }
  })

  return (
    <form onSubmit={(event) => void submit(event)} noValidate className="grid gap-4">
      <MaterielFormFields control={form.control} creation={!materiel} />
      {error !== null &&
        (isApiError(error) && error.code === 'CONFLIT_CONCURRENCE' ? (
          <InlineAlert tone="warning" title="La fiche a été modifiée entre-temps">
            <p>Rechargez la fiche pour reprendre la dernière version, puis appliquez vos changements.</p>
            <Button
              variant="secondary"
              size="sm"
              className="mt-2"
              onClick={() => {
                void queryClient.invalidateQueries({ queryKey: materielKeys.all })
                onDone()
              }}
            >
              Recharger la fiche
            </Button>
          </InlineAlert>
        ) : (
          <InlineAlert tone="danger" title="Enregistrement impossible">
            {getErrorMessage(error)}
          </InlineAlert>
        ))}
      <DialogFooter>
        <Button variant="secondary" onClick={onDone} disabled={pending}>
          Annuler
        </Button>
        <Button type="submit" loading={pending}>
          {materiel ? 'Enregistrer' : 'Créer le matériel'}
        </Button>
      </DialogFooter>
    </form>
  )
}
