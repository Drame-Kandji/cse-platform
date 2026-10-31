import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { PageParams } from '@/shared/api'
import { maintenancesApi } from '../api/maintenances.api'
import { maintenanceKeys, panneKeys } from '../api/maintenances.keys'
import type {
  ClotureMaintenance,
  MaintenanceCreation,
  MaintenanceFiltres,
  PanneCreation,
  PanneFiltres,
} from '../api/types'

/** Ordres de travail pagines. */
export function useMaintenances(params: MaintenanceFiltres & PageParams) {
  return useQuery({
    queryKey: maintenanceKeys.list(params),
    queryFn: ({ signal }) => maintenancesApi.lister(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Pannes paginees (un chef ne voit que celles de ses chantiers). */
export function usePannes(params: PanneFiltres & PageParams) {
  return useQuery({
    queryKey: panneKeys.list(params),
    queryFn: ({ signal }) => maintenancesApi.pannes(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Recherche serveur de materiels (selecteur). */
export function useRechercheMateriels(q: string, enabled: boolean) {
  return useQuery({
    queryKey: ['materiels', 'list', { q, size: 20, sort: 'code,asc' }] as const,
    queryFn: ({ signal }) => maintenancesApi.rechercherMateriels(q, signal),
    enabled,
    placeholderData: keepPreviousData,
  })
}

/** Invalide OT, pannes et materiels (l'etat du materiel change avec la maintenance). */
function useInvalider() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: maintenanceKeys.all })
    void queryClient.invalidateQueries({ queryKey: panneKeys.all })
    void queryClient.invalidateQueries({ queryKey: ['materiels'] })
  }
}

/** Planification d'un ordre de travail (RM), eventuellement depuis une panne. */
export function usePlanifierMaintenance() {
  const invalider = useInvalider()
  return useMutation({
    mutationFn: (body: MaintenanceCreation) => maintenancesApi.planifier(body),
    meta: { successMessage: 'Maintenance planifiée', silentError: true },
    onSuccess: invalider,
  })
}

/** Demarrage d'un OT : le materiel passe EN_MAINTENANCE. */
export function useDemarrerMaintenance() {
  const invalider = useInvalider()
  return useMutation({
    mutationFn: (id: string) => maintenancesApi.demarrer(id),
    meta: { successMessage: 'Maintenance démarrée' },
    onSuccess: invalider,
  })
}

/** Cloture d'un OT avec cout et observations. */
export function useCloturerMaintenance() {
  const invalider = useInvalider()
  return useMutation({
    mutationFn: ({ id, ...body }: ClotureMaintenance & { id: string }) => maintenancesApi.cloturer(id, body),
    meta: { successMessage: 'Maintenance clôturée', silentError: true },
    onSuccess: invalider,
  })
}

/** Signalement d'une panne (CHEF, RM). */
export function useSignalerPanne() {
  const invalider = useInvalider()
  return useMutation({
    mutationFn: (body: PanneCreation) => maintenancesApi.signaler(body),
    meta: { successMessage: 'Panne signalée', silentError: true },
    onSuccess: invalider,
  })
}
