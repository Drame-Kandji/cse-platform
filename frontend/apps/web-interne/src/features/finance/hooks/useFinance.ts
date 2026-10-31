import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api'
import { financeApi } from '../api/finance.api'
import { financeScopes } from '../api/finance.keys'
import type { EngagementFiltres, FactureFiltres } from '../api/types'

/** Synthese financiere (montants engages, factures, mandates, payes). */
export function useSyntheseFinance() {
  return useQuery({
    queryKey: financeScopes.synthese(),
    queryFn: ({ signal }) => financeApi.synthese(signal),
  })
}

/** Liste paginee des engagements budgetaires. */
export function useEngagements(params: EngagementFiltres & PageParams) {
  return useQuery({
    queryKey: financeScopes.engagementList(params),
    queryFn: ({ signal }) => financeApi.engagements(params, signal),
    placeholderData: keepPreviousData,
  })
}

/**
 * Liste paginee des factures.
 * @param enabled faux pour les roles sans acces aux factures (DG)
 */
export function useFactures(params: FactureFiltres & PageParams, enabled = true) {
  return useQuery({
    queryKey: financeScopes.factureList(params),
    queryFn: ({ signal }) => financeApi.factures(params, signal),
    placeholderData: keepPreviousData,
    enabled,
  })
}

/** Contrats selectionnables pour l'enregistrement d'une facture (charge a l'ouverture). */
export function useContratsFacturables(enabled: boolean) {
  return useQuery({
    queryKey: financeScopes.contrats(),
    queryFn: ({ signal }) => financeApi.contrats(signal),
    enabled,
  })
}
