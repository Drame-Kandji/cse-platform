import { Building2, ChevronDown, LogOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useAuth, useCurrentUser } from '@/shared/auth/context'
import { Avatar } from '@/shared/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'
import { paths } from '../routes'

/** Menu utilisateur : identite, entreprise, deconnexion. */
export function UserMenu() {
  const { t } = useTranslation('portail')
  const { logout } = useAuth()
  const user = useCurrentUser()
  const navigate = useNavigate()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        className="flex items-center gap-2.5 rounded-lg p-1 pr-1.5 transition-colors hover:bg-muted data-[state=open]:bg-muted"
        aria-label={t('shell.userMenu')}
      >
        <Avatar name={user.name} size="sm" />
        <span className="hidden max-w-40 truncate text-sm font-medium xl:block">{user.name}</span>
        <ChevronDown className="hidden size-4 text-muted-foreground xl:block" aria-hidden />
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-64">
        <div className="flex items-center gap-3 px-2.5 py-2.5">
          <Avatar name={user.name} />
          <div className="grid min-w-0">
            <p className="truncate text-sm font-semibold">{user.name}</p>
            <p className="truncate text-xs text-muted-foreground">{user.email ?? user.username}</p>
          </div>
        </div>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          icon={<Building2 />}
          onSelect={() => {
            navigate(paths.entreprise)
          }}
        >
          {t('nav.entreprise')}
        </DropdownMenuItem>
        <DropdownMenuItem
          icon={<LogOut />}
          destructive
          onSelect={() => {
            void logout()
          }}
        >
          {t('shell.logout')}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
