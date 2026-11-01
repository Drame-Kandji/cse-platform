import { Monitor, Moon, Sun } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useTheme, type ThemePreference } from '@/shared/theme/theme-context'
import { IconButton } from '@/shared/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'

const OPTIONS = [
  { value: 'light', icon: Sun },
  { value: 'dark', icon: Moon },
  { value: 'system', icon: Monitor },
] as const

/** Choix du theme : clair, sombre ou systeme (persiste). */
export function ThemeToggle() {
  const { t } = useTranslation()
  const { theme, resolvedTheme, setTheme } = useTheme()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <IconButton
          label={t('theme.label')}
          tooltip={false}
          icon={resolvedTheme === 'dark' ? <Moon /> : <Sun />}
        />
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-44">
        <DropdownMenuLabel>{t('theme.label')}</DropdownMenuLabel>
        <DropdownMenuRadioGroup
          value={theme}
          onValueChange={(value) => {
            setTheme(value as ThemePreference)
          }}
        >
          {OPTIONS.map((option) => (
            <DropdownMenuRadioItem key={option.value} value={option.value} icon={<option.icon />}>
              {t(`theme.${option.value}`)}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
