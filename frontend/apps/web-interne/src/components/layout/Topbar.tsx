import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { DEMO_PROFILES, useAuth } from '../../app/auth'
import { navItemForPath } from '../../app/navigation'
import { ROLE_LABELS, ROLE_SHORT, ROLE_TONE } from '../../lib/labels'
import { initials } from '../../lib/format'
import { cn } from '../../lib/cn'
import { Icon } from '../ui/Icon'
import { Badge } from '../ui/Badge'
import { NotificationBell } from './NotificationBell'
import { useToast } from '../ui/Toast'

export function Topbar({ onMenu }: { onMenu: () => void }) {
  const { user, switchRole } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const toast = useToast()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement | null>(null)
  const item = navItemForPath(location.pathname)

  useEffect(() => {
    if (!open) return
    const onClick = (event: MouseEvent) => {
      if (!ref.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [open])

  if (!user) return null

  return (
    <header className="sticky top-0 z-30 flex h-[76px] items-center justify-between gap-3 border-b border-ink-200 bg-white/95 px-4 backdrop-blur lg:px-7">
      <div className="flex min-w-0 items-center gap-3">
        <button
          type="button"
          onClick={onMenu}
          className="grid h-9 w-9 place-items-center rounded-lg border border-ink-200 text-ink-600 lg:hidden"
          aria-label="Ouvrir le menu"
        >
          <Icon name="menu" size={18} />
        </button>
        <div className="min-w-0">
          <nav className="flex items-center gap-1.5 text-[11px] text-ink-400" aria-label="Fil d’Ariane">
            <span className="font-medium text-ink-500">CSE</span>
            <Icon name="chevronRight" size={11} />
            <span className="truncate font-medium text-ink-500">{item?.shortLabel ?? 'Espace interne'}</span>
            {item ? (
              <>
                <Icon name="chevronRight" size={11} />
                <span className="hidden truncate text-brand-700 sm:inline">{ROLE_SHORT[user.role]}</span>
              </>
            ) : null}
          </nav>
          <h2 className="truncate text-sm font-semibold text-brand-950 sm:text-base">{item?.label ?? 'Plateforme interne CSE'}</h2>
        </div>
      </div>

      <div className="flex items-center gap-2 sm:gap-3">
        <div className="hidden items-center gap-2 rounded-full border border-ink-200 bg-white px-3 py-1.5 xl:flex">
          <span className="h-1.5 w-1.5 rounded-full bg-success-500" />
          <span className="text-[11px] font-medium text-ink-500">Gateway 8080 · 2026-Q4</span>
        </div>

        <NotificationBell />

        <div className="relative" ref={ref}>
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            className="flex h-10 items-center gap-2 rounded-lg border border-ink-200 bg-white pl-1.5 pr-2.5 transition hover:border-brand-300"
            aria-expanded={open}
            aria-label="Sélecteur de profil RBAC"
          >
            <span className="grid h-7 w-7 place-items-center rounded-full bg-brand-900 text-[10px] font-bold text-white">
              {initials(user.prenom, user.nom)}
            </span>
            <span className="hidden text-left sm:block">
              <span className="block text-[11px] font-semibold leading-tight text-brand-950">
                {user.prenom} {user.nom}
              </span>
              <span className="block text-[10px] leading-tight text-ink-500">{ROLE_SHORT[user.role]}</span>
            </span>
            <Icon name="chevronDown" size={14} className="text-ink-400" />
          </button>

          {open ? (
            <div className="absolute right-0 top-12 z-[900] w-[340px] max-w-[calc(100vw-2rem)] animate-slide-up overflow-hidden rounded-panel border border-ink-200 bg-white shadow-float">
              <div className="border-b border-ink-100 bg-ink-100/50 px-4 py-3">
                <p className="text-[10px] font-semibold uppercase tracking-widest text-ink-400">Changer de profil RBAC</p>
                <p className="mt-0.5 text-xs text-ink-500">Le menu et les actions s’adaptent immédiatement au profil sélectionné.</p>
              </div>
              <div className="max-h-[400px] overflow-y-auto p-1.5">
                {DEMO_PROFILES.map((profile) => {
                  const active = profile.role === user.role
                  return (
                    <button
                      key={profile.role}
                      type="button"
                      onClick={() => {
                        switchRole(profile.role)
                        setOpen(false)
                        navigate('/')
                        toast.success(`Profil ${ROLE_LABELS[profile.role]} activé`, `${profile.prenom} ${profile.nom} · ${profile.email}`)
                      }}
                      className={cn(
                        'flex w-full items-center gap-3 rounded-lg px-2.5 py-2.5 text-left transition',
                        active ? 'bg-brand-50 ring-1 ring-brand-100' : 'hover:bg-ink-100',
                      )}
                    >
                      <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-brand-900 text-[10px] font-bold text-white">
                        {initials(profile.prenom, profile.nom)}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="flex items-center gap-2">
                          <span className="truncate text-xs font-semibold text-brand-950">
                            {profile.prenom} {profile.nom}
                          </span>
                          {active ? <Badge tone={ROLE_TONE[profile.role]}>Actif</Badge> : null}
                        </span>
                        <span className="mt-0.5 block truncate text-[11px] text-ink-500">{ROLE_LABELS[profile.role]}</span>
                        <span className="block truncate text-[10px] text-ink-400">{profile.email}</span>
                      </span>
                      {!active ? <Icon name="chevronRight" size={14} className="text-ink-300" /> : <Icon name="checkCircle" size={15} className="text-brand-600" />}
                    </button>
                  )
                })}
              </div>
              <div className="border-t border-ink-100 px-4 py-2.5 text-[10px] text-ink-500">
                Démonstration : le backend accepte tous les rôles via le bypass de développement.
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </header>
  )
}
