import { ArrowLeft, ShieldAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { ROLE_SHORT_LABELS, type Role } from '@/shared/domain/labels'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { StatusPage } from '@/shared/ui/status-page'

/** Ecran 403 : affiche les roles autorises pour faciliter la demande d'acces. */
export function ForbiddenView({ roles = [] }: { roles?: readonly Role[] }) {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation()
  const navigate = useNavigate()
  return (
    <StatusPage
      code={t('forbidden.code')}
      icon={<ShieldAlert aria-hidden />}
      title={t('forbidden.title')}
      description={t('forbidden.description')}
      actions={
        <>
          <Button
            variant="secondary"
            leftIcon={<ArrowLeft />}
            onClick={() => {
              navigate(-1)
            }}
          >
            {tc('actions.back')}
          </Button>
          <Button asChild>
            <Link to="/">{tc('actions.goHome')}</Link>
          </Button>
        </>
      }
    >
      {roles.length > 0 && (
        <div className="mt-6 grid justify-items-center gap-2">
          <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {t('forbidden.requiredRoles')}
          </p>
          <div className="flex flex-wrap justify-center gap-1.5">
            {roles.map((role) => (
              <Badge key={role} tone="neutral">
                {ROLE_SHORT_LABELS[role]}
              </Badge>
            ))}
          </div>
        </div>
      )}
    </StatusPage>
  )
}
