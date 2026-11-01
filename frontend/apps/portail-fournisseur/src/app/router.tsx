import { createBrowserRouter, Outlet, ScrollRestoration, type RouteObject } from 'react-router-dom'
import { PortalShell } from './layout/PortalShell'
import { AuthGate } from './pages/AuthGate'
import { Component as NotFoundPage } from './pages/NotFoundPage'
import { RouteErrorPage } from './pages/RouteErrorPage'
import { paths } from './routes'

type PageModule = Promise<{ Component: React.ComponentType }>

/** Route paresseuse : le module de page exporte `Component`. */
function page(path: string, load: () => PageModule): RouteObject {
  return { path, lazy: load }
}

const portalRoutes: RouteObject[] = [
  page(paths.accueil, () => import('@/features/accueil/pages/AccueilPage')),
  page(paths.appelsOffres, () => import('@/features/appels-offres/pages/AppelsOffresPage')),
  page(paths.appelOffreDetail, () => import('@/features/appels-offres/pages/AppelOffreDetailPage')),
  page(paths.contrats, () => import('@/features/contrats/pages/ContratsPage')),
  page(paths.contratDetail, () => import('@/features/contrats/pages/ContratDetailPage')),
  page(paths.factures, () => import('@/features/factures/pages/FacturesPage')),
  page(paths.entreprise, () => import('@/features/entreprise/pages/EntreprisePage')),
  { path: '*', Component: NotFoundPage },
]

export const routeTree: RouteObject[] = [
  {
    element: (
      <>
        <ScrollRestoration />
        <Outlet />
      </>
    ),
    errorElement: <RouteErrorPage />,
    children: [
      // Inscription publique : hors garde d'authentification.
      page(paths.inscription, () => import('@/features/inscription/pages/InscriptionPage')),
      {
        element: <AuthGate />,
        children: [
          {
            element: <PortalShell />,
            children: [{ errorElement: <RouteErrorPage />, children: portalRoutes }],
          },
        ],
      },
    ],
  },
]

/** Routeur de donnees du portail. */
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
