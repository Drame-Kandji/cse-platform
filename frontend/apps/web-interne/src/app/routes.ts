/**
 * Chemins de l'application : SOURCE UNIQUE. Ne jamais ecrire une URL en dur dans un composant.
 * - `paths` : motifs (avec `:id`) utilises par le routeur et les NavLink ;
 * - `to`    : constructeurs d'URL pour les liens vers une ressource.
 */
export const paths = {
  dashboard: '/',
  besoins: '/besoins',
  besoinNouveau: '/besoins/nouveau',
  besoinDetail: '/besoins/:id',
  besoinModifier: '/besoins/:id/modifier',
  validation: '/validation',
  materiels: '/materiels',
  materielDetail: '/materiels/:id',
  carte: '/carte',
  planning: '/planning',
  chantiers: '/chantiers',
  maintenances: '/maintenances',
  appelsOffres: '/appels-offres',
  appelOffreDetail: '/appels-offres/:id',
  contrats: '/contrats',
  contratDetail: '/contrats/:id',
  fournisseurs: '/fournisseurs',
  fournisseurDetail: '/fournisseurs/:id',
  finance: '/finance',
  rapports: '/rapports',
  admin: '/admin',
  adminUtilisateurs: '/admin/utilisateurs',
  adminReferentiels: '/admin/referentiels',
  adminAudit: '/admin/audit',
  adminPlateforme: '/admin/plateforme',
  notifications: '/notifications',
  profil: '/profil',
  design: '/design',
} as const

export type AppPath = (typeof paths)[keyof typeof paths]

const withId = (pattern: string) => (id: string) => pattern.replace(':id', encodeURIComponent(id))

/** Constructeurs d'URL : `to.besoinDetail(besoin.id)`. */
export const to = {
  besoinDetail: withId(paths.besoinDetail),
  besoinModifier: withId(paths.besoinModifier),
  materielDetail: withId(paths.materielDetail),
  appelOffreDetail: withId(paths.appelOffreDetail),
  fournisseurDetail: withId(paths.fournisseurDetail),
  contratDetail: withId(paths.contratDetail),
} as const
