/**
 * Referentiel central des enumerations metier et de leur presentation (libelle FR + ton).
 * Source unique pour StatusBadge, filtres, selects et exports. Les valeurs reprennent
 * exactement les enums Java des microservices : ne jamais les traduire cote API.
 */

/**
 * Tons visuels (mappes sur les jetons de couleur). Charte bleu + blanc : `info`, `warning` et
 * `accent` s'affichent en bleu, `success` en neutre ; ils restent acceptes pour compatibilite.
 */
export type Tone = 'neutral' | 'primary' | 'info' | 'success' | 'warning' | 'danger' | 'accent'

/**
 * Tons autorises pour un etat metier : neutre par defaut, bleu pour "actif / en cours / a traiter",
 * rouge uniquement pour erreur, rejet, critique ou retard.
 */
export type StatusTone = 'neutral' | 'primary' | 'danger'

export interface StatusDescriptor {
  /** Libelle francais affiche a l'utilisateur. */
  readonly label: string
  /** Ton visuel du badge (neutre, bleu ou rouge). */
  readonly tone: StatusTone
  /** Description optionnelle (infobulle, aide). */
  readonly description?: string
}

type DescriptorMap<V extends string> = Readonly<Record<V, StatusDescriptor>>

export const ROLES = [
  'CHEF_CHANTIER',
  'RESPONSABLE_MATERIEL',
  'RESPONSABLE_ACHATS',
  'DAF',
  'DIRECTION_GENERALE',
  'ADMIN_SYSTEME',
] as const
export type Role = (typeof ROLES)[number]

export const STATUT_BESOIN = ['EN_ATTENTE', 'VALIDE', 'REJETE', 'AFFECTE_DIRECT', 'EN_ACHAT'] as const
export type StatutBesoin = (typeof STATUT_BESOIN)[number]

/** Nature d'une entree de l'historique d'un besoin (enum Java `ActionHistorique`, ms-besoins). */
export const ACTION_HISTORIQUE_BESOIN = [
  'SOUMISSION',
  'MODIFICATION',
  'APPROBATION_ETAPE',
  'VALIDATION',
  'REJET',
  'ORIENTATION_EN_ATTENTE',
  'AFFECTATION_DIRECTE',
  'ORIENTATION_ACHAT',
  'MATERIEL_AFFECTE',
] as const
export type ActionHistoriqueBesoin = (typeof ACTION_HISTORIQUE_BESOIN)[number]

export const PRIORITE = ['NORMAL', 'URGENT', 'CRITIQUE'] as const
export type Priorite = (typeof PRIORITE)[number]

export const ETAT_MATERIEL = ['DISPONIBLE', 'AFFECTE', 'EN_MAINTENANCE', 'REFORME'] as const
export type EtatMateriel = (typeof ETAT_MATERIEL)[number]

export const CATEGORIE_MATERIEL = [
  'MATERIEL_ROULANT',
  'OUTILLAGE',
  'EQUIPEMENT_INFORMATIQUE',
  'CONSOMMABLE',
] as const
export type CategorieMateriel = (typeof CATEGORIE_MATERIEL)[number]

export const STATUT_CHANTIER = ['EN_COURS', 'TERMINE'] as const
export type StatutChantier = (typeof STATUT_CHANTIER)[number]

export const STATUT_AO = ['PUBLIE', 'CLOTURE', 'ATTRIBUE'] as const
export type StatutAO = (typeof STATUT_AO)[number]

export const STATUT_FOURNISSEUR = ['EN_ATTENTE_VALIDATION', 'ACTIF', 'SUSPENDU', 'BLACKLISTE'] as const
export type StatutFournisseur = (typeof STATUT_FOURNISSEUR)[number]

export const STATUT_CONTRAT = [
  'BROUILLON',
  'SIGNE',
  'ENGAGEMENT_VALIDE',
  'EN_LIVRAISON',
  'LIVRE',
  'CLOTURE',
] as const
export type StatutContrat = (typeof STATUT_CONTRAT)[number]

export const STATUT_MAINTENANCE = ['PLANIFIEE', 'EN_COURS', 'CLOTUREE'] as const
export type StatutMaintenance = (typeof STATUT_MAINTENANCE)[number]

export const TYPE_MAINTENANCE = ['PREVENTIVE', 'CORRECTIVE', 'LOURDE'] as const
export type TypeMaintenance = (typeof TYPE_MAINTENANCE)[number]

export const STATUT_AFFECTATION = ['EN_ATTENTE_RECEPTION', 'EN_COURS', 'TERMINEE'] as const
export type StatutAffectation = (typeof STATUT_AFFECTATION)[number]

export const GRAVITE_PANNE = ['MINEURE', 'MAJEURE', 'BLOQUANTE'] as const
export type GravitePanne = (typeof GRAVITE_PANNE)[number]

export const STATUT_PANNE = ['SIGNALEE', 'PRISE_EN_CHARGE', 'RESOLUE'] as const
export type StatutPanne = (typeof STATUT_PANNE)[number]

export const TYPE_MOUVEMENT = [
  'AFFECTATION',
  'RECEPTION',
  'LIBERATION',
  'MAINTENANCE',
  'REFORME',
  'ENREGISTREMENT',
] as const
export type TypeMouvement = (typeof TYPE_MOUVEMENT)[number]

export const TYPE_APPEL_OFFRE = ['APPEL_OFFRES', 'DEMANDE_PRIX'] as const
export type TypeAppelOffre = (typeof TYPE_APPEL_OFFRE)[number]

export const CRITERE_EVALUATION = [
  'PRIX',
  'DELAI',
  'QUALITE_TECHNIQUE',
  'GARANTIE',
  'SAV',
  'REFERENCES',
] as const
export type CritereEvaluation = (typeof CRITERE_EVALUATION)[number]

export const STATUT_APPROBATION = ['EN_ATTENTE', 'APPROUVEE', 'REFUSEE'] as const
export type StatutApprobation = (typeof STATUT_APPROBATION)[number]

export const STATUT_JALON = ['A_VENIR', 'ATTEINT', 'EN_RETARD'] as const
export type StatutJalon = (typeof STATUT_JALON)[number]

export const STATUT_FACTURE = ['RECUE', 'MANDATEE', 'PAYEE', 'REJETEE'] as const
export type StatutFacture = (typeof STATUT_FACTURE)[number]

export const TYPE_DOCUMENT = [
  'NINEA',
  'RCCM',
  'QUITUS_FISCAL',
  'ASSURANCE',
  'CERTIFICATION',
  'AUTRE',
] as const
export type TypeDocument = (typeof TYPE_DOCUMENT)[number]

export const GRAVITE_LITIGE = ['FAIBLE', 'MOYENNE', 'ELEVEE'] as const
export type GraviteLitige = (typeof GRAVITE_LITIGE)[number]

export const STATUT_LITIGE = ['OUVERT', 'EN_COURS', 'RESOLU', 'CLASSE'] as const
export type StatutLitige = (typeof STATUT_LITIGE)[number]

export const TYPE_PROCES_VERBAL = ['OUVERTURE', 'EVALUATION'] as const
export type TypeProcesVerbal = (typeof TYPE_PROCES_VERBAL)[number]

export const NIVEAU_NOTIFICATION = ['INFO', 'SUCCES', 'ALERTE', 'CRITIQUE'] as const
export type NiveauNotification = (typeof NIVEAU_NOTIFICATION)[number]

export const ROLE_LABELS: DescriptorMap<Role> = {
  CHEF_CHANTIER: {
    label: 'Chef de chantier',
    tone: 'neutral',
    description: 'Exprime les besoins de son chantier',
  },
  RESPONSABLE_MATERIEL: {
    label: 'Responsable Matériel',
    tone: 'neutral',
    description: 'Gère le parc et les affectations',
  },
  RESPONSABLE_ACHATS: {
    label: 'Responsable Achats',
    tone: 'neutral',
    description: "Pilote appels d'offres et contrats",
  },
  DAF: {
    label: 'Directeur administratif et financier',
    tone: 'neutral',
    description: 'Engagements, factures et budget',
  },
  DIRECTION_GENERALE: {
    label: 'Direction Générale',
    tone: 'neutral',
    description: 'Vision consolidée et arbitrages',
  },
  ADMIN_SYSTEME: {
    label: 'Administrateur système',
    tone: 'neutral',
    description: 'Comptes, référentiels et audit',
  },
}

/** Libelle court d'un role (menus, badges). */
export const ROLE_SHORT_LABELS: Readonly<Record<Role, string>> = {
  CHEF_CHANTIER: 'Chef de chantier',
  RESPONSABLE_MATERIEL: 'Resp. Matériel',
  RESPONSABLE_ACHATS: 'Resp. Achats',
  DAF: 'DAF',
  DIRECTION_GENERALE: 'Direction Générale',
  ADMIN_SYSTEME: 'Admin système',
}

export const STATUS_MAPS = {
  role: ROLE_LABELS,
  statutBesoin: {
    EN_ATTENTE: { label: 'En attente', tone: 'primary' },
    VALIDE: { label: 'Validé', tone: 'neutral' },
    REJETE: { label: 'Rejeté', tone: 'danger' },
    AFFECTE_DIRECT: { label: 'Affecté directement', tone: 'neutral' },
    EN_ACHAT: { label: 'En achat', tone: 'primary' },
  } satisfies DescriptorMap<StatutBesoin>,
  actionHistoriqueBesoin: {
    SOUMISSION: { label: 'Soumission', tone: 'neutral' },
    MODIFICATION: { label: 'Modification', tone: 'neutral' },
    APPROBATION_ETAPE: { label: "Approbation d'étape", tone: 'neutral' },
    VALIDATION: { label: 'Validation', tone: 'neutral' },
    REJET: { label: 'Rejet', tone: 'danger' },
    ORIENTATION_EN_ATTENTE: { label: 'Orientation en attente', tone: 'neutral' },
    AFFECTATION_DIRECTE: { label: 'Affectation directe', tone: 'neutral' },
    ORIENTATION_ACHAT: { label: 'Orientation vers les achats', tone: 'neutral' },
    MATERIEL_AFFECTE: { label: 'Matériel affecté', tone: 'neutral' },
  } satisfies DescriptorMap<ActionHistoriqueBesoin>,
  priorite: {
    NORMAL: { label: 'Normale', tone: 'neutral' },
    URGENT: { label: 'Urgente', tone: 'primary' },
    CRITIQUE: { label: 'Critique', tone: 'danger' },
  } satisfies DescriptorMap<Priorite>,
  etatMateriel: {
    DISPONIBLE: { label: 'Disponible', tone: 'neutral' },
    AFFECTE: { label: 'Affecté', tone: 'primary' },
    EN_MAINTENANCE: { label: 'En maintenance', tone: 'primary' },
    REFORME: { label: 'Réformé', tone: 'neutral' },
  } satisfies DescriptorMap<EtatMateriel>,
  categorieMateriel: {
    MATERIEL_ROULANT: { label: 'Matériel roulant', tone: 'neutral' },
    OUTILLAGE: { label: 'Outillage', tone: 'neutral' },
    EQUIPEMENT_INFORMATIQUE: { label: 'Équipement informatique', tone: 'neutral' },
    CONSOMMABLE: { label: 'Consommable', tone: 'neutral' },
  } satisfies DescriptorMap<CategorieMateriel>,
  statutChantier: {
    EN_COURS: { label: 'En cours', tone: 'primary' },
    TERMINE: { label: 'Terminé', tone: 'neutral' },
  } satisfies DescriptorMap<StatutChantier>,
  statutAO: {
    PUBLIE: { label: 'Publié', tone: 'primary' },
    CLOTURE: { label: 'Clôturé', tone: 'neutral' },
    ATTRIBUE: { label: 'Attribué', tone: 'neutral' },
  } satisfies DescriptorMap<StatutAO>,
  statutFournisseur: {
    EN_ATTENTE_VALIDATION: { label: 'En attente de validation', tone: 'primary' },
    ACTIF: { label: 'Actif', tone: 'neutral' },
    SUSPENDU: { label: 'Suspendu', tone: 'neutral' },
    BLACKLISTE: { label: 'Blacklisté', tone: 'danger' },
  } satisfies DescriptorMap<StatutFournisseur>,
  statutContrat: {
    BROUILLON: { label: 'Brouillon', tone: 'neutral' },
    SIGNE: { label: 'Signé', tone: 'neutral' },
    ENGAGEMENT_VALIDE: { label: 'Engagement validé', tone: 'primary' },
    EN_LIVRAISON: { label: 'En livraison', tone: 'primary' },
    LIVRE: { label: 'Livré', tone: 'neutral' },
    CLOTURE: { label: 'Clôturé', tone: 'neutral' },
  } satisfies DescriptorMap<StatutContrat>,
  statutMaintenance: {
    PLANIFIEE: { label: 'Planifiée', tone: 'neutral' },
    EN_COURS: { label: 'En cours', tone: 'primary' },
    CLOTUREE: { label: 'Clôturée', tone: 'neutral' },
  } satisfies DescriptorMap<StatutMaintenance>,
  typeMaintenance: {
    PREVENTIVE: { label: 'Préventive', tone: 'neutral' },
    CORRECTIVE: { label: 'Corrective', tone: 'neutral' },
    LOURDE: { label: 'Lourde', tone: 'neutral' },
  } satisfies DescriptorMap<TypeMaintenance>,
  statutAffectation: {
    EN_ATTENTE_RECEPTION: { label: 'En attente de réception', tone: 'primary' },
    EN_COURS: { label: 'En cours', tone: 'primary' },
    TERMINEE: { label: 'Terminée', tone: 'neutral' },
  } satisfies DescriptorMap<StatutAffectation>,
  gravitePanne: {
    MINEURE: { label: 'Mineure', tone: 'neutral' },
    MAJEURE: { label: 'Majeure', tone: 'neutral' },
    BLOQUANTE: { label: 'Bloquante', tone: 'danger', description: 'Matériel immobilisé' },
  } satisfies DescriptorMap<GravitePanne>,
  statutPanne: {
    SIGNALEE: { label: 'Signalée', tone: 'primary' },
    PRISE_EN_CHARGE: { label: 'Prise en charge', tone: 'primary' },
    RESOLUE: { label: 'Résolue', tone: 'neutral' },
  } satisfies DescriptorMap<StatutPanne>,
  typeMouvement: {
    AFFECTATION: { label: 'Affectation', tone: 'neutral' },
    RECEPTION: { label: 'Réception', tone: 'neutral' },
    LIBERATION: { label: 'Libération', tone: 'neutral' },
    MAINTENANCE: { label: 'Maintenance', tone: 'neutral' },
    REFORME: { label: 'Réforme', tone: 'neutral' },
    ENREGISTREMENT: { label: 'Enregistrement', tone: 'neutral' },
  } satisfies DescriptorMap<TypeMouvement>,
  typeAppelOffre: {
    APPEL_OFFRES: { label: "Appel d'offres", tone: 'neutral' },
    DEMANDE_PRIX: { label: 'Demande de prix', tone: 'neutral' },
  } satisfies DescriptorMap<TypeAppelOffre>,
  critereEvaluation: {
    PRIX: { label: 'Prix', tone: 'neutral', description: 'Calculé à partir du montant proposé' },
    DELAI: { label: 'Délai de livraison', tone: 'neutral', description: 'Calculé à partir du délai proposé' },
    QUALITE_TECHNIQUE: { label: 'Qualité technique', tone: 'neutral' },
    GARANTIE: { label: 'Garantie', tone: 'neutral' },
    SAV: { label: 'Service après-vente', tone: 'neutral' },
    REFERENCES: { label: 'Références', tone: 'neutral' },
  } satisfies DescriptorMap<CritereEvaluation>,
  statutApprobation: {
    EN_ATTENTE: { label: 'En attente', tone: 'primary' },
    APPROUVEE: { label: 'Approuvée', tone: 'neutral' },
    REFUSEE: { label: 'Refusée', tone: 'danger' },
  } satisfies DescriptorMap<StatutApprobation>,
  statutJalon: {
    A_VENIR: { label: 'À venir', tone: 'neutral' },
    ATTEINT: { label: 'Atteint', tone: 'neutral' },
    EN_RETARD: { label: 'En retard', tone: 'danger' },
  } satisfies DescriptorMap<StatutJalon>,
  statutFacture: {
    RECUE: { label: 'Reçue', tone: 'primary' },
    MANDATEE: { label: 'Mandatée', tone: 'primary' },
    PAYEE: { label: 'Payée', tone: 'neutral' },
    REJETEE: { label: 'Rejetée', tone: 'danger' },
  } satisfies DescriptorMap<StatutFacture>,
  typeDocument: {
    NINEA: { label: 'NINEA', tone: 'neutral' },
    RCCM: { label: 'RCCM', tone: 'neutral' },
    QUITUS_FISCAL: { label: 'Quitus fiscal', tone: 'neutral' },
    ASSURANCE: { label: 'Assurance', tone: 'neutral' },
    CERTIFICATION: { label: 'Certification', tone: 'neutral' },
    AUTRE: { label: 'Autre', tone: 'neutral' },
  } satisfies DescriptorMap<TypeDocument>,
  graviteLitige: {
    FAIBLE: { label: 'Faible', tone: 'neutral' },
    MOYENNE: { label: 'Moyenne', tone: 'neutral' },
    ELEVEE: { label: 'Élevée', tone: 'danger' },
  } satisfies DescriptorMap<GraviteLitige>,
  statutLitige: {
    OUVERT: { label: 'Ouvert', tone: 'primary' },
    EN_COURS: { label: 'En cours', tone: 'primary' },
    RESOLU: { label: 'Résolu', tone: 'neutral' },
    CLASSE: { label: 'Classé', tone: 'neutral' },
  } satisfies DescriptorMap<StatutLitige>,
  typeProcesVerbal: {
    OUVERTURE: { label: "Procès-verbal d'ouverture des plis", tone: 'neutral' },
    EVALUATION: { label: "Procès-verbal d'évaluation", tone: 'neutral' },
  } satisfies DescriptorMap<TypeProcesVerbal>,
  niveauNotification: {
    INFO: { label: 'Information', tone: 'neutral' },
    SUCCES: { label: 'Succès', tone: 'neutral' },
    ALERTE: { label: 'Alerte', tone: 'primary' },
    CRITIQUE: { label: 'Critique', tone: 'danger' },
  } satisfies DescriptorMap<NiveauNotification>,
} as const

/** Nom d'une enumeration enregistree dans STATUS_MAPS. */
export type StatusKind = keyof typeof STATUS_MAPS
/** Valeurs possibles pour une enumeration donnee. */
export type StatusValue<K extends StatusKind> = keyof (typeof STATUS_MAPS)[K] & string

/**
 * Retourne le descripteur (libelle + ton) d'une valeur d'enum.
 * Une valeur inconnue (nouvelle valeur backend) retombe sur un ton neutre.
 */
export function describeStatus(kind: StatusKind, value: string): StatusDescriptor {
  const map: Readonly<Record<string, StatusDescriptor>> = STATUS_MAPS[kind]
  return map[value] ?? { label: value, tone: 'neutral' }
}

/** Libelle francais d'une valeur d'enum. */
export function labelOf(kind: StatusKind, value: string): string {
  return describeStatus(kind, value).label
}

/**
 * Options `{ value, label }` pour un select ou un filtre, dans l'ordre de declaration.
 * @example <Select options={optionsOf('priorite')} />
 */
export function optionsOf<K extends StatusKind>(kind: K): { value: StatusValue<K>; label: string }[] {
  const map: Readonly<Record<string, StatusDescriptor>> = STATUS_MAPS[kind]
  return Object.entries(map).map(([value, descriptor]) => ({
    value: value as StatusValue<K>,
    label: descriptor.label,
  }))
}
