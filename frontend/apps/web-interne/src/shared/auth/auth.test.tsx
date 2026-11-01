import { act, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, testUser } from '@/test/render'
import { AuthProvider } from './AuthProvider'
import { Can } from './Can'
import { hasAnyRole, userFromClaims } from './claims'
import { useAuth } from './context'
import { RequireRole } from './RequireRole'
import type { AuthDriver, AuthUser } from './types'

describe('RequireRole', () => {
  it('affiche le contenu si le role est autorise', () => {
    renderWithProviders(
      <RequireRole roles={['DAF', 'DIRECTION_GENERALE']}>
        <p>Espace finance</p>
      </RequireRole>,
      { user: testUser(['DAF']) },
    )
    expect(screen.getByText('Espace finance')).toBeInTheDocument()
  })

  it('affiche la page 403 avec les roles autorises sinon', () => {
    renderWithProviders(
      <RequireRole roles={['DAF']}>
        <p>Espace finance</p>
      </RequireRole>,
      { user: testUser(['CHEF_CHANTIER']) },
    )
    expect(screen.queryByText('Espace finance')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Accès refusé' })).toBeInTheDocument()
    expect(screen.getByText('DAF')).toBeInTheDocument()
  })

  it('autorise tout utilisateur connecte si la liste est vide', () => {
    renderWithProviders(
      <RequireRole roles={[]}>
        <p>Tableau de bord</p>
      </RequireRole>,
      { user: testUser(['ADMIN_SYSTEME']) },
    )
    expect(screen.getByText('Tableau de bord')).toBeInTheDocument()
  })

  it("ne rend rien tant que l'utilisateur n'est pas authentifie", () => {
    const { container } = renderWithProviders(
      <RequireRole roles={[]}>
        <p>Secret</p>
      </RequireRole>,
      { user: null },
    )
    expect(container).toBeEmptyDOMElement()
  })
})

describe('Can', () => {
  it('masque une action non autorisee et affiche le fallback', () => {
    renderWithProviders(
      <Can roles={['RESPONSABLE_MATERIEL']} fallback={<span>Lecture seule</span>}>
        <button type="button">Valider</button>
      </Can>,
      { user: testUser(['CHEF_CHANTIER']) },
    )
    expect(screen.queryByRole('button', { name: 'Valider' })).not.toBeInTheDocument()
    expect(screen.getByText('Lecture seule')).toBeInTheDocument()
  })
})

describe('claims', () => {
  it('extrait identite, roles CSE et chantiers du jeton', () => {
    const user = userFromClaims({
      sub: 'abc',
      given_name: 'Fatou',
      family_name: 'Sarr',
      preferred_username: 'f.sarr',
      email: 'f.sarr@cse.sn',
      realm_access: { roles: ['offline_access', 'RESPONSABLE_MATERIEL', 'uma_authorization'] },
      chantier_ids: 'ch-1',
    })
    expect(user).toEqual({
      id: 'abc',
      username: 'f.sarr',
      name: 'Fatou Sarr',
      email: 'f.sarr@cse.sn',
      roles: ['RESPONSABLE_MATERIEL'],
      chantierIds: ['ch-1'],
      realmRoles: ['offline_access', 'RESPONSABLE_MATERIEL', 'uma_authorization'],
    })
  })
  it('expose le fournisseur rattache et les roles hors plateforme interne', () => {
    const user = userFromClaims({
      sub: 'f',
      realm_access: { roles: ['FOURNISSEUR'] },
      fournisseur_id: 'f0000000-0000-0000-0000-000000000001',
    })
    expect(user.roles).toEqual([])
    expect(user.realmRoles).toEqual(['FOURNISSEUR'])
    expect(user.fournisseurId).toBe('f0000000-0000-0000-0000-000000000001')
  })
  it('tolere l absence de roles et de chantiers', () => {
    expect(userFromClaims({ sub: 'x' })).toMatchObject({ name: 'x', roles: [], chantierIds: [] })
  })
  it('rejette un jeton sans sub', () => {
    expect(() => userFromClaims({ name: 'X' })).toThrow()
  })
  it('hasAnyRole', () => {
    expect(hasAnyRole(['DAF'], [])).toBe(true)
    expect(hasAnyRole(['DAF'], ['DAF', 'ADMIN_SYSTEME'])).toBe(true)
    expect(hasAnyRole(['DAF'], ['ADMIN_SYSTEME'])).toBe(false)
  })
})

describe('AuthProvider', () => {
  function fakeDriver(initial: AuthUser | null): AuthDriver {
    return {
      mode: 'keycloak',
      init: () => Promise.resolve(initial),
      login: () => Promise.resolve(),
      logout: () => Promise.resolve(),
      getToken: () => Promise.resolve('t'),
      subscribe: () => () => undefined,
    }
  }
  function Probe() {
    const auth = useAuth()
    return (
      <p>
        {auth.status === 'authenticated'
          ? `${auth.user?.name ?? ''} ${String(auth.hasAnyRole(['DAF']))}`
          : auth.status}
      </p>
    )
  }

  it('passe de loading a authenticated avec le pilote', async () => {
    const driver = fakeDriver({
      id: '1',
      username: 'a',
      name: 'Aminata Diallo',
      email: undefined,
      roles: ['DAF'],
      chantierIds: [],
    })
    render(
      <AuthProvider driverFactory={() => Promise.resolve(driver)}>
        <Probe />
      </AuthProvider>,
    )
    expect(screen.getByText('loading')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByText('Aminata Diallo true')).toBeInTheDocument()
    })
  })

  it('expose une erreur si le pilote echoue', async () => {
    await act(async () => {
      render(
        <AuthProvider driverFactory={() => Promise.reject(new Error('Keycloak injoignable'))}>
          <Probe />
        </AuthProvider>,
      )
      await Promise.resolve()
    })
    expect(await screen.findByText('error')).toBeInTheDocument()
  })
})
