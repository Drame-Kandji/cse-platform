import { zodResolver } from '@hookform/resolvers/zod'
import { XCircle } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { z } from 'zod'
import { isApiError } from '@/shared/api'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import type { Besoin } from '../api/types'
import { useRejeterBesoin } from '../hooks/useBesoinMutations'
import { rejetSchema } from '../schemas/besoin.schema'

interface Props {
  besoin: Pick<Besoin, 'id' | 'reference'>
  open: boolean
  onOpenChange: (open: boolean) => void
}

/** Rejet d'un besoin : motif obligatoire, decision definitive (action destructive). */
export function RejeterDialog({ besoin, open, onOpenChange }: Props) {
  const { t } = useTranslation('besoins')
  const rejeter = useRejeterBesoin()
  const form = useForm<z.input<typeof rejetSchema>, unknown, z.output<typeof rejetSchema>>({
    resolver: zodResolver(rejetSchema),
    defaultValues: { motif: '' },
  })
  const close = (next: boolean) => {
    if (rejeter.isPending) return
    if (!next) form.reset()
    onOpenChange(next)
  }
  const submit = form.handleSubmit((values) => {
    rejeter.mutate(
      { id: besoin.id, motif: values.motif },
      {
        onSuccess: () => {
          close(false)
        },
        onError: (error) => {
          if (applyServerErrors(error, form.setError)) return
          if (isApiError(error) && error.code === 'MOTIF_REJET_OBLIGATOIRE') {
            form.setError('motif', { type: 'server', message: error.userMessage }, { shouldFocus: true })
          }
        },
      },
    )
  })
  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent
        title={t('rejeter.title', { reference: besoin.reference })}
        description={t('rejeter.description')}
      >
        <form id={`rejet-${besoin.id}`} onSubmit={(event) => void submit(event)} noValidate>
          <FormField control={form.control} name="motif" label={t('rejeter.motif')} required>
            {({ field, control }) => (
              <Textarea {...control} {...field} rows={3} placeholder={t('rejeter.motifPlaceholder')} />
            )}
          </FormField>
        </form>
        <DialogFooter>
          <Button
            variant="secondary"
            disabled={rejeter.isPending}
            onClick={() => {
              close(false)
            }}
          >
            {t('form.cancel')}
          </Button>
          <Button
            type="submit"
            form={`rejet-${besoin.id}`}
            variant="danger"
            loading={rejeter.isPending}
            leftIcon={<XCircle />}
          >
            {t('rejeter.confirmer')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
