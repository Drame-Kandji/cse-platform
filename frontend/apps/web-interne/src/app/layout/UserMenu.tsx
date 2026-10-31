import { ChevronDown, LogOut, UserRound } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useCurrentUser, useAuth } from '@/shared/auth/context'
import { ROLE_LABELS } from '@/shared/domain/labels'
import { Avatar } from '@/shared/ui/avatar'
import { Badge } from '@/shared/ui/badge'
import { paths } from '../routes'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'

/** Menu utilisateur : identite, role(s) en francais, deconnexion. */
export function UserMenu() {
  const { t } = useTranslation()
  const { logout, mode } = useAuth()
  const user = useCurrentUser()
  const navigate = useNavigate()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        className="flex items-center gap-2.5 rounded-lg p-1 pr-1.5 transition-colors hover:bg-muted data-[state=open]:bg-muted"
        aria-label={t('shell.userMenu')}
      >
        <Avatar name={user.name} size="sm" />
        <span className="hidden min-w-0 truncate text-sm font-medium xl:block">{user.name}</span>
        <ChevronDown className="hidden size-4 text-muted-foreground xl:block" aria-hidden />
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-72">
        <div className="flex items-center gap-3 px-2.5 py-2.5">
          <Avatar name={user.name} />
          <div className="grid min-w-0">
            <p className="truncate text-sm font-semibold">{user.name}</p>
            <p className="truncate text-xs text-muted-foreground">{user.email ?? user.username}</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-1.5 px-2.5 pb-2.5">
          {user.roles.map((role) => (
            <Badge key={role} tone={ROLE_LABELS[role].tone} size="sm">
              {ROLE_LABELS[role].label}
            </Badge>
          ))}
          {mode === 'dev' && (
            <Badge tone="warning" variant="outline" size="sm">
              {t('shell.devMode')}
            </Badge>
          )}
        </div>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          icon={<UserRound />}
          onSelect={() => {
            navigate(paths.profil)
          }}
        >
          {t('shell.profile')}
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
