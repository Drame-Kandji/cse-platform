import { Copy, Eye, EyeOff, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import type { FieldControlProps } from '@/shared/ui/field'
import { IconButton } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { generatePassword } from '../lib/password'

export interface PasswordInputProps extends Partial<FieldControlProps> {
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  name?: string
}

/** Saisie d'un mot de passe temporaire : generateur, affichage et copie. */
export function PasswordInput({ value, onChange, onBlur, name, ...control }: PasswordInputProps) {
  const [visible, setVisible] = useState(false)
  return (
    <div className="flex gap-1.5">
      <Input
        {...control}
        name={name}
        type={visible ? 'text' : 'password'}
        autoComplete="new-password"
        spellCheck={false}
        value={value}
        onChange={(event) => {
          onChange(event.target.value)
        }}
        onBlur={onBlur}
        className="font-mono"
      />
      <IconButton
        variant="secondary"
        label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
        icon={visible ? <EyeOff /> : <Eye />}
        onClick={() => {
          setVisible((current) => !current)
        }}
      />
      <IconButton
        variant="secondary"
        label="Générer un mot de passe"
        icon={<RefreshCw />}
        onClick={() => {
          onChange(generatePassword())
          setVisible(true)
        }}
      />
      <IconButton
        variant="secondary"
        label="Copier le mot de passe"
        icon={<Copy />}
        disabled={!value}
        onClick={() => {
          void navigator.clipboard.writeText(value).then(
            () => toast.success('Mot de passe copié'),
            () => toast.error('Copie impossible : sélectionnez le texte manuellement'),
          )
        }}
      />
    </div>
  )
}
