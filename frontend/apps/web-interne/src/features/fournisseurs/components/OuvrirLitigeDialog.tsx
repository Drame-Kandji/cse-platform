import { zodResolver } from '@hookform/resolvers/zod'
import { AlertOctagon } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { optionsOf } from '@/shared/domain/labels'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogTrigger } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import { useOuvrirLitige } from '../hooks/useFournisseurMutations'
import { litigeSchema, type LitigeForm } from '../schemas/fournisseur.schema'

/** Ouverture d'un litige avec un fournisseur (RA, RM). */
export function OuvrirLitigeDialog({ fournisseurId }: { fournisseurId: string }) {
  const [open, setOpen] = useState(false)
  const ouvrir = useOuvrirLitige(fournisseurId)
  const form = useForm<LitigeForm>({
    resolver: zodResolver(litigeSchema),
    defaultValues: { objet: '', description: '', gravite: 'MOYENNE' },
  })
  const submit = form.handleSubmit(async (values) => {
    await ouvrir.mutateAsync(values)
    form.reset()
    setOpen(false)
  })
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="secondary" leftIcon={<AlertOctagon />}>
          Ouvrir un litige
        </Button>
      </DialogTrigger>
      <DialogContent title="Ouvrir un litige">
        <form
          noValidate
          className="grid gap-4"
          onSubmit={(event) => {
            void submit(event).catch(() => undefined)
          }}
        >
          <FormField control={form.control} name="objet" label="Objet" required>
            {({ field, control }) => <Input {...control} {...field} />}
          </FormField>
          <FormField control={form.control} name="description" label="Description" required>
            {({ field, control }) => <Textarea {...control} {...field} />}
          </FormField>
          <FormField control={form.control} name="gravite" label="Gravité" required>
            {({ field }) => (
              <RadioGroup
                aria-label="Gravité"
                variant="card"
                orientation="horizontal"
                options={optionsOf('graviteLitige')}
                value={field.value}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <DialogFooter>
            <Button
              variant="secondary"
              onClick={() => {
                setOpen(false)
              }}
            >
              Annuler
            </Button>
            <Button type="submit" loading={ouvrir.isPending}>
              Ouvrir le litige
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
