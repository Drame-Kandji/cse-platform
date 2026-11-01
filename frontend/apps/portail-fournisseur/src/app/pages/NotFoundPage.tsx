import { Compass } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Button } from '@/shared/ui/button'
import { StatusPage } from '@/shared/ui/status-page'
import { paths } from '../routes'

/** Page 404 (dans la coquille). */
export function Component() {
  const { t } = useTranslation('portail')
  return (
    <StatusPage
      code="404"
      icon={<Compass aria-hidden />}
      title={t('notFound.title')}
      description={t('notFound.description')}
      actions={
        <Button asChild>
          <Link to={paths.accueil}>{t('notFound.home')}</Link>
        </Button>
      }
    />
  )
}
