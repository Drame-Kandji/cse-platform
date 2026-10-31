import { ETAT_MATERIEL, type EtatMateriel } from '@/shared/domain/labels'
import type { CarteFeature, CarteFeatureProperties } from '@/shared/domain/types'

/** Marqueur de la carte : un chantier et ses engins, ou des engins hors chantier au meme point. */
export interface CarteGroupe {
  /** Cle stable : "chantier:<id>" ou coordonnees arrondies "lat,lng" (points isoles). */
  readonly key: string
  readonly lat: number
  readonly lng: number
  /** Chantier du groupe, null pour des engins hors chantier. */
  readonly chantier: CarteFeatureProperties | null
  readonly materiels: readonly CarteFeatureProperties[]
  /** Effectif des materiels par etat (materiels sans etat ignores). */
  readonly parEtat: Readonly<Record<EtatMateriel, number>>
}

interface Accumulateur {
  key: string
  latSum: number
  lngSum: number
  points: number
  chantier: CarteFeatureProperties | null
  materiels: CarteFeatureProperties[]
}

function emptyCounts(): Record<EtatMateriel, number> {
  return Object.fromEntries(ETAT_MATERIEL.map((etat) => [etat, 0])) as Record<EtatMateriel, number>
}

function isValidCoordinate(lat: number, lng: number): boolean {
  return Number.isFinite(lat) && Number.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180
}

/**
 * Regroupe les entites GeoJSON (agregation sans dependance de clustering).
 * ms-suivi renvoie la position GPS propre de chaque engin (quelques centaines de metres autour du
 * chantier) et seulement le nom de son chantier : un engin dont `chantierNom` correspond a un chantier
 * renvoye est rattache au marqueur de ce chantier (positionne sur le chantier). Les autres (depot,
 * chantier hors perimetre) sont regroupes par coordonnees arrondies.
 * GeoJSON ordonne les coordonnees [longitude, latitude].
 *
 * @param features entites de `GET /api/materiels/carte`
 * @param precision decimales conservees pour les points isoles (4 = ~11 m)
 * @returns groupes tries : chantiers d'abord (par libelle), puis points isoles
 */
export function grouperParPosition(features: readonly CarteFeature[], precision = 4): CarteGroupe[] {
  const groupes = new Map<string, Accumulateur>()
  const parNomChantier = new Map<string, Accumulateur>()
  const points = features.filter((feature) => {
    const [lng, lat] = feature.geometry.coordinates
    return isValidCoordinate(lat, lng)
  })
  for (const feature of points) {
    if (feature.properties.kind !== 'CHANTIER') continue
    const [lng, lat] = feature.geometry.coordinates
    const key = `chantier:${feature.properties.id}`
    if (groupes.has(key)) continue
    const groupe = { key, latSum: lat, lngSum: lng, points: 1, chantier: feature.properties, materiels: [] }
    groupes.set(key, groupe)
    parNomChantier.set(feature.properties.libelle, groupe)
  }
  for (const feature of points) {
    if (feature.properties.kind === 'CHANTIER') continue
    const chantier = feature.properties.chantierNom
      ? parNomChantier.get(feature.properties.chantierNom)
      : undefined
    if (chantier) {
      chantier.materiels.push(feature.properties)
      continue
    }
    const [lng, lat] = feature.geometry.coordinates
    const key = `${lat.toFixed(precision)},${lng.toFixed(precision)}`
    let groupe = groupes.get(key)
    if (!groupe) {
      groupe = { key, latSum: 0, lngSum: 0, points: 0, chantier: null, materiels: [] }
      groupes.set(key, groupe)
    }
    groupe.latSum += lat
    groupe.lngSum += lng
    groupe.points += 1
    groupe.materiels.push(feature.properties)
  }
  return [...groupes.values()]
    .map((groupe): CarteGroupe => {
      const parEtat = emptyCounts()
      for (const materiel of groupe.materiels) {
        if (materiel.etat) parEtat[materiel.etat] += 1
      }
      return {
        key: groupe.key,
        lat: groupe.latSum / groupe.points,
        lng: groupe.lngSum / groupe.points,
        chantier: groupe.chantier,
        materiels: groupe.materiels,
        parEtat,
      }
    })
    .sort((a, b) => {
      if (a.chantier && !b.chantier) return -1
      if (!a.chantier && b.chantier) return 1
      const la = a.chantier?.libelle ?? a.materiels[0]?.libelle ?? ''
      const lb = b.chantier?.libelle ?? b.materiels[0]?.libelle ?? ''
      return la.localeCompare(lb, 'fr')
    })
}

/** Nombre de materiels a afficher pour un groupe (declare par le chantier ou compte localement). */
export function effectifGroupe(groupe: CarteGroupe): number {
  return Math.max(groupe.materiels.length, groupe.chantier?.nombreMateriels ?? 0)
}

/**
 * Diametre (px) d'un marqueur de chantier, croissant avec la racine de l'effectif.
 * @example tailleMarqueur(0) // 28 ; tailleMarqueur(100) // 56
 */
export function tailleMarqueur(effectif: number): number {
  return Math.round(Math.min(56, 28 + Math.sqrt(Math.max(0, effectif)) * 2.8))
}

/** Etat majoritaire d'un groupe (ordre de declaration en cas d'egalite), null si vide. */
export function etatDominant(groupe: CarteGroupe): EtatMateriel | null {
  let best: EtatMateriel | null = null
  for (const etat of ETAT_MATERIEL) {
    if (groupe.parEtat[etat] > 0 && (best === null || groupe.parEtat[etat] > groupe.parEtat[best]))
      best = etat
  }
  return best
}

/** Emprise du Senegal (sud-ouest, nord-est) pour le cadrage initial. */
export const SENEGAL_BOUNDS: [[number, number], [number, number]] = [
  [12.3, -17.55],
  [16.7, -11.35],
]
