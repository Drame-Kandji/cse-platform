import { zodResolver } from '@hookform/resolvers/zod'
import { Star } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { formatNumber } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogTrigger } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { useEvaluerFournisseur } from '../hooks/useFournisseurMutations'
import { CRITERES_EVALUATION, evaluationSchema, type EvaluationForm } from '../schemas/evaluation.schema'

const DEFAULTS: EvaluationForm = { qualite: 5, delais: 5, conformite: 5, sav: 5, prix: 5, commentaire: '' }

/** Moyenne indicative /100 des cinq criteres (le score officiel est calcule par le serveur, R07). */
function apercu(values: EvaluationForm): number {
  const notes = CRITERES_EVALUATION.map(({ name }) => values[name])
  const valid = notes.filter((note) => Number.isFinite(note))
  return valid.length ? (valid.reduce((sum, note) => sum + note, 0) / valid.length) * 10 : 0
}

/** Dialogue "Evaluer" : cinq criteres notes de 0 a 10 (curseur + saisie numerique). */
export function EvaluerDialog({ fournisseurId }: { fournisseurId: string }) {
  const { t } = useTranslation('fournisseurs')
  const [open, setOpen] = useState(false)
  const evaluer = useEvaluerFournisseur(fournisseurId)
  const form = useForm<EvaluationForm>({ resolver: zodResolver(evaluationSchema), defaultValues: DEFAULTS })
  const values = form.watch()
  const submit = form.handleSubmit(async (data) => {
    await evaluer.mutateAsync({ ...data, commentaire: data.commentaire || undefined })
    form.reset(DEFAULTS)
    setOpen(false)
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button leftIcon={<Star />}>{t('evaluations.evaluer')}</Button>
      </DialogTrigger>
      <DialogContent title={t('evaluations.dialogTitle')} description={t('evaluations.dialogDescription')}>
        <form
          noValidate
          className="grid gap-4"
          onSubmit={(event) => {
            void submit(event).catch(() => undefined)
          }}
        >
          {CRITERES_EVALUATION.map(({ name, label }) => (
            <FormField key={name} control={form.control} name={name} label={label} required>
              {({ field, control }) => (
                <div className="flex items-center gap-3">
                  <input
                    type="range"
                    min={0}
                    max={10}
                    step={1}
                    aria-label={`${label} (curseur)`}
                    value={Number.isFinite(field.value) ? field.value : 0}
                    onChange={(event) => {
                      field.onChange(event.target.valueAsNumber)
                    }}
                    className="h-2 flex-1 cursor-pointer accent-primary"
                  />
                  <Input
                    {...control}
                    ref={field.ref}
                    name={field.name}
                    type="number"
                    inputMode="numeric"
                    min={0}
                    max={10}
                    step={1}
                    className="w-20 text-right tabular-nums"
                    value={Number.isFinite(field.value) ? field.value : ''}
                    onBlur={field.onBlur}
                    onChange={(event) => {
                      field.onChange(event.target.value === '' ? Number.NaN : event.target.valueAsNumber)
                    }}
                  />
                </div>
              )}
            </FormField>
          ))}
          <FormField control={form.control} name="commentaire" label={t('evaluations.commentaire')} optional>
            {({ field, control }) => <Textarea {...control} {...field} value={field.value ?? ''} rows={3} />}
          </FormField>
          <p className="rounded-lg bg-surface-sunken px-3 py-2 text-sm text-muted-foreground">
            Note indicative :{' '}
            <span className="font-semibold tabular-nums text-foreground">
              {formatNumber(apercu(values), { decimals: 1 })} / 100
            </span>
          </p>
          <DialogFooter>
            <Button
              variant="secondary"
              onClick={() => {
                setOpen(false)
              }}
            >
              Annuler
            </Button>
            <Button type="submit" loading={evaluer.isPending}>
              Enregistrer l'évaluation
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
