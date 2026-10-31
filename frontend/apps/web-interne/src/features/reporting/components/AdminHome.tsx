import { Activity, History, Library, Users } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { paths } from '@/app/routes'
import { Card } from '@/shared/ui/card'

const ADMIN_SHORTCUTS: readonly { title: string; description: string; href: string; icon: ReactNode }[] = [
  {
    title: 'Utilisateurs',
    description: 'Comptes, rôles et mots de passe',
    href: paths.adminUtilisateurs,
    icon: <Users />,
  },
  {
    title: 'Référentiels',
    description: "Types d'engins et seuils de maintenance",
    href: paths.adminReferentiels,
    icon: <Library />,
  },
  {
    title: "Journal d'audit",
    description: 'Traçabilité des actions sensibles',
    href: paths.adminAudit,
    icon: <History />,
  },
  {
    title: 'État de la plateforme',
    description: 'Santé et latence des services',
    href: paths.adminPlateforme,
    icon: <Activity />,
  },
]

export /** Accueil de l'administrateur systeme (pas de profil de reporting) : acces rapides. */
function AdminHome() {
  return (
    <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {ADMIN_SHORTCUTS.map((item) => (
        <li key={item.href}>
          <Link
            to={item.href}
            className="block h-full rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <Card interactive className="flex h-full flex-col gap-3 p-5">
              <span className="grid size-10 place-items-center rounded-lg bg-primary/10 text-primary-emphasis [&_svg]:size-5">
                {item.icon}
              </span>
              <span className="grid gap-1">
                <span className="text-sm font-semibold">{item.title}</span>
                <span className="text-sm text-muted-foreground">{item.description}</span>
              </span>
            </Card>
          </Link>
        </li>
      ))}
    </ul>
  )
}
