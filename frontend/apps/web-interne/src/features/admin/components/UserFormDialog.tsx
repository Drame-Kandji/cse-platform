import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { applyServerErrors } from '@/shared/lib/forms'
import { getErrorMessage } from '@/shared/api/errors'
import { isRole } from '@/shared/auth/claims'
import { ROLE_LABELS, ROLES } from '@/shared/domain/labels'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import { useCreerUtilisateur, useModifierUtilisateur } from '../hooks/useUtilisateurs'
import { generatePassword } from '../lib/password'
import { buildUtilisateurPayload } from '../lib/payload'
import {
  utilisateurSchema,
  type UtilisateurFormInput,
  type UtilisateurFormOutput,
} from '../schemas/utilisateur.schema'
import type { Utilisateur } from '../types'
import { ChantiersPicker } from './ChantiersPicker'
import { PasswordInput } from './PasswordInput'

export interface UserFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Compte a modifier ; absent = creation. */
  utilisateur?: Utilisateur | null
}

function defaults(utilisateur?: Utilisateur | null): UtilisateurFormInput {
  return {
    prenom: utilisateur?.prenom ?? '',
    nom: utilisateur?.nom ?? '',
    email: utilisateur?.email ?? '',
    telephone: utilisateur?.telephone ?? '',
    role: utilisateur && isRole(utilisateur.role) ? utilisateur.role : 'CHEF_CHANTIER',
    chantierIds: [...(utilisateur?.chantierIds ?? [])],
    perimetreParc: utilisateur?.perimetreParc ?? '',
    seuilDelegation: utilisateur?.seuilDelegation != null ? String(utilisateur.seuilDelegation) : '',
    budgetGere: utilisateur?.budgetGere != null ? String(utilisateur.budgetGere) : '',
    motDePasseTemporaire: utilisateur ? '' : generatePassword(),
  }
}

const ROLE_OPTIONS = ROLES.map((role) => ({
  value: role,
  label: ROLE_LABELS[role].label,
}))

/**
 * Creation / modification d'un compte. Les champs metier dependent du role :
 * chef de chantier -> chantiers ; achats -> seuil de delegation ; DAF -> budget gere ; materiel -> perimetre.
 */
export function UserFormDialog({ open, onOpenChange, utilisateur }: UserFormDialogProps) {
  const creation = !utilisateur
  const form = useForm<UtilisateurFormInput, unknown, UtilisateurFormOutput>({
    resolver: zodResolver(utilisateurSchema(creation)),
    defaultValues: defaults(utilisateur),
  })
  const creer = useCreerUtilisateur()
  const modifier = useModifierUtilisateur()
  const role = form.watch('role')

  useEffect(() => {
    if (open) form.reset(defaults(utilisateur))
  }, [open, utilisateur, form])

  const submit = form.handleSubmit(async (values) => {
    const payload = buildUtilisateurPayload(values)
    try {
      if (utilisateur) await modifier.mutateAsync({ id: utilisateur.id, ...payload })
      else await creer.mutateAsync({ ...payload, motDePasseTemporaire: values.motDePasseTemporaire })
      onOpenChange(false)
    } catch (error) {
      if (!applyServerErrors(error, form.setError)) {
        form.setError('root', { message: getErrorMessage(error) })
      }
    }
  })

  const rootError = form.formState.errors.root?.message

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        size="lg"
        title={creation ? 'Nouvel utilisateur' : `Modifier ${utilisateur.prenom} ${utilisateur.nom}`}
      >
        <form
          id="utilisateur-form"
          className="grid gap-5"
          noValidate
          onSubmit={(event) => void submit(event)}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField control={form.control} name="prenom" label="Prénom" required>
              {({ field, control }) => <Input {...control} {...field} autoComplete="given-name" />}
            </FormField>
            <FormField control={form.control} name="nom" label="Nom" required>
              {({ field, control }) => <Input {...control} {...field} autoComplete="family-name" />}
            </FormField>
            <FormField control={form.control} name="email" label="Adresse e-mail" required>
              {({ field, control }) => <Input {...control} {...field} type="email" autoComplete="email" />}
            </FormField>
            <FormField control={form.control} name="telephone" label="Téléphone" optional>
              {({ field, control }) => (
                <Input {...control} {...field} type="tel" placeholder="+221 77 000 00 00" />
              )}
            </FormField>
          </div>

          <FormField control={form.control} name="role" label="Rôle" required>
            {({ field, control }) => (
              <RadioGroup
                aria-describedby={control['aria-describedby']}
                aria-label="Rôle"
                variant="card"
                className="sm:grid-cols-2"
                options={ROLE_OPTIONS}
                value={field.value}
                onValueChange={field.onChange}
              />
            )}
          </FormField>

          {role === 'CHEF_CHANTIER' && (
            <FormField control={form.control} name="chantierIds" label="Chantiers rattachés" required>
              {({ field, control }) => (
                <ChantiersPicker {...control} value={field.value} onChange={field.onChange} />
              )}
            </FormField>
          )}
          {role === 'RESPONSABLE_ACHATS' && (
            <FormField
              control={form.control}
              name="seuilDelegation"
              label="Seuil de délégation (FCFA)"
              required
              hint="Au-delà : approbation de la Direction Générale (R11)"
            >
              {({ field, control }) => (
                <Input {...control} {...field} inputMode="numeric" placeholder="150000000" />
              )}
            </FormField>
          )}
          {role === 'DAF' && (
            <FormField control={form.control} name="budgetGere" label="Budget géré (FCFA)" optional>
              {({ field, control }) => (
                <Input {...control} {...field} inputMode="numeric" placeholder="5000000000" />
              )}
            </FormField>
          )}
          {role === 'RESPONSABLE_MATERIEL' && (
            <FormField control={form.control} name="perimetreParc" label="Périmètre du parc" optional>
              {({ field, control }) => <Input {...control} {...field} placeholder="Dakar, Thiès" />}
            </FormField>
          )}

          {creation && (
            <FormField
              control={form.control}
              name="motDePasseTemporaire"
              label="Mot de passe temporaire"
              required
              hint="À changer à la première connexion"
            >
              {({ field, control }) => (
                <PasswordInput
                  {...control}
                  name={field.name}
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                />
              )}
            </FormField>
          )}

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
          <Button type="submit" form="utilisateur-form" loading={form.formState.isSubmitting}>
            {creation ? "Créer l'utilisateur" : 'Enregistrer'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
