import { FileText } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { labelOf } from '@/shared/domain/labels'
import { formatDate } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import type { DocumentAdministratif } from '@/domain/types'

/** Documents administratifs (deposes par le service achats) et leur expiration. */
export function DocumentsTab({ documents }: { documents: readonly DocumentAdministratif[] }) {
  const { t } = useTranslation('portail')
  if (documents.length === 0) {
    return (
      <Card>
        <EmptyState icon={<FileText aria-hidden />} title={t('entreprise.noDocuments')} />
      </Card>
    )
  }
  return (
    <Card>
      <ul className="divide-y divide-border">
        {documents.map((doc) => (
          <li key={doc.id} className="flex flex-wrap items-center gap-x-4 gap-y-1 px-5 py-3.5">
            <div className="grid min-w-0 flex-1 gap-0.5">
              <span className="text-sm font-medium">{labelOf('typeDocument', doc.type)}</span>
              <span className="truncate text-xs text-muted-foreground">{doc.libelle}</span>
            </div>
            {doc.expire ? (
              <Badge tone="danger">{t('entreprise.expire')}</Badge>
            ) : (
              <span className="text-xs tabular-nums text-muted-foreground">
                {doc.dateExpiration
                  ? t('entreprise.expireLe', { date: formatDate(doc.dateExpiration) })
                  : t('entreprise.sansExpiration')}
              </span>
            )}
          </li>
        ))}
      </ul>
    </Card>
  )
}
