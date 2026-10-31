import { useEffect, useState } from 'react'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { Field } from '@/shared/ui/field'
import { useReinitialiserMotDePasse } from '../hooks/useUtilisateurs'
import { generatePassword, PASSWORD_RULES } from '../lib/password'
import type { Utilisateur } from '../types'
import { PasswordInput } from './PasswordInput'

export interface ResetPasswordDialogProps {
  utilisateur: Utilisateur | null
  onOpenChange: (open: boolean) => void
}

/** Reinitialisation du mot de passe : mot de passe temporaire genere (modifiable) puis envoi. */
export function ResetPasswordDialog({ utilisateur, onOpenChange }: ResetPasswordDialogProps) {
  const [password, setPassword] = useState('')
  const [touched, setTouched] = useState(false)
  const mutation = useReinitialiserMotDePasse()
  useEffect(() => {
    if (utilisateur) {
      setPassword(generatePassword())
      setTouched(false)
    }
  }, [utilisateur])
  const failed = PASSWORD_RULES.find((rule) => !rule.test(password))
  return (
    <Dialog open={utilisateur !== null} onOpenChange={onOpenChange}>
      <DialogContent
        title="Réinitialiser le mot de passe"
        description={utilisateur ? `${utilisateur.prenom} ${utilisateur.nom}` : undefined}
      >
        <form
          id="reset-password-form"
          noValidate
          onSubmit={(event) => {
            event.preventDefault()
            setTouched(true)
            if (!utilisateur || failed) return
            mutation.mutate(
              { id: utilisateur.id, motDePasseTemporaire: password },
              {
                onSuccess: () => {
                  onOpenChange(false)
                },
              },
            )
          }}
        >
          <Field label="Mot de passe temporaire" required error={touched ? failed?.message : undefined}>
            {(control) => (
              <PasswordInput
                {...control}
                value={password}
                onChange={setPassword}
                onBlur={() => {
                  setTouched(true)
                }}
              />
            )}
          </Field>
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
          <Button type="submit" form="reset-password-form" loading={mutation.isPending}>
            Réinitialiser
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
