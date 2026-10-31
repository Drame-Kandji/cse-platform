import { createQueryKeys } from '@/shared/api'

/** Cles de requete du module finance. */
export const financeKeys = createQueryKeys('finance')

/** Sous-cles : synthese, engagements, factures, contrats selectionnables. */
export const financeScopes = {
  synthese: () => financeKeys.custom('synthese'),
  engagements: () => financeKeys.custom('engagements'),
  engagementList: (params: object) => financeKeys.custom('engagements', params),
  factures: () => financeKeys.custom('factures'),
  factureList: (params: object) => financeKeys.custom('factures', params),
  contrats: () => financeKeys.custom('contrats'),
}
