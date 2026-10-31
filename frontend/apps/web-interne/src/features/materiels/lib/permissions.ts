import type { Role } from '@/shared/domain/labels'
import type { Affectation, MaterielDetail } from '@/shared/domain/types'

/** Utilisateur reduit aux informations utiles aux droits. */
export interface ActeurParc {
  readonly roles: readonly Role[]
  readonly chantierIds: readonly string[]
}

export interface MaterielPermissions {
  readonly modifier: boolean
  /** Bouton visible (RM) ; voir `affecterBloque` pour la desactivation (R04). */
  readonly affecter: boolean
  /** Motif de blocage de l'affectation (materiel non DISPONIBLE), sinon null. */
  readonly affecterBloque: string | null
  readonly confirmerReception: boolean
  readonly liberer: boolean
  readonly releverHeures: boolean
  readonly changerEtat: boolean
  readonly demanderReforme: boolean
  readonly validerReforme: boolean
  readonly signalerPanne: boolean
}

/** Affectation en attente de reception (au plus une a la fois). */
export function affectationEnAttente(
  materiel: Pick<MaterielDetail, 'affectations'>,
): Affectation | undefined {
  return materiel.affectations.find((affectation) => affectation.statut === 'EN_ATTENTE_RECEPTION')
}

/** Affectation en cours (materiel sur chantier). */
export function affectationEnCours(materiel: Pick<MaterielDetail, 'affectations'>): Affectation | undefined {
  return materiel.affectations.find((affectation) => affectation.statut === 'EN_COURS')
}

/**
 * Actions autorisees sur une fiche engin selon le role et l'etat (ergonomie ; l'API fait foi).
 * - RM : cycle de vie complet (affecter si DISPONIBLE (R04), liberer, etat, demande de reforme) ;
 * - CHEF : reception, releve d'heures et panne uniquement sur ses chantiers ;
 * - DG : lecture seule (R12), sauf la decision sur une reforme demandee (R13) ;
 * - REFORME est terminal : plus aucune action.
 */
export function materielPermissions(materiel: MaterielDetail, acteur: ActeurParc): MaterielPermissions {
  const rm = acteur.roles.includes('RESPONSABLE_MATERIEL')
  const chef = acteur.roles.includes('CHEF_CHANTIER')
  const dg = acteur.roles.includes('DIRECTION_GENERALE')
  const actif = materiel.etat !== 'REFORME'
  const enAttente = affectationEnAttente(materiel)
  const enCours = affectationEnCours(materiel)
  const surMonChantier = (chantierId: string | null | undefined) =>
    chef && Boolean(chantierId) && acteur.chantierIds.includes(chantierId ?? '')
  const chantierCourant = materiel.chantierActuelId ?? enCours?.chantierId
  const terrain = actif && (rm || surMonChantier(chantierCourant))

  return {
    modifier: rm && actif,
    affecter: rm && actif,
    affecterBloque:
      materiel.etat === 'DISPONIBLE' ? null : 'Seul un matériel disponible peut être affecté (R04).',
    confirmerReception: actif && enAttente !== undefined && (rm || surMonChantier(enAttente.chantierId)),
    liberer: rm && actif && (materiel.etat === 'AFFECTE' || enCours !== undefined || enAttente !== undefined),
    releverHeures: terrain,
    // DISPONIBLE/AFFECTE -> EN_MAINTENANCE ; EN_MAINTENANCE -> remise en service (AFFECTE si affectation active).
    changerEtat: rm && actif,
    demanderReforme: rm && actif && !materiel.reformeDemandee,
    validerReforme: dg && actif && materiel.reformeDemandee,
    signalerPanne: terrain,
  }
}
