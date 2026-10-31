import { Compass } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Button } from '@/shared/ui/button'
import { StatusPage } from '@/shared/ui/status-page'
import { paths } from '../routes'

/** Page 404 (dans la coquille). */
export function Component() {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation()
  return (
    <StatusPage
      code={t('notFound.code')}
      icon={<Compass aria-hidden />}
      title={t('notFound.title')}
      description={t('notFound.description')}
      actions={
        <Button asChild>
          <Link to={paths.dashboard}>{tc('actions.goHome')}</Link>
        </Button>
      }
    />
  )
}
