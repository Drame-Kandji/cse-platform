import type { AppelOffrePortail, Contrat, Facture, OffrePortail, ProfilFournisseur } from '@/domain/types'
import { FOURNISSEUR_ID } from './render'

/** Appel d'offres ouvert (procedure ouverte, date limite dans 5 jours). */
export function appelOffre(overrides: Partial<AppelOffrePortail> = {}): AppelOffrePortail {
  return {
    id: 'ao-1',
    reference: 'AO-2026-012',
    objet: 'Fourniture de 3 camions-bennes',
    description: 'Camions 6x4 pour Thiès',
    type: 'APPEL_OFFRES',
    categorie: 'MATERIEL_ROULANT',
    quantite: 3,
    specifications: 'Benne 20 m3',
    datePublication: '2026-10-23T08:00:00Z',
    dateLimite: new Date(Date.now() + 5 * 86_400_000).toISOString(),
    statut: 'PUBLIE',
    criteres: [
      { code: 'PRIX', poids: 60 },
      { code: 'DELAI', poids: 40 },
    ],
    offreDeposee: false,
    ...overrides,
  }
}

export function offre(overrides: Partial<OffrePortail> = {}): OffrePortail {
  return {
    id: 'of-1',
    appelOffreId: 'ao-1',
    appelOffreReference: 'AO-2026-012',
    appelOffreObjet: 'Fourniture de 3 camions-bennes',
    montantPropose: 96_000_000,
    delaiLivraisonJours: 30,
    garantieMois: 12,
    dateDepot: '2026-10-25T10:00:00Z',
    statutAppelOffre: 'PUBLIE',
    ...overrides,
  }
}

export function contrat(overrides: Partial<Contrat> = {}): Contrat {
  return {
    id: 'ct-1',
    numero: 'CTR-2026-001',
    appelOffreId: 'ao-0',
    appelOffreReference: 'AO-2026-001',
    offreId: 'of-0',
    fournisseurId: FOURNISSEUR_ID,
    fournisseurNom: 'Sahel Engins SA',
    objet: 'Acquisition de deux pelles hydrauliques',
    designation: 'Pelle hydraulique 20 t',
    categorie: 'MATERIEL_ROULANT',
    quantite: 2,
    montant: 285_000_000,
    statut: 'EN_ATTENTE_SIGNATURE',
    delaiLivraisonJours: 45,
    dateLivraisonPrevue: '2026-12-15',
    clauses: {
      garantieMois: 24,
      penaliteRetardPctParJour: 0.5,
      conditionsLivraison: 'Livraison sur chantier',
    },
    jalons: [
      { id: 'j-1', libelle: 'Livraison de la première pelle', datePrevue: '2026-12-01', statut: 'A_VENIR' },
    ],
    penalitesRetard: 0,
    version: 1,
    ...overrides,
  }
}

export function facture(overrides: Partial<Facture> = {}): Facture {
  return {
    id: 'fa-1',
    numero: 'FA-SE-2026-118',
    contratId: 'ct-1',
    contratNumero: 'CTR-2026-001',
    montantHt: 100_000_000,
    tva: 18_000_000,
    montantTtc: 118_000_000,
    dateEmission: '2026-10-20',
    dateEcheance: '2026-11-19',
    statut: 'RECUE',
    enRetard: false,
    ...overrides,
  }
}

export function profil(overrides: Partial<ProfilFournisseur> = {}): ProfilFournisseur {
  return {
    id: FOURNISSEUR_ID,
    raisonSociale: 'Sahel Engins SA',
    ninea: '005123456 2G3',
    pays: 'Sénégal',
    email: 'contact@sahel-engins.sn',
    specialites: ['MATERIEL_ROULANT'],
    certifications: ['ISO 9001:2015'],
    statut: 'ACTIF',
    scorePerformance: 82.5,
    nombreEvaluations: 6,
    tauxLivraisonALHeure: 87.5,
    dateInscription: '2025-11-03',
    documents: [],
    version: 4,
    ...overrides,
  }
}
