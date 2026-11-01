import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { useDeposerOffre } from '@/api/mutations'
import { InlineError } from '@/components/InlineError'
import { messageErreur } from '@/domain/errors'
import type { AppelOffrePortail } from '@/domain/types'
import { OFFRE_VIDE, offreSchema, type OffreInput, type OffreOutput } from '../schemas/offre.schema'

/** Depot d'une offre : montant FCFA, delai, garantie, description. Erreurs metier affichees en ligne. */
export function DeposerOffreDialog({
  appelOffre,
  open,
  onOpenChange,
}: {
  appelOffre: Pick<AppelOffrePortail, 'id' | 'reference' | 'objet'>
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation('portail')
  const { t: tc } = useTranslation()
  const [erreur, setErreur] = useState<string | null>(null)
  const deposer = useDeposerOffre(appelOffre.id)
  const form = useForm<OffreInput, unknown, OffreOutput>({
    resolver: zodResolver(offreSchema),
    defaultValues: OFFRE_VIDE,
  })

  const close = (next: boolean) => {
    if (!next) {
      form.reset(OFFRE_VIDE)
      setErreur(null)
    }
    onOpenChange(next)
  }

  const submit = form.handleSubmit(async (values) => {
    setErreur(null)
    try {
      await deposer.mutateAsync(values)
      close(false)
    } catch (error) {
      if (!applyServerErrors(error, form.setError))
        setErreur(messageErreur(error, { conflit: t('ao.dialog.conflit') }))
    }
  })

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent
        title={t('ao.dialog.title')}
        description={`${appelOffre.reference} · ${appelOffre.objet}`}
      >
        <form onSubmit={(event) => void submit(event)} noValidate className="grid gap-4 sm:grid-cols-2">
          <InlineError message={erreur} className="sm:col-span-2" />
          <FormField
            control={form.control}
            name="montantPropose"
            label={t('ao.dialog.montant')}
            required
            className="sm:col-span-2"
          >
            {({ field, control }) => (
              <Input
                {...control}
                {...field}
                inputMode="numeric"
                rightSlot={<span className="pr-2 text-xs text-muted-foreground">FCFA</span>}
              />
            )}
          </FormField>
          <FormField control={form.control} name="delaiLivraisonJours" label={t('ao.dialog.delai')} required>
            {({ field, control }) => <Input {...control} {...field} inputMode="numeric" />}
          </FormField>
          <FormField control={form.control} name="garantieMois" label={t('ao.dialog.garantie')} optional>
            {({ field, control }) => <Input {...control} {...field} inputMode="numeric" />}
          </FormField>
          <FormField
            control={form.control}
            name="description"
            label={t('ao.dialog.description')}
            optional
            className="sm:col-span-2"
          >
            {({ field, control }) => <Textarea {...control} {...field} rows={4} />}
          </FormField>
          <DialogFooter className="sm:col-span-2">
            <Button
              variant="secondary"
              onClick={() => {
                close(false)
              }}
              disabled={deposer.isPending}
            >
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" loading={deposer.isPending}>
              {t('ao.dialog.submit')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
