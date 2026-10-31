import { Outlet, ScrollRestoration } from 'react-router-dom'

/** Racine du routeur : restauration du defilement entre pages. */
export function RootLayout() {
  return (
    <>
      <ScrollRestoration />
      <Outlet />
    </>
  )
}
