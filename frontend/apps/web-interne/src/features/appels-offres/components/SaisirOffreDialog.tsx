import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { getErrorMessage, isApiError } from '@/shared/api'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { Select } from '@/shared/ui/select'
import type { AppelOffre } from '../api/types'
import { useSaisirOffre } from '../hooks/useAppelOffreActions'
import { useFournisseursInvitables } from '../hooks/useAppelsOffres'
import { offreSaisieSchema, type OffreSaisieInput } from '../schemas/appel-offre.schema'
import { NumberInput } from './NumberInput'

interface Props {
  ao: AppelOffre
  open: boolean
  onOpenChange: (open: boolean) => void
}

/** Refus metier rattaches au champ fournisseur (contrat §5, R02/R14). */
const ERREURS_FOURNISSEUR: Readonly<Record<string, string>> = {
  OFFRE_DEJA_DEPOSEE: 'Ce fournisseur a déjà déposé une offre sur cet appel d’offres.',
  FOURNISSEUR_NON_INVITE: 'Seuls les fournisseurs invités peuvent répondre à une demande de prix.',
  FOURNISSEUR_NON_ELIGIBLE: 'Fournisseur suspendu ou blacklisté : offre refusée (R14).',
}

/**
 * Saisie par le RA d'une offre recue hors portail. Demande de prix : invites uniquement ;
 * appel d'offres : tout fournisseur actif de la specialite (R14).
 */
export function SaisirOffreDialog({ ao, open, onOpenChange }: Props) {
  const saisir = useSaisirOffre(ao.id)
  const ouvert = ao.type === 'APPEL_OFFRES'
  const actifs = useFournisseursInvitables(open && ouvert ? ao.categorie : undefined)
  const invites = new Set(ao.fournisseursInvites.map((f) => f.id))
  const options = [
    ...ao.fournisseursInvites.map((f) => ({ value: f.id, label: `${f.raisonSociale} (invité)` })),
    ...(ouvert ? (actifs.data?.content ?? []) : [])
      .filter((f) => !invites.has(f.id))
      .map((f) => ({ value: f.id, label: f.raisonSociale })),
  ]
  const form = useForm<OffreSaisieInput>({
    resolver: zodResolver(offreSaisieSchema),
    defaultValues: {
      fournisseurId: '',
      montantPropose: undefined,
      delaiLivraisonJours: undefined,
      description: '',
    },
  })
  const fermer = (next: boolean) => {
    if (!next) form.reset()
    onOpenChange(next)
  }
  const submit = form.handleSubmit(async (values) => {
    try {
      await saisir.mutateAsync({
        fournisseurId: values.fournisseurId,
        montantPropose: values.montantPropose,
        delaiLivraisonJours: values.delaiLivraisonJours,
        garantieMois: values.garantieMois,
        description: values.description?.trim() || undefined,
      })
      fermer(false)
    } catch (error) {
      if (applyServerErrors(error, form.setError)) return
      const message = isApiError(error) && error.code ? ERREURS_FOURNISSEUR[error.code] : undefined
      if (message) form.setError('fournisseurId', { type: 'server', message })
      else toast.error(getErrorMessage(error))
    }
  })
  return (
    <Dialog open={open} onOpenChange={fermer}>
      <DialogContent
        title="Saisir une offre reçue"
        description={`Offre transmise hors portail${ouvert ? '' : ' par un fournisseur invité'}. Elle restera anonymisée jusqu'à la clôture.`}
      >
        <form noValidate onSubmit={(event) => void submit(event)} className="grid gap-4">
          <FormField
            control={form.control}
            name="fournisseurId"
            label="Fournisseur"
            required
            hint={
              ouvert
                ? 'Fournisseurs actifs de la spécialité ; les invités sont signalés.'
                : 'Demande de prix : fournisseurs invités uniquement.'
            }
          >
            {({ field, control }) => (
              <Select
                {...control}
                options={options}
                value={field.value || undefined}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <div className="grid gap-4 sm:grid-cols-3">
            <FormField control={form.control} name="montantPropose" label="Montant (FCFA)" required>
              {({ field, control }) => (
                <NumberInput {...control} min={0} value={field.value} onChange={field.onChange} />
              )}
            </FormField>
            <FormField control={form.control} name="delaiLivraisonJours" label="Délai (jours)" required>
              {({ field, control }) => (
                <NumberInput {...control} min={1} value={field.value} onChange={field.onChange} />
              )}
            </FormField>
            <FormField control={form.control} name="garantieMois" label="Garantie (mois)" optional>
              {({ field, control }) => (
                <NumberInput {...control} min={0} value={field.value} onChange={field.onChange} />
              )}
            </FormField>
          </div>
          <FormField control={form.control} name="description" label="Description" optional>
            {({ field, control }) => <Textarea {...control} {...field} value={field.value ?? ''} rows={3} />}
          </FormField>
          <DialogFooter>
            <Button
              variant="secondary"
              onClick={() => {
                fermer(false)
              }}
            >
              Annuler
            </Button>
            <Button type="submit" loading={saisir.isPending}>
              Enregistrer l&apos;offre
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
