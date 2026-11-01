/** Chemins du portail (jamais d'URL en dur dans un composant). */
export const paths = {
  accueil: '/',
  inscription: '/inscription',
  appelsOffres: '/appels-offres',
  appelOffreDetail: '/appels-offres/:id',
  contrats: '/contrats',
  contratDetail: '/contrats/:id',
  factures: '/factures',
  entreprise: '/entreprise',
} as const

/** Liens parametres. */
export const to = {
  appelOffreDetail: (id: string) => `/appels-offres/${id}`,
  mesOffres: () => '/appels-offres?onglet=mes-offres',
  contratDetail: (id: string) => `/contrats/${id}`,
  contrats: (onglet?: string) => (onglet ? `/contrats?onglet=${onglet}` : '/contrats'),
  factures: (statut?: string) => (statut ? `/factures?statut=${statut}` : '/factures'),
  nouvelleFacture: (contratId: string) => `/factures?nouvelle=${contratId}`,
}
