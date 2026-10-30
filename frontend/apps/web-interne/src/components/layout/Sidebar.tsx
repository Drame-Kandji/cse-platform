import { useQuery } from '@tanstack/react-query'
import { NavLink } from 'react-router-dom'
import { besoinsApi, fournisseursApi, materielsApi, achatsApi } from '../../api/endpoints'
import { unwrapList } from '../../api/client'
import { useAuth } from '../../app/auth'
import { navItemsForRole } from '../../app/navigation'
import { cn } from '../../lib/cn'
import { ROLE_SHORT, ROLE_LABELS } from '../../lib/labels'
import { initials } from '../../lib/format'
import { Icon, type IconName } from '../ui/Icon'

type Props = {
  open: boolean
  onClose: () => void
}

export function Sidebar({ open, onClose }: Props) {
  const { user, logout } = useAuth()

  if (!user) return null

  const items = navItemsForRole(user.role)
  const has = (path: string) => items.some((item) => item.path === path)

  const besoinsQuery = useQuery({
    queryKey: ['sidebar', 'besoins'],
    queryFn: () => besoinsApi.list({ statut: 'EN_ATTENTE', size: 100 }),
    enabled: has('/besoins'),
  })
  const materielsQuery = useQuery({
    queryKey: ['sidebar', 'materiels'],
    queryFn: () => materielsApi.list({ size: 100 }),
    enabled: has('/parc'),
  })
  const aoQuery = useQuery({
    queryKey: ['sidebar', 'appels-offres'],
    queryFn: () => achatsApi.appelsOffres({ statut: 'PUBLIE', size: 100 }),
    enabled: has('/achats'),
  })
  const fournisseursQuery = useQuery({
    queryKey: ['sidebar', 'fournisseurs'],
    queryFn: () => fournisseursApi.list({ statut: 'ACTIF', size: 100 }),
    enabled: has('/fournisseurs'),
  })

  const counts: Record<string, number | undefined> = {
    '/besoins': besoinsQuery.data ? unwrapList(besoinsQuery.data).length : undefined,
    '/parc': materielsQuery.data ? unwrapList(materielsQuery.data).length : undefined,
    '/achats': aoQuery.data ? unwrapList(aoQuery.data).length : undefined,
    '/fournisseurs': fournisseursQuery.data ? unwrapList(fournisseursQuery.data).length : undefined,
  }

  return (
    <>
      {open ? (
        <button
          type="button"
          aria-label="Fermer le menu"
          className="fixed inset-0 z-40 bg-brand-950/50 backdrop-blur-sm lg:hidden"
          onClick={onClose}
        />
      ) : null}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-[272px] flex-col bg-brand-950 text-white transition-transform duration-200 lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
        aria-label="Navigation principale"
      >
        <div className="flex h-[76px] items-center gap-3 border-b border-white/10 px-5">
          <div className="relative grid h-10 w-10 place-items-center rounded-xl bg-white text-brand-950 shadow-lg">
            <span className="text-xs font-black tracking-tight">CSE</span>
            <span className="absolute -bottom-1 -right-1 h-3.5 w-3.5 rounded-full border-2 border-brand-950 bg-accent-500" />
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold leading-tight">Compagnie Sahélienne</p>
            <p className="truncate text-[11px] uppercase tracking-widest text-brand-200">d’Entreprises · Sénégal</p>
          </div>
          <button type="button" onClick={onClose} className="ml-auto rounded-lg p-1.5 text-brand-200 hover:bg-white/10 lg:hidden" aria-label="Fermer">
            <Icon name="x" size={18} />
          </button>
        </div>

        <div className="mx-4 mt-4 rounded-xl border border-white/10 bg-white/5 px-3 py-2.5">
          <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-brand-300">Profil actif</p>
          <p className="mt-1 text-sm font-semibold text-white">{ROLE_SHORT[user.role]}</p>
          <p className="text-[11px] text-brand-200">{ROLE_LABELS[user.role]}</p>
        </div>

        <nav className="mt-4 flex-1 space-y-1 overflow-y-auto px-3 pb-4">
          {items.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'group flex min-h-[44px] items-center gap-3 rounded-lg px-3 text-sm transition',
                  isActive
                    ? 'bg-white text-brand-950 shadow-sm'
                    : 'text-brand-100 hover:bg-white/10 hover:text-white',
                )
              }
            >
              {({ isActive }) => (
                <>
                  <Icon
                    name={item.icon as IconName}
                    size={18}
                    className={cn('shrink-0', isActive ? 'text-brand-700' : 'text-brand-300 group-hover:text-accent-400')}
                  />
                  <span className="flex-1 truncate">{item.label}</span>
                  {counts[item.path] !== undefined ? (
                    <span
                      className={cn(
                        'rounded-full px-1.5 py-0.5 text-[10px] font-bold',
                        isActive ? 'bg-brand-900 text-white' : 'bg-white/10 text-brand-100',
                      )}
                    >
                      {counts[item.path]}
                    </span>
                  ) : null}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-white/10 p-3">
          <div className="flex items-center gap-3 rounded-lg px-2 py-2">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-accent-500 text-xs font-bold text-brand-950">
              {initials(user.prenom, user.nom)}
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold">
                {user.prenom} {user.nom}
              </p>
              <p className="truncate text-[11px] text-brand-300">{user.email}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={logout}
            className="mt-1 flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-brand-200 transition hover:bg-white/10 hover:text-white"
          >
            <Icon name="logout" size={17} />
            Se déconnecter
          </button>
          <p className="mt-2 px-3 text-[10px] text-brand-400">CSE Platform v2.0 · Environnement de démonstration</p>
        </div>
      </aside>
    </>
  )
}
