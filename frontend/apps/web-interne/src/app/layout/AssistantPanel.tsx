import { Sparkles } from 'lucide-react'
import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { AssistantChat } from '@/features/assistant/components/AssistantChat'
import { useAssistantChat } from '@/features/assistant/hooks/useAssistant'
import { useCurrentUser } from '@/shared/auth/context'
import { BREAKPOINTS, useMediaQuery } from '@/shared/lib/hooks'
import { Button } from '@/shared/ui/button'
import { Sheet, SheetContent } from '@/shared/ui/sheet'
import { useShell } from './shell-context'

/** Bouton flottant d'ouverture de l'assistant (toutes les pages). */
export function AssistantLauncher() {
  const { t } = useTranslation()
  const { setAssistantOpen, assistantOpen } = useShell()
  return (
    <Button
      onClick={() => {
        setAssistantOpen(true)
      }}
      className="cse-assistant-fab fixed bottom-[calc(1rem+env(safe-area-inset-bottom,0px))] right-4 z-30 h-12 rounded-full px-4 shadow-overlay sm:right-6 sm:h-11"
      leftIcon={<Sparkles />}
      aria-label={t('assistant.open')}
      aria-haspopup="dialog"
      aria-expanded={assistantOpen}
    >
      <span className="hidden sm:inline">{t('assistant.open')}</span>
    </Button>
  )
}

/**
 * Panneau lateral de l'assistant IA (plein ecran sur mobile, focus piege par le dialogue).
 * La conversation est conservee entre deux ouvertures.
 */
export function AssistantPanel() {
  const { t } = useTranslation()
  const { assistantOpen, setAssistantOpen } = useShell()
  const user = useCurrentUser()
  const chat = useAssistantChat()
  const isDesktop = useMediaQuery(BREAKPOINTS.lg)
  const onNavigate = useCallback(() => {
    if (!isDesktop) setAssistantOpen(false)
  }, [isDesktop, setAssistantOpen])
  return (
    <Sheet open={assistantOpen} onOpenChange={setAssistantOpen}>
      <SheetContent
        title={t('assistant.title')}
        description={t('assistant.subtitle')}
        size="lg"
        bodyClassName="flex flex-col overflow-hidden p-0"
      >
        <AssistantChat chat={chat} firstName={user.name.split(' ')[0]} onNavigate={onNavigate} />
      </SheetContent>
    </Sheet>
  )
}
