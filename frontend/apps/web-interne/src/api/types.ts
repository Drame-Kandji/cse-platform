export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first?: boolean
  last: boolean
}

export type ProblemDetail = {
  type?: string
  title?: string
  status?: number
  detail?: string
  code?: string
}

export type Priorite = 'NORMAL' | 'URGENT' | 'CRITIQUE'
export type StatutBesoin = 'EN_ATTENTE' | 'VALIDE' | 'REJETE' | 'EN_ACHAT' | 'SATISFAIT'
export type EtatMateriel = 'DISPONIBLE' | 'AFFECTE' | 'EN_MAINTENANCE' | 'EN_PANNE' | 'REFORME'
export type StatutAppelOffre = 'PUBLIE' | 'CLOTURE' | 'ATTRIBUE'
export type StatutFournisseur = 'ACTIF' | 'EN_ATTENTE_VALIDATION' | 'SUSPENDU' | 'BLACKLISTE'

export interface BesoinMateriel {
  id: string
  reference: string
  designation: string
  quantite: number
  specifications: string | null
  priorite: Priorite
  statut: StatutBesoin
  chantierId: string
  demandeurId: string
  dureeUtilisationPrevue?: number | null
  justification: string | null
  motifRejet: string | null
  createdAt: string
}

export interface BesoinCreation {
  designation: string
  quantite: number
  specifications: string
  priorite: Priorite
  chantierId: string
  demandeurId: string
  dureeUtilisationPrevue: number
  justification: string
}

export interface BesoinSoumission {
  id: string
  reference: string
  statut: StatutBesoin
}

export interface BesoinHistoriqueEntry {
  besoinId: string
  statutActuel: StatutBesoin
  dateCreation: string
}

export interface Materiel {
  id: string
  immatriculation: string
  designation: string
  categorie: string
  marque?: string | null
  modele?: string | null
  etat: EtatMateriel
  chantierActuelId: string | null
  latitude?: number | null
  longitude?: number | null
  heuresUtilisation?: number | null
  heuresFonctionnement?: number | null
  localisation?: string | null
  createdAt?: string
}

export interface DisponibiliteResult {
  disponible: boolean
  quantiteDisponible: number
  materielsDisponibles?: Materiel[]
}

export interface GeoJsonMateriels {
  type: 'FeatureCollection'
  features: Materiel[]
}

export interface Chantier {
  id: string
  nom: string
  adresse: string | null
  ville: string | null
  latitude: number | null
  longitude: number | null
  statut: string
  budget: number | null
}

export interface ChantierCreation {
  nom: string
  adresse: string
  ville: string
  latitude: number
  longitude: number
  budget: number
}

export interface AffectationCreation {
  chantierId: string
  besoinId: string | null
  demandeurId: string
  dateDebut: string
  dateFinPrevue: string
  motif: string
  affectePar: string
}

export interface Maintenance {
  id: string
  numeroOt: string
  materielId: string
  typeMaintenance: 'PREVENTIVE' | 'CORRECTIVE' | 'LOURDE' | string
  statut: string
  datePlanifiee: string
  technicien: string | null
  motif: string | null
  cout: number | null
}

export interface MaintenanceCreation {
  materielId: string
  typeMaintenance: 'PREVENTIVE' | 'CORRECTIVE' | 'LOURDE'
  datePlanifiee: string
  technicien: string
  motif: string
  coutEstime: number
}

export interface PanneCreation {
  materielId: string
  chantierId: string
  description: string
  urgence: 'NORMALE' | 'URGENTE' | 'CRITIQUE'
}

export interface PanneSignalement {
  id: string
  materielId: string
  chantierId: string
  urgence: string
  statut: string
  horodatage: string
}

export interface AffectationPlanning {
  materielImmatriculation: string
  designation: string
  chantier: string
  dateDebut: string
  dateFinPrevue: string | null
}

export interface AppelOffre {
  id: string
  reference: string
  objet: string
  typeAo: 'APPEL_OFFRES' | 'CONSULTATION_RESTREINTE' | string
  besoinId: string | null
  dateLimite: string
  statut: StatutAppelOffre
  offreRetenueId: string | null
}

export interface AppelOffreCreation {
  objet: string
  typeAo: 'APPEL_OFFRES' | 'CONSULTATION_RESTREINTE'
  besoinId: string
  dateLimite: string
}

export interface Offre {
  id: string
  appelOffreId: string
  fournisseurId: string | null
  codeAnonyme: string
  montantPropose: number
  delaiLivraisonJours: number
  noteTechnique: number
}

export interface OffreComparatif {
  offreId: string
  codeAnonyme: string
  montantPropose: number
  delaiLivraisonJours: number
  noteTechnique: number
  scoreGlobalPondere: number
}

export interface Contrat {
  id: string
  numero: string
  appelOffreId: string
  offreId: string | null
  fournisseurId: string
  montantTotal: number
  statut: string
  engagementValideDaf: boolean
}

export interface ContratCreation {
  appelOffreId: string
  offreId: string
  fournisseurId: string
  montantTotal: number
}

export interface Commande {
  id: string
  numeroCommande: string
  contratNumero: string
  statut: string
  delaiContractuelJours: number
}

export interface Facture {
  id: string
  numeroFacture: string
  contratId: string
  montantTtc: number
  statut: string
}

export interface Fournisseur {
  id: string
  raisonSociale: string
  ninea: string
  rc: string | null
  email: string
  telephone: string | null
  specialite: string
  statut: StatutFournisseur
  scorePerformance: number
  demandeBlacklistage: boolean
  motifBlacklistage: string | null
  createdAt: string
}

export interface FournisseurCreation {
  raisonSociale: string
  ninea: string
  rc: string
  email: string
  telephone: string
  specialite: string
}

export interface EvaluationFournisseur {
  noteQualite: number
  noteDelai: number
  noteConformite: number
  noteSav: number
  commentaire: string
}

export interface CatalogueEntry {
  reference: string
  designation: string
  categorie: string
  prixUnitaireFcfa: number
  delaiJours: number
}

export interface ComparaisonPrixEntry {
  fournisseur: string
  categorie: string
  designation: string
  prixUnitaireFcfa: number
  scoreFournisseur: number
}

export interface Litige {
  id: string
  fournisseurId: string
  motif: string
  statut: string
}

export interface Utilisateur {
  id: string
  nom: string
  prenom: string
  email: string
  role: string
  actif: boolean
  chantierAttribueId: string | null
  perimetreParc: string | null
}

export interface UtilisateurCreation {
  nom: string
  prenom: string
  email: string
  role: string
  chantierAttribueId: string | null
  perimetreParc: string | null
}

export interface AuditLog {
  id: string
  utilisateur: string
  action: string
  entite: string
  entiteId: string
  details: string
  timestamp: string
}

export interface NotificationCse {
  id: string
  destinataire: string
  type: string
  sujet: string
  message: string
  lue: boolean
  createdAt: string
}

export interface NotificationCreation {
  destinataire: string
  type: string
  sujet: string
  message: string
}

export interface ChatbotEntry {
  id: string
  question: string
  intentionDetectee: string
  reponse: string
  horodatage: string
}

export interface KpiDashboard {
  profil: string
  periode: string
  tauxDisponibiliteParcPct: number
  tauxUtilisationMoyennePct: number
  delaiMoyenApprovisionnementJours: number
  budgetEngageFcfa: number
  budgetRealiseFcfa: number
  chantiersActifs: number
  enginsEnMaintenance: number
  fournisseursActifs: number
}

export interface SeriePoint {
  periode: string
  kpi: string
  valeur: number
}

export interface ExportReport {
  status: string
  format: string
  periode: string
  fichier: string
}
