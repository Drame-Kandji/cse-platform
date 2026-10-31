import { zodResolver } from '@hookform/resolvers/zod'
import { Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { optionsOf, type CategorieMateriel } from '@/shared/domain/labels'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogTrigger } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { Select } from '@/shared/ui/select'
import { useEnregistrerLigneCatalogue } from '../hooks/useFournisseurMutations'
import {
  ligneCatalogueSchema,
  type LigneCatalogueFormInput,
  type LigneCatalogueFormOutput,
} from '../schemas/fournisseur.schema'

const TEXT = [
  { name: 'reference', label: 'Référence' },
  { name: 'designation', label: 'Désignation' },
  { name: 'prixUnitaire', label: 'Prix unitaire (FCFA)', numeric: true },
  { name: 'unite', label: 'Unité' },
  { name: 'delaiLivraisonJours', label: 'Délai de livraison (jours)', numeric: true },
] as const

/** Ajout ou mise a jour (meme reference) d'un article du catalogue ; le prix est historise. */
export function LigneCatalogueDialog({
  fournisseurId,
  specialites,
}: {
  fournisseurId: string
  /** La categorie d'un article doit etre une specialite du fournisseur (422 sinon). */
  specialites: readonly CategorieMateriel[]
}) {
  const schema = useMemo(
    () =>
      ligneCatalogueSchema.refine((value) => specialites.includes(value.categorie), {
        message: 'La catégorie doit être une spécialité du fournisseur',
        path: ['categorie'],
      }),
    [specialites],
  )
  const options = optionsOf('categorieMateriel').filter((option) => specialites.includes(option.value))
  const [open, setOpen] = useState(false)
  const enregistrer = useEnregistrerLigneCatalogue(fournisseurId)
  const form = useForm<LigneCatalogueFormInput, unknown, LigneCatalogueFormOutput>({
    resolver: zodResolver(schema),
    defaultValues: {
      reference: '',
      designation: '',
      unite: 'unité',
      prixUnitaire: Number.NaN,
      delaiLivraisonJours: 0,
    },
  })
  const submit = form.handleSubmit(async (values) => {
    await enregistrer.mutateAsync(values)
    form.reset()
    setOpen(false)
  })
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="secondary" size="sm" leftIcon={<Plus />}>
          Ajouter un article
        </Button>
      </DialogTrigger>
      <DialogContent
        title="Article du catalogue"
        description="Une référence existante est mise à jour et son ancien prix historisé."
      >
        <form
          noValidate
          className="grid gap-4 sm:grid-cols-2"
          onSubmit={(event) => {
            void submit(event).catch(() => undefined)
          }}
        >
          {TEXT.map((item) => (
            <FormField key={item.name} control={form.control} name={item.name} label={item.label} required>
              {({ field, control }) => (
                <Input
                  {...control}
                  {...field}
                  value={Number.isNaN(field.value) ? '' : String(field.value)}
                  inputMode={'numeric' in item ? 'numeric' : undefined}
                />
              )}
            </FormField>
          ))}
          <FormField control={form.control} name="categorie" label="Catégorie" required>
            {({ field, control }) => (
              <Select {...control} options={options} value={field.value} onValueChange={field.onChange} />
            )}
          </FormField>
          <DialogFooter className="sm:col-span-2">
            <Button
              variant="secondary"
              onClick={() => {
                setOpen(false)
              }}
            >
              Annuler
            </Button>
            <Button type="submit" loading={enregistrer.isPending}>
              Enregistrer
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
