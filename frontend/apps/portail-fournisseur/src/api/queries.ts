import { useQuery } from '@tanstack/react-query'
import { appelOffreKeys, contratKeys, entrepriseKeys, factureKeys, offreKeys } from './keys'
import { portailApi } from './portail.api'

/** Appels d'offres ouverts au fournisseur (publies, date limite non atteinte). */
export function useAppelsOffres() {
  return useQuery({
    queryKey: appelOffreKeys.lists(),
    queryFn: ({ signal }) => portailApi.appelsOffres(signal),
  })
}

/** Detail d'un appel d'offres (404 si ferme, d'un autre perimetre ou route absente). */
export function useAppelOffre(id: string) {
  return useQuery({
    queryKey: appelOffreKeys.detail(id),
    queryFn: ({ signal }) => portailApi.appelOffre(id, signal),
  })
}

/** Offres deposees par le fournisseur connecte. */
export function useMesOffres() {
  return useQuery({ queryKey: offreKeys.lists(), queryFn: ({ signal }) => portailApi.mesOffres(signal) })
}

/** Contrats du fournisseur (tous statuts : les onglets filtrent cote client). */
export function useContrats() {
  return useQuery({ queryKey: contratKeys.lists(), queryFn: ({ signal }) => portailApi.contrats(signal) })
}

/** Detail d'un contrat du fournisseur. */
export function useContrat(id: string) {
  return useQuery({
    queryKey: contratKeys.detail(id),
    queryFn: ({ signal }) => portailApi.contrat(id, signal),
  })
}

/** Factures du fournisseur. */
export function useFactures() {
  return useQuery({ queryKey: factureKeys.lists(), queryFn: ({ signal }) => portailApi.factures(signal) })
}

/** Profil de l'entreprise (statut, score, documents). */
export function useProfil() {
  return useQuery({
    queryKey: entrepriseKeys.custom('profil'),
    queryFn: ({ signal }) => portailApi.profil(signal),
  })
}

/** Catalogue de prix du fournisseur. */
export function useCatalogue() {
  return useQuery({
    queryKey: entrepriseKeys.custom('catalogue'),
    queryFn: ({ signal }) => portailApi.catalogue(signal),
  })
}
