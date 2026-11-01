import type { ProfilFournisseur, ProfilModification } from '@/domain/types'
import type { ProfilInput, ProfilOutput } from '../schemas/entreprise.schema'

/** Valeurs du formulaire a partir du profil (champs nuls => chaines vides). */
export function profilVersFormulaire(p: ProfilFournisseur): ProfilInput {
  return {
    raisonSociale: p.raisonSociale,
    ninea: p.ninea,
    rccm: p.rccm ?? '',
    adresse: p.adresse ?? '',
    ville: p.ville ?? '',
    pays: p.pays,
    telephone: p.telephone ?? '',
    email: p.email,
    contactNom: p.contactNom ?? '',
    contactFonction: p.contactFonction ?? '',
    specialites: [...p.specialites],
    certifications: p.certifications.join('\n'),
    banque: p.coordonneesBancaires?.banque ?? '',
    iban: p.coordonneesBancaires?.iban ?? '',
  }
}

/** Corps de `PUT /api/portail/profil` : coordonnees bancaires omises si vides (= inchangees). */
export function formulaireVersModification(values: ProfilOutput, version: number): ProfilModification {
  const { banque, iban, ...reste } = values
  return { ...reste, coordonneesBancaires: banque && iban ? { banque, iban } : undefined, version }
}
