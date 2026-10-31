import { zodResolver } from '@hookform/resolvers/zod'
import { FilePlus2 } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { optionsOf } from '@/shared/domain/labels'
import { toIsoDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DatePicker } from '@/shared/ui/date-picker'
import { Dialog, DialogContent, DialogFooter, DialogTrigger } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { Select } from '@/shared/ui/select'
import { useAjouterDocument } from '../hooks/useFournisseurMutations'
import { documentSchema, type DocumentForm } from '../schemas/fournisseur.schema'

/** Ajout d'un document administratif (NINEA, RCCM, quitus...) par le RA. */
export function AddDocumentDialog({ fournisseurId }: { fournisseurId: string }) {
  const [open, setOpen] = useState(false)
  const ajouter = useAjouterDocument(fournisseurId)
  const form = useForm<DocumentForm>({
    resolver: zodResolver(documentSchema),
    defaultValues: { libelle: '', dateExpiration: null },
  })
  const submit = form.handleSubmit(async (values) => {
    await ajouter.mutateAsync({
      type: values.type,
      libelle: values.libelle,
      dateExpiration: toIsoDate(values.dateExpiration),
    })
    form.reset()
    setOpen(false)
  })
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="secondary" size="sm" leftIcon={<FilePlus2 />}>
          Ajouter un document
        </Button>
      </DialogTrigger>
      <DialogContent title="Ajouter un document administratif">
        <form
          noValidate
          className="grid gap-4"
          onSubmit={(event) => {
            void submit(event).catch(() => undefined)
          }}
        >
          <FormField control={form.control} name="type" label="Type" required>
            {({ field, control }) => (
              <Select
                {...control}
                options={optionsOf('typeDocument')}
                value={field.value}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <FormField control={form.control} name="libelle" label="Libellé" required>
            {({ field, control }) => <Input {...control} {...field} />}
          </FormField>
          <FormField control={form.control} name="dateExpiration" label="Date d'expiration" optional>
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} />
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
            <Button type="submit" loading={ajouter.isPending}>
              Ajouter
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
