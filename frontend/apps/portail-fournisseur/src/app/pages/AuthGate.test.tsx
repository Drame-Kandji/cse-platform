import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { fournisseurUser, renderPortail } from '@/test/render'
import { AuthGate } from './AuthGate'

function rendre(user: ReturnType<typeof fournisseurUser> | null) {
  return renderPortail(
    <Routes>
      <Route element={<AuthGate />}>
        <Route path="/" element={<p>Portail</p>} />
      </Route>
    </Routes>,
    { user },
  )
}

describe('AuthGate', () => {
  it('fournisseur : acces au portail', () => {
    rendre(fournisseurUser())
    expect(screen.getByText('Portail')).toBeInTheDocument()
  })
  it('autre role : acces reserve aux fournisseurs avec deconnexion', () => {
    rendre(fournisseurUser({ realmRoles: ['RESPONSABLE_ACHATS'], roles: ['RESPONSABLE_ACHATS'] }))
    expect(screen.getByRole('heading', { name: 'Accès réservé aux fournisseurs' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Se déconnecter' })).toBeInTheDocument()
    expect(screen.queryByText('Portail')).not.toBeInTheDocument()
  })
  it('non connecte : page de connexion avec lien d inscription', () => {
    rendre(null)
    expect(screen.getByRole('button', { name: 'Se connecter' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Inscrire mon entreprise' })).toHaveAttribute(
      'href',
      '/inscription',
    )
  })
})
