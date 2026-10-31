import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { isApiError } from '@/shared/api/errors'
import type { Affectation, Materiel } from '@/shared/domain/types'
import { applyServerErrors } from '@/shared/lib/forms'
import { toIsoDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Combobox } from '@/shared/ui/combobox'
import { DatePicker } from '@/shared/ui/date-picker'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { AffectationAlert, MaterielSummary } from './AffecterDialogParts'
import {
  useAffecterMateriel,
  useBesoinOptions,
  useChantierOptions,
  useMaterielDetail,
  useMateriels,
} from './hooks'
import { affectationSchema, type AffectationFormInput, type AffectationFormOutput } from './schemas'

/** Materiel pre-selectionne (fiche engin, ligne du planning). `etat` absent : lu sur la fiche. */
export type MaterielAffectable = Pick<Materiel, 'id' | 'code' | 'designation'> &
  Partial<Pick<Materiel, 'etat'>>

export interface AffecterDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Materiel impose ; sinon un selecteur de materiels disponibles est affiche. */
  materiel?: MaterielAffectable
  defaultChantierId?: string
  defaultDateDebut?: Date
  defaultDateFinPrevue?: Date
  onAffected?: (affectation: Affectation) => void
}

/**
 * Dialogue "Affecter un materiel" (RM) : chantier, periode, besoin facultatif.
 * Controle R04 cote client (materiel DISPONIBLE) ; le conflit de periode (409 CONFLIT_AFFECTATION)
 * et les refus metier (422) sont affiches dans le dialogue.
 */
export function AffecterDialog({ open, onOpenChange, ...props }: AffecterDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent size="md" title="Affecter un matériel">
        {open && (
          <AffecterForm
            {...props}
            onDone={() => {
              onOpenChange(false)
            }}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function AffecterForm({
  materiel,
  defaultChantierId,
  defaultDateDebut,
  defaultDateFinPrevue,
  onAffected,
  onDone,
}: Omit<AffecterDialogProps, 'open' | 'onOpenChange'> & { onDone: () => void }) {
  const [search, setSearch] = useState('')
  const [serverError, setServerError] = useState<unknown>(null)
  const form = useForm<AffectationFormInput, unknown, AffectationFormOutput>({
    resolver: zodResolver(affectationSchema),
    defaultValues: {
      materielId: materiel?.id ?? '',
      chantierId: defaultChantierId ?? null,
      besoinId: null,
      dateDebut: defaultDateDebut ?? new Date(),
      dateFinPrevue: defaultDateFinPrevue ?? null,
    },
  })
  const chantierId = form.watch('chantierId')
  const dateDebut = form.watch('dateDebut')
  const needsEtat = materiel !== undefined && materiel.etat === undefined
  const detail = useMaterielDetail(needsEtat ? materiel.id : undefined)
  const disponibles = useMateriels({ etat: 'DISPONIBLE', q: search || undefined, page: 0, size: 50 })
  const chantiers = useChantierOptions({ enCoursSeulement: true })
  const besoins = useBesoinOptions(chantierId)
  const affecter = useAffecterMateriel()

  const etat = materiel ? (materiel.etat ?? detail.data?.etat) : 'DISPONIBLE'
  const nonDisponible = etat !== undefined && etat !== 'DISPONIBLE'
  const materielOptions = (disponibles.data?.content ?? []).map((item) => ({
    value: item.id,
    label: `${item.code} — ${item.designation}`,
    description: item.typeEnginLibelle ?? undefined,
  }))

  const submit = form.handleSubmit(async (values) => {
    setServerError(null)
    try {
      const affectation = await affecter.mutateAsync({
        materielId: values.materielId,
        chantierId: values.chantierId ?? '',
        besoinId: values.besoinId ?? undefined,
        dateDebut: toIsoDate(values.dateDebut) ?? '',
        dateFinPrevue: toIsoDate(values.dateFinPrevue) ?? '',
      })
      onAffected?.(affectation)
      onDone()
    } catch (error) {
      if (!applyServerErrors(error, form.setError) || (isApiError(error) && error.status === 409)) {
        setServerError(error)
      }
    }
  })

  return (
    <form onSubmit={(event) => void submit(event)} noValidate className="grid gap-4">
      {materiel ? (
        <MaterielSummary materiel={materiel} etat={etat} loading={needsEtat && detail.isPending} />
      ) : (
        <FormField control={form.control} name="materielId" label="Matériel disponible" required>
          {({ field, control }) => (
            <Combobox
              {...control}
              options={materielOptions}
              value={field.value || null}
              onValueChange={(value) => {
                field.onChange(value ?? '')
              }}
              onSearchChange={setSearch}
              loading={disponibles.isFetching}
              placeholder="Rechercher par code ou désignation"
              emptyText="Aucun matériel disponible"
            />
          )}
        </FormField>
      )}
      <FormField control={form.control} name="chantierId" label="Chantier" required>
        {({ field, control }) => (
          <Combobox
            {...control}
            options={chantiers.data ?? []}
            value={field.value}
            onValueChange={(value) => {
              field.onChange(value)
              form.setValue('besoinId', null)
            }}
            loading={chantiers.isPending}
            placeholder="Choisir un chantier"
          />
        )}
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField control={form.control} name="dateDebut" label="Début" required>
          {({ field, control }) => <DatePicker {...control} value={field.value} onChange={field.onChange} />}
        </FormField>
        <FormField control={form.control} name="dateFinPrevue" label="Fin prévue" required>
          {({ field, control }) => (
            <DatePicker
              {...control}
              value={field.value}
              onChange={field.onChange}
              min={dateDebut ?? undefined}
            />
          )}
        </FormField>
      </div>
      <FormField
        control={form.control}
        name="besoinId"
        label="Besoin satisfait"
        optional
      >
        {({ field, control }) => (
          <Combobox
            {...control}
            options={besoins.data ?? []}
            value={field.value}
            onValueChange={field.onChange}
            disabled={!chantierId}
            loading={besoins.isFetching}
            placeholder="Aucun besoin rattaché"
            emptyText="Aucun besoin validé pour ce chantier"
          />
        )}
      </FormField>
      <AffectationAlert nonDisponible={nonDisponible} etat={etat} error={serverError} />
      <DialogFooter>
        <Button variant="secondary" onClick={onDone} disabled={affecter.isPending}>
          Annuler
        </Button>
        <Button type="submit" loading={affecter.isPending} disabled={nonDisponible || affecter.isPending}>
          Affecter
        </Button>
      </DialogFooter>
    </form>
  )
}
