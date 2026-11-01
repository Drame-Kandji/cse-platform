import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { optionsOf } from '@/shared/domain/labels'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { Select } from '@/shared/ui/select'
import { usePublierLigne } from '@/api/mutations'
import { InlineError } from '@/components/InlineError'
import { messageErreur } from '@/domain/errors'
import type { LigneCatalogue } from '@/domain/types'
import { ligneSchema, type LigneInput, type LigneOutput } from '../schemas/entreprise.schema'

const vide: LigneInput = {
  reference: '',
  designation: '',
  categorie: 'MATERIEL_ROULANT',
  prixUnitaire: '',
  unite: 'unité',
  delaiLivraisonJours: '',
}

const versFormulaire = (ligne: LigneCatalogue | null): LigneInput =>
  ligne
    ? {
        reference: ligne.reference,
        designation: ligne.designation,
        categorie: ligne.categorie,
        prixUnitaire: String(ligne.prixUnitaire),
        unite: ligne.unite,
        delaiLivraisonJours: String(ligne.delaiLivraisonJours),
      }
    : vide

/** Ajout ou modification d'une ligne (upsert par reference : la reference d'une ligne existante est figee). */
export function LigneDialog({
  open,
  ligne,
  onOpenChange,
}: {
  open: boolean
  /** Ligne a modifier ; `null` = nouvelle ligne. */
  ligne: LigneCatalogue | null
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation('portail')
  const { t: tc } = useTranslation()
  const [erreur, setErreur] = useState<string | null>(null)
  const publier = usePublierLigne()
  const form = useForm<LigneInput, unknown, LigneOutput>({
    resolver: zodResolver(ligneSchema),
    defaultValues: versFormulaire(ligne),
  })
  const { reset } = form
  useEffect(() => {
    if (open) {
      reset(versFormulaire(ligne))
      setErreur(null)
    }
  }, [open, ligne, reset])

  const submit = form.handleSubmit(async (values) => {
    try {
      await publier.mutateAsync(values)
      onOpenChange(false)
    } catch (error) {
      if (!applyServerErrors(error, form.setError)) setErreur(messageErreur(error))
    }
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={ligne ? t('catalogue.modifier') : t('catalogue.ajouter')}>
        <form onSubmit={(event) => void submit(event)} noValidate className="grid gap-4 sm:grid-cols-2">
          <InlineError message={erreur} className="sm:col-span-2" />
          <FormField
            control={form.control}
            name="reference"
            label={t('catalogue.columns.reference')}
            required
          >
            {({ field, control }) => <Input {...control} {...field} readOnly={ligne !== null} />}
          </FormField>
          <FormField
            control={form.control}
            name="categorie"
            label={t('catalogue.columns.categorie')}
            required
          >
            {({ field, control }) => (
              <Select
                {...control}
                options={optionsOf('categorieMateriel')}
                value={field.value}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="designation"
            label={t('catalogue.columns.designation')}
            required
            className="sm:col-span-2"
          >
            {({ field, control }) => <Input {...control} {...field} />}
          </FormField>
          <FormField control={form.control} name="prixUnitaire" label={t('catalogue.columns.prix')} required>
            {({ field, control }) => (
              <Input
                {...control}
                {...field}
                inputMode="numeric"
                rightSlot={<span className="pr-2 text-xs text-muted-foreground">FCFA</span>}
              />
            )}
          </FormField>
          <FormField control={form.control} name="unite" label={t('catalogue.unite')} required>
            {({ field, control }) => <Input {...control} {...field} />}
          </FormField>
          <FormField
            control={form.control}
            name="delaiLivraisonJours"
            label={t('catalogue.columns.delai')}
            required
          >
            {({ field, control }) => <Input {...control} {...field} inputMode="numeric" />}
          </FormField>
          <DialogFooter className="sm:col-span-2">
            <Button
              variant="secondary"
              onClick={() => {
                onOpenChange(false)
              }}
              disabled={publier.isPending}
            >
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" loading={publier.isPending}>
              {tc('actions.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
