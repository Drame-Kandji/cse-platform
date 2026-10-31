import { Select } from '@/shared/ui/select'
import { PROFIL_LABELS } from '../lib/profils'
import type { Profil } from '../types'

export interface ProfilSwitcherProps {
  /** Profils autorises (seuls proposes). */
  profils: readonly Profil[]
  value: Profil
  onChange: (profil: Profil) => void
}

/** Choix de la vue (profil) du tableau de bord ; masque s'il n'y a qu'un profil autorise. */
export function ProfilSwitcher({ profils, value, onChange }: ProfilSwitcherProps) {
  if (profils.length < 2) return null
  return (
    <Select
      aria-label="Vue du tableau de bord"
      className="w-auto min-w-52"
      value={value}
      onValueChange={onChange}
      options={profils.map((profil) => ({
        value: profil,
        label: `Vue ${PROFIL_LABELS[profil].label}`,
      }))}
    />
  )
}
