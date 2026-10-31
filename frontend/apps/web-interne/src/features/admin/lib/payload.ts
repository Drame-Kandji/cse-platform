import type { Role } from '@/shared/domain/labels'
import type { UtilisateurFormOutput } from '../schemas/utilisateur.schema'
import type { UtilisateurModification } from '../types'

/** Attribut metier propre a chaque role (les autres sont ignores a l'envoi). */
export const ROLE_ATTRIBUTE: Readonly<
  Partial<Record<Role, 'chantierIds' | 'seuilDelegation' | 'budgetGere' | 'perimetreParc'>>
> = {
  CHEF_CHANTIER: 'chantierIds',
  RESPONSABLE_ACHATS: 'seuilDelegation',
  DAF: 'budgetGere',
  RESPONSABLE_MATERIEL: 'perimetreParc',
}

/** Corps API d'un compte : seuls les attributs du role choisi sont transmis. */
export function buildUtilisateurPayload(values: UtilisateurFormOutput): UtilisateurModification {
  const attribute = ROLE_ATTRIBUTE[values.role]
  return {
    email: values.email,
    prenom: values.prenom,
    nom: values.nom,
    ...(values.telephone ? { telephone: values.telephone } : {}),
    role: values.role,
    ...(attribute === 'chantierIds' ? { chantierIds: values.chantierIds } : {}),
    ...(attribute === 'seuilDelegation' && values.seuilDelegation !== undefined
      ? { seuilDelegation: values.seuilDelegation }
      : {}),
    ...(attribute === 'budgetGere' && values.budgetGere !== undefined
      ? { budgetGere: values.budgetGere }
      : {}),
    ...(attribute === 'perimetreParc' && values.perimetreParc ? { perimetreParc: values.perimetreParc } : {}),
  }
}
