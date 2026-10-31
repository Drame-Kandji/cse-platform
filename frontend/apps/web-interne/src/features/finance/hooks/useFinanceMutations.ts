import { useMutation, useQueryClient } from '@tanstack/react-query'
import { financeApi } from '../api/finance.api'
import { financeScopes } from '../api/finance.keys'
import type { DecisionEngagement, FactureCreation } from '../api/types'

function useInvalidate() {
  const queryClient = useQueryClient()
  return (scope: 'engagements' | 'factures') =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: financeScopes.synthese() }),
      queryClient.invalidateQueries({
        queryKey: scope === 'engagements' ? financeScopes.engagements() : financeScopes.factures(),
      }),
      queryClient.invalidateQueries({ queryKey: ['contrats'] }),
    ])
}

/** Decision du DAF sur un engagement (R06) : approbation ou refus commente. */
export function useDeciderEngagement() {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: ({ contratId, ...body }: DecisionEngagement & { contratId: string }) =>
      financeApi.deciderEngagement(contratId, body),
    onSuccess: () => invalidate('engagements'),
  })
}

/** Mandatement d'une facture recue (reference de mandat obligatoire). */
export function useMandaterFacture() {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: ({ id, referenceMandat }: { id: string; referenceMandat: string }) =>
      financeApi.mandater(id, referenceMandat),
    meta: { successMessage: 'Facture mandatée' },
    onSuccess: () => invalidate('factures'),
  })
}

/** Paiement d'une facture mandatee. */
export function usePayerFacture() {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (id: string) => financeApi.payer(id),
    meta: { successMessage: 'Paiement enregistré' },
    onSuccess: () => invalidate('factures'),
  })
}

/** Rejet motive d'une facture recue. */
export function useRejeterFacture() {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: ({ id, motif }: { id: string; motif: string }) => financeApi.rejeter(id, motif),
    meta: { successMessage: 'Facture rejetée' },
    onSuccess: () => invalidate('factures'),
  })
}

/** Enregistrement d'une facture recue (erreurs de champs gerees par le formulaire). */
export function useCreerFacture() {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (body: FactureCreation) => financeApi.creerFacture(body),
    meta: { successMessage: 'Facture enregistrée', silentError: true },
    onSuccess: () => invalidate('factures'),
  })
}
