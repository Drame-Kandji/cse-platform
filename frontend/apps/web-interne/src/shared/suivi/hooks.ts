import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { PanneCreation } from '@/shared/domain/types'
import { suiviApi, type AffectationCreation, type MaterielFiltres } from './api'
import { affectationKeys, besoinOptionKeys, chantierKeys, materielKeys, panneKeys } from './keys'

/** Taille maximale autorisee par le backend : suffisante pour les listes de choix. */
const OPTIONS_SIZE = 50

/** Liste paginee du parc materiel (filtres serveur). */
export function useMateriels(params: MaterielFiltres & { page: number; size: number; sort?: string }) {
  return useQuery({
    queryKey: materielKeys.list(params),
    queryFn: ({ signal }) => suiviApi.materiels(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Fiche complete d'un materiel (historiques inclus). */
export function useMaterielDetail(id: string | undefined) {
  return useQuery({
    queryKey: materielKeys.detail(id ?? ''),
    queryFn: ({ signal }) => suiviApi.materiel(id ?? '', signal),
    enabled: Boolean(id),
  })
}

/** Chantiers proposes dans les selects et filtres (par defaut : chantiers en cours). */
export function useChantierOptions(options: { enCoursSeulement?: boolean } = {}) {
  const statut = options.enCoursSeulement ? 'EN_COURS' : undefined
  return useQuery({
    queryKey: chantierKeys.list({ statut, size: OPTIONS_SIZE, usage: 'options' }),
    queryFn: ({ signal }) => suiviApi.chantiers({ statut, size: OPTIONS_SIZE, sort: 'nom,asc' }, signal),
    staleTime: 5 * 60_000,
    select: (page) =>
      page.content.map((chantier) => ({
        value: chantier.id,
        label: chantier.nom,
        description: `${chantier.code} · ${chantier.localisation}`,
      })),
  })
}

/** Besoins valides d'un chantier, rattachables a une affectation. */
export function useBesoinOptions(chantierId: string | null | undefined) {
  return useQuery({
    queryKey: besoinOptionKeys.list({ chantierId, statut: 'VALIDE', usage: 'options' }),
    queryFn: ({ signal }) =>
      suiviApi.besoins({ chantierId: chantierId ?? '', statut: 'VALIDE', size: OPTIONS_SIZE }, signal),
    enabled: Boolean(chantierId),
    select: (page) =>
      page.content.map((besoin) => ({
        value: besoin.id,
        label: `${besoin.reference} — ${besoin.designation}`,
        description: `Quantité : ${String(besoin.quantite)}`,
      })),
  })
}

/**
 * Affectation d'un materiel a un chantier (R04).
 * Erreur geree par le formulaire (conflit 409 affiche en ligne) : `silentError`.
 */
export function useAffecterMateriel() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ materielId, ...body }: AffectationCreation & { materielId: string }) =>
      suiviApi.affecter(materielId, body),
    meta: { successMessage: 'Matériel affecté', silentError: true },
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: materielKeys.all }),
        queryClient.invalidateQueries({ queryKey: affectationKeys.all }),
        queryClient.invalidateQueries({ queryKey: chantierKeys.all }),
      ]),
  })
}

/** Signalement d'une panne (CHEF sur son chantier, RM). */
export function useSignalerPanne() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: PanneCreation) => suiviApi.signalerPanne(body),
    meta: { successMessage: 'Panne signalée' },
    onSuccess: (_panne, body) =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: panneKeys.all }),
        queryClient.invalidateQueries({ queryKey: materielKeys.detail(body.materielId) }),
      ]),
  })
}
