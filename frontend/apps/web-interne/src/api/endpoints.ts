import { api } from './client'
import type {
  AffectationCreation,
  AffectationPlanning,
  AppelOffre,
  AppelOffreCreation,
  AuditLog,
  BesoinCreation,
  BesoinHistoriqueEntry,
  BesoinMateriel,
  BesoinSoumission,
  CatalogueEntry,
  ChatbotEntry,
  Chantier,
  ChantierCreation,
  Commande,
  ComparaisonPrixEntry,
  Contrat,
  ContratCreation,
  DisponibiliteResult,
  EvaluationFournisseur,
  ExportReport,
  Facture,
  Fournisseur,
  FournisseurCreation,
  GeoJsonMateriels,
  KpiDashboard,
  Litige,
  Maintenance,
  MaintenanceCreation,
  Materiel,
  NotificationCse,
  NotificationCreation,
  Offre,
  OffreComparatif,
  PageResponse,
  PanneCreation,
  PanneSignalement,
  SeriePoint,
  Utilisateur,
  UtilisateurCreation,
} from './types'

type Params = Record<string, string | number | boolean | null | undefined>

function clean(params?: Params): Record<string, string | number | boolean> {
  const out: Record<string, string | number | boolean> = {}
  if (!params) return out
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') out[key] = value
  })
  return out
}

const data = <T>(promise: Promise<{ data: T }>) => promise.then((response) => response.data)

export const besoinsApi = {
  list: (params?: Params) => data(api.get<PageResponse<BesoinMateriel>>('/besoins', { params: clean(params) })),
  detail: (id: string) => data(api.get<BesoinMateriel>(`/besoins/${id}`)),
  create: (body: BesoinCreation) => data(api.post<BesoinSoumission>('/besoins', body)),
  valider: (id: string, validePar: string) => data(api.patch<{ id: string; statut: string }>(`/besoins/${id}/valider`, { validePar })),
  rejeter: (id: string, motif: string, rejetePar: string) =>
    data(api.patch<{ id: string; statut: string }>(`/besoins/${id}/rejeter`, { motif, rejetePar })),
  consolidation: () => data(api.get<BesoinMateriel[]>('/besoins/consolidation')),
  historique: (id: string) => data(api.get<BesoinHistoriqueEntry[]>(`/besoins/${id}/historique`)),
}

export const chantiersApi = {
  list: () => data(api.get<Chantier[]>('/chantiers')),
  create: (body: ChantierCreation) => data(api.post<Chantier>('/chantiers', body)),
}

export const materielsApi = {
  list: (params?: Params) => data(api.get<PageResponse<Materiel>>('/materiels', { params: clean(params) })),
  detail: (id: string) => data(api.get<Materiel>(`/materiels/${id}`)),
  carte: () => data(api.get<GeoJsonMateriels>('/materiels/carte')),
  disponibilite: (params: { categorie: string; designation: string; quantite: number }) =>
    data(api.get<DisponibiliteResult>('/materiels/disponibilite', { params })),
  affecter: (id: string, body: AffectationCreation) =>
    data(api.post<{ id: string; status: string }>(`/materiels/${id}/affecter`, body)),
  confirmerReception: (id: string) =>
    data(api.post<{ id: string; status: string }>(`/materiels/${id}/confirmer-reception`)),
  demanderReforme: (id: string) =>
    data(api.post<{ id: string; status: string }>(`/materiels/${id}/reforme/demande`)),
  validerReforme: (id: string) =>
    data(api.patch<{ id: string; status: string }>(`/materiels/${id}/reforme/valider`)),
}

export const suiviApi = {
  planning: () => data(api.get<AffectationPlanning[]>('/affectations/planning')),
  creerPanne: (body: PanneCreation) => data(api.post<PanneSignalement>('/pannes', body)),
  maintenances: () => data(api.get<Maintenance[]>('/maintenances')),
  creerMaintenance: (body: MaintenanceCreation) => data(api.post<Maintenance>('/maintenances', body)),
  cloturerMaintenance: (id: string) => data(api.patch<Maintenance>(`/maintenances/${id}/cloturer`)),
}

export const achatsApi = {
  appelsOffres: (params?: Params) =>
    data(api.get<PageResponse<AppelOffre>>('/appels-offres', { params: clean(params) })),
  creerAppelOffre: (body: AppelOffreCreation) => data(api.post<AppelOffre>('/appels-offres', body)),
  offres: (id: string) => data(api.get<Offre[]>(`/appels-offres/${id}/offres`)),
  cloturer: (id: string) => data(api.post<AppelOffre>(`/appels-offres/${id}/cloturer`)),
  comparatif: (id: string) => data(api.get<OffreComparatif[]>(`/appels-offres/${id}/comparatif`)),
  attribuer: (id: string, offreId: string) => data(api.post<AppelOffre>(`/appels-offres/${id}/attribuer`, { offreId })),
  contrats: () => data(api.get<Contrat[]>('/contrats')),
  creerContrat: (body: ContratCreation) => data(api.post<Contrat>('/contrats', body)),
  signerContrat: (id: string) => data(api.post<Contrat>(`/contrats/${id}/signer`)),
  commandes: () => data(api.get<Commande[]>('/commandes')),
  factures: () => data(api.get<Facture[]>('/factures')),
  mandaterFacture: (id: string) => data(api.post<{ factureId: string; statut: string }>(`/factures/${id}/mandater`)),
  validerEngagement: (contratId: string) => data(api.post<Contrat>(`/engagements/${contratId}/valider`)),
}

export const fournisseursApi = {
  list: (params?: Params) =>
    data(api.get<PageResponse<Fournisseur>>('/fournisseurs', { params: clean(params) })),
  detail: (id: string) => data(api.get<Fournisseur>(`/fournisseurs/${id}`)),
  create: (body: FournisseurCreation) => data(api.post<Fournisseur>('/fournisseurs', body)),
  validerInscription: (id: string) => data(api.patch<Fournisseur>(`/fournisseurs/${id}/valider-inscription`)),
  evaluer: (id: string, body: EvaluationFournisseur) => data(api.post<Fournisseur>(`/fournisseurs/${id}/evaluer`, body)),
  suspendre: (id: string) => data(api.patch<Fournisseur>(`/fournisseurs/${id}/suspendre`)),
  demanderBlacklistage: (id: string, motif: string) =>
    data(api.post<Fournisseur>(`/fournisseurs/${id}/blacklistage/demande`, { motif })),
  blacklister: (id: string) => data(api.patch<Fournisseur>(`/fournisseurs/${id}/blacklister`)),
  classement: (categorie?: string) =>
    data(api.get<Fournisseur[]>('/fournisseurs/classement', { params: clean({ categorie }) })),
  catalogue: (id: string) => data(api.get<CatalogueEntry[]>(`/fournisseurs/${id}/catalogue`)),
  comparaisonPrix: (categorie: string) =>
    data(api.get<ComparaisonPrixEntry[]>('/fournisseurs/comparaison-prix', { params: { categorie } })),
  ouvrirLitige: (id: string, motif: string) => data(api.post<Litige>(`/fournisseurs/${id}/litiges`, { motif })),
}

export const reportingApi = {
  kpis: (profil: string, periode = '2026-Q4') =>
    data(api.get<KpiDashboard>('/reporting/kpis', { params: { profil, periode } })),
  series: (kpi: string, granularite = 'MENSUEL') =>
    data(api.get<SeriePoint[]>('/reporting/series', { params: { kpi, granularite } })),
  export: (format: 'pdf' | 'xlsx', periode = '2026-Q4') =>
    data(api.get<ExportReport>('/reporting/export', { params: { format, periode } })),
}

export const adminApi = {
  utilisateurs: (params?: Params) =>
    data(api.get<PageResponse<Utilisateur>>('/admin/utilisateurs', { params: clean(params) })),
  creerUtilisateur: (body: UtilisateurCreation) => data(api.post<Utilisateur>('/admin/utilisateurs', body)),
  changerStatut: (id: string, actif: boolean) =>
    data(api.patch<Utilisateur>(`/admin/utilisateurs/${id}/statut`, { actif })),
}

export const auditApi = {
  list: (params?: Params) => data(api.get<PageResponse<AuditLog>>('/audit', { params: clean(params) })),
}

export const notificationsApi = {
  list: (destinataire?: string) =>
    data(api.get<NotificationCse[]>('/notifications', { params: clean({ destinataire }) })),
  envoyer: (body: NotificationCreation) => data(api.post<NotificationCse>('/notifications', body)),
  marquerLue: (id: string) => data(api.patch<NotificationCse>(`/notifications/${id}/lue`)),
}

export const chatbotApi = {
  query: (question: string) => data(api.post<ChatbotEntry>('/chatbot/query', { question })),
  historique: () => data(api.get<ChatbotEntry[]>('/chatbot/historique')),
  supprimer: (id: string) => data(api.delete<{ deleted: string }>(`/chatbot/historique/${id}`)),
}
