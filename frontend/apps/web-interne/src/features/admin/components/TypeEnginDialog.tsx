import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { getErrorMessage } from '@/shared/api/errors'
import { optionsOf } from '@/shared/domain/labels'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { Select } from '@/shared/ui/select'
import { Switch } from '@/shared/ui/switch'
import { useEnregistrerTypeEngin } from '../hooks/useTypesEngins'
import {
  typeEnginSchema,
  type TypeEnginFormInput,
  type TypeEnginFormOutput,
} from '../schemas/type-engin.schema'
import type { TypeEngin } from '../types'

export interface TypeEnginDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  typeEngin?: TypeEngin | null
}

function defaults(typeEngin?: TypeEngin | null): TypeEnginFormInput {
  return {
    code: typeEngin?.code ?? '',
    libelle: typeEngin?.libelle ?? '',
    categorie: typeEngin?.categorie ?? 'MATERIEL_ROULANT',
    seuilMaintenanceHeures:
      typeEngin?.seuilMaintenanceHeures != null ? String(typeEngin.seuilMaintenanceHeures) : '',
    seuilMaintenanceJours:
      typeEngin?.seuilMaintenanceJours != null ? String(typeEngin.seuilMaintenanceJours) : '',
    actif: typeEngin?.actif ?? true,
  }
}

/** Creation / modification d'un type d'engin et de ses seuils de maintenance preventive. */
export function TypeEnginDialog({ open, onOpenChange, typeEngin }: TypeEnginDialogProps) {
  const form = useForm<TypeEnginFormInput, unknown, TypeEnginFormOutput>({
    resolver: zodResolver(typeEnginSchema),
    defaultValues: defaults(typeEngin),
  })
  const mutation = useEnregistrerTypeEngin()
  useEffect(() => {
    if (open) form.reset(defaults(typeEngin))
  }, [open, typeEngin, form])

  const submit = form.handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync({ ...values, ...(typeEngin ? { id: typeEngin.id } : {}) })
      onOpenChange(false)
    } catch (error) {
      if (!applyServerErrors(error, form.setError)) form.setError('root', { message: getErrorMessage(error) })
    }
  })
  const rootError = form.formState.errors.root?.message

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        title={typeEngin ? `Modifier ${typeEngin.libelle}` : "Nouveau type d'engin"}
        description="Les seuils déclenchent les alertes de maintenance préventive (heures de fonctionnement ou jours écoulés)."
      >
        <form id="type-engin-form" className="grid gap-4" noValidate onSubmit={(event) => void submit(event)}>
          <div className="grid gap-4 sm:grid-cols-[10rem_minmax(0,1fr)]">
            <FormField control={form.control} name="code" label="Code" required>
              {({ field, control }) => (
                <Input {...control} {...field} className="font-mono uppercase" placeholder="PELLE-HYD" />
              )}
            </FormField>
            <FormField control={form.control} name="libelle" label="Libellé" required>
              {({ field, control }) => <Input {...control} {...field} placeholder="Pelle hydraulique" />}
            </FormField>
          </div>
          <FormField control={form.control} name="categorie" label="Catégorie" required>
            {({ field, control }) => (
              <Select
                {...control}
                options={optionsOf('categorieMateriel')}
                value={field.value}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="seuilMaintenanceHeures"
              label="Seuil (heures)"
              optional
              hint="Ex. 250 h de fonctionnement"
            >
              {({ field, control }) => (
                <Input
                  {...control}
                  {...field}
                  inputMode="numeric"
                  rightSlot={<span className="pr-2 text-xs text-muted-foreground">h</span>}
                />
              )}
            </FormField>
            <FormField
              control={form.control}
              name="seuilMaintenanceJours"
              label="Seuil (jours)"
              optional
              hint="Ex. 180 jours entre deux révisions"
            >
              {({ field, control }) => (
                <Input
                  {...control}
                  {...field}
                  inputMode="numeric"
                  rightSlot={<span className="pr-2 text-xs text-muted-foreground">j</span>}
                />
              )}
            </FormField>
          </div>
          <FormField control={form.control} name="actif" label="Disponibilité">
            {({ field, control }) => (
              <Switch
                id={control.id}
                checked={field.value}
                onCheckedChange={field.onChange}
                label="Type actif"
                description="Un type inactif n'est plus proposé à l'enregistrement de matériels."
              />
            )}
          </FormField>
          {rootError && (
            <p role="alert" className="rounded-lg bg-danger/10 px-3 py-2 text-sm text-danger-emphasis">
              {rootError}
            </p>
          )}
        </form>
        <DialogFooter>
          <Button
            variant="secondary"
            onClick={() => {
              onOpenChange(false)
            }}
          >
            Annuler
          </Button>
          <Button type="submit" form="type-engin-form" loading={form.formState.isSubmitting}>
            Enregistrer
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
