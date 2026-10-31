import { useState } from 'react'
import { RouterProvider } from 'react-router-dom'
import { ErrorBoundary } from '@/shared/errors/ErrorBoundary'
import { AppProviders } from './providers'
import { createAppRouter } from './router'

/** Racine de l'application. */
export function App() {
  const [router] = useState(createAppRouter)
  return (
    <ErrorBoundary>
      <AppProviders>
        <RouterProvider router={router} future={{ v7_startTransition: true }} />
      </AppProviders>
    </ErrorBoundary>
  )
}
