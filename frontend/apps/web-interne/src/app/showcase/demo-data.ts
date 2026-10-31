import {
  CATEGORIE_MATERIEL,
  ETAT_MATERIEL,
  type CategorieMateriel,
  type EtatMateriel,
} from '@/shared/domain/labels'

/** Donnees fictives de la vitrine (route /design, developpement uniquement). */
export interface DemoMateriel {
  id: string
  code: string
  designation: string
  categorie: CategorieMateriel
  etat: EtatMateriel
  chantier: string | null
  valeur: number
  miseEnService: string
}

const DESIGNATIONS = [
  'Pelle hydraulique CAT 320',
  'Chargeuse Volvo L90',
  'Bétonnière 350 L',
  'Groupe électrogène 60 kVA',
  'Compacteur Bomag BW 213',
  'Camion benne Renault K',
  'Station totale Leica',
  'Ordinateur portable terrain',
  'Marteau piqueur Hilti',
  'Grue mobile Liebherr',
]
const CHANTIERS = [
  'Autoroute Diamniadio-AIBD',
  'TER phase 2',
  'Port de Saint-Louis',
  'Pont de Kaolack',
  null,
]

export const DEMO_MATERIELS: DemoMateriel[] = Array.from({ length: 47 }, (_, index) => {
  const etat = ETAT_MATERIEL[(index * 7) % ETAT_MATERIEL.length] ?? 'DISPONIBLE'
  return {
    id: `mat-${index + 1}`,
    code: `CSE-${String(1000 + index * 13)}`,
    designation: DESIGNATIONS[index % DESIGNATIONS.length] ?? 'Matériel',
    categorie: CATEGORIE_MATERIEL[index % 3] ?? 'OUTILLAGE',
    etat,
    chantier: etat === 'AFFECTE' ? (CHANTIERS[index % 4] ?? null) : null,
    valeur: 1_500_000 + ((index * 7_919_113) % 180_000_000),
    miseEnService: new Date(2019 + (index % 6), index % 12, 1 + (index % 27)).toISOString(),
  }
})

export const MONTHLY = [
  'janv.',
  'févr.',
  'mars',
  'avr.',
  'mai',
  'juin',
  'juil.',
  'août',
  'sept.',
  'oct.',
].map((mois, index) => ({
  mois,
  disponibilite: 0.72 + Math.sin(index / 2) * 0.06 + index * 0.008,
  maintenance: 4_200_000 + Math.round(Math.cos(index) * 1_100_000) + index * 150_000,
  achats: 9_500_000 + Math.round(Math.sin(index * 1.3) * 2_600_000),
}))
