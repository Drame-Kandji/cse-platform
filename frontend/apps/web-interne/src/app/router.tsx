import { createBrowserRouter, Navigate, type RouteObject } from 'react-router-dom'
import { RequireRole } from '@/shared/auth/RequireRole'
import type { Role } from '@/shared/domain/labels'
import { access } from './access'
import { AppShell } from './layout/AppShell'
import { AuthGate } from './pages/AuthGate'
import { Component as NotFoundPage } from './pages/NotFoundPage'
import { RootLayout } from './pages/RootLayout'
import { RouteErrorPage } from './pages/RouteErrorPage'
import type { CrumbKey, RouteHandle } from './route-handle'
import { paths } from './routes'

type PageModule = Promise<{ Component: React.ComponentType }>

/** Route paresseuse : le module de page exporte `Component` (code-splitting automatique). */
function page(path: string | undefined, load: () => PageModule, crumb?: CrumbKey): RouteObject {
  const handle: RouteHandle = { crumb }
  return path === undefined ? { index: true, lazy: load, handle } : { path, lazy: load, handle }
}

/** Groupe de routes protege par roles (RequireRole rend l'Outlet). */
function guarded(roles: readonly Role[], children: RouteObject[]): RouteObject {
  return { element: <RequireRole roles={roles} />, children }
}

/** Section avec fil d'Ariane parent (ex. Besoins > Detail). */
function section(path: string, crumb: CrumbKey, children: RouteObject[]): RouteObject {
  const handle: RouteHandle = { crumb }
  return { path, handle, children }
}

const appRoutes: RouteObject[] = [
  page(paths.dashboard, () => import('@/features/reporting/pages/DashboardPage'), 'items.dashboard'),
  page(
    paths.notifications,
    () => import('@/features/notifications/pages/NotificationsPage'),
    'items.notifications',
  ),
  page(paths.profil, () => import('@/features/profil/pages/ProfilPage'), 'items.profil'),
  guarded(access.besoins, [
    section(paths.besoins, 'items.besoins', [
      page(undefined, () => import('@/features/besoins/pages/BesoinsListPage')),
      guarded(access.besoinNouveau, [
        page(
          paths.besoinNouveau,
          () => import('@/features/besoins/pages/BesoinCreatePage'),
          'items.besoinNouveau',
        ),
        page(
          paths.besoinModifier,
          () => import('@/features/besoins/pages/BesoinEditPage'),
          'crumbs.modifier',
        ),
      ]),
      page(paths.besoinDetail, () => import('@/features/besoins/pages/BesoinDetailPage'), 'crumbs.detail'),
    ]),
  ]),
  guarded(access.validation, [
    page(paths.validation, () => import('@/features/besoins/pages/ValidationPage'), 'items.validation'),
  ]),
  guarded(access.materiels, [
    section(paths.materiels, 'items.materiels', [
      page(undefined, () => import('@/features/materiels/pages/MaterielsListPage')),
      page(
        paths.materielDetail,
        () => import('@/features/materiels/pages/MaterielDetailPage'),
        'crumbs.detail',
      ),
    ]),
  ]),
  guarded(access.carte, [page(paths.carte, () => import('@/features/carte/pages/CartePage'), 'items.carte')]),
  guarded(access.planning, [
    page(paths.planning, () => import('@/features/planning/pages/PlanningPage'), 'items.planning'),
  ]),
  guarded(access.chantiers, [
    page(paths.chantiers, () => import('@/features/chantiers/pages/ChantiersPage'), 'items.chantiers'),
  ]),
  guarded(access.maintenances, [
    page(
      paths.maintenances,
      () => import('@/features/maintenances/pages/MaintenancesPage'),
      'items.maintenances',
    ),
  ]),
  guarded(access.appelsOffres, [
    section(paths.appelsOffres, 'items.appelsOffres', [
      page(undefined, () => import('@/features/appels-offres/pages/AppelsOffresListPage')),
      page(
        paths.appelOffreDetail,
        () => import('@/features/appels-offres/pages/AppelOffreDetailPage'),
        'crumbs.detail',
      ),
    ]),
  ]),
  guarded(access.contrats, [
    section(paths.contrats, 'items.contrats', [
      page(undefined, () => import('@/features/contrats/pages/ContratsPage')),
      page(paths.contratDetail, () => import('@/features/contrats/pages/ContratDetailPage'), 'crumbs.detail'),
    ]),
  ]),
  guarded(access.fournisseurs, [
    section(paths.fournisseurs, 'items.fournisseurs', [
      page(undefined, () => import('@/features/fournisseurs/pages/FournisseursListPage')),
      page(
        paths.fournisseurDetail,
        () => import('@/features/fournisseurs/pages/FournisseurDetailPage'),
        'crumbs.detail',
      ),
    ]),
  ]),
  guarded(access.finance, [
    page(paths.finance, () => import('@/features/finance/pages/FinancePage'), 'items.finance'),
  ]),
  guarded(access.rapports, [
    page(paths.rapports, () => import('@/features/reporting/pages/RapportsPage'), 'items.rapports'),
  ]),
  guarded(access.admin, [
    section(paths.admin, 'crumbs.admin', [
      { index: true, element: <Navigate to={paths.adminUtilisateurs} replace /> },
      page(
        paths.adminUtilisateurs,
        () => import('@/features/admin/pages/UtilisateursPage'),
        'items.utilisateurs',
      ),
      page(
        paths.adminReferentiels,
        () => import('@/features/admin/pages/ReferentielsPage'),
        'items.referentiels',
      ),
      page(paths.adminAudit, () => import('@/features/admin/pages/AuditPage'), 'items.audit'),
      page(paths.adminPlateforme, () => import('@/features/admin/pages/PlateformePage'), 'items.plateforme'),
    ]),
  ]),
  { path: '*', Component: NotFoundPage },
]

/** Vitrine du design system : enregistree uniquement en developpement (absente du build). */
const devRoutes: RouteObject[] = import.meta.env.DEV
  ? [page(paths.design, () => import('./showcase/DesignPage'))]
  : []

export const routeTree: RouteObject[] = [
  {
    element: <RootLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      ...devRoutes,
      {
        element: <AuthGate />,
        children: [
          { element: <AppShell />, children: [{ errorElement: <RouteErrorPage />, children: appRoutes }] },
        ],
      },
    ],
  },
]

/** Cree le routeur de donnees de l'application. */
export function createAppRouter() {
  return createBrowserRouter(routeTree, {
    future: {
      v7_relativeSplatPath: true,
      v7_fetcherPersist: true,
      v7_normalizeFormMethod: true,
      v7_partialHydration: true,
      v7_skipActionErrorRevalidation: true,
    },
  })
}
