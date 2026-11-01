import { useTranslation } from 'react-i18next'
import { formatMoney, formatNumber } from '@/shared/lib/format'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import { useSignerContrat } from '@/api/mutations'
import type { Contrat } from '@/domain/types'

/** Confirmation de signature : rappelle l'objet, le montant et le delai de livraison. */
export function SignerDialog({
  contrat,
  open,
  onOpenChange,
}: {
  contrat: Contrat
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation('portail')
  const signer = useSignerContrat(contrat.id)
  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t('contrats.signer.title', { numero: contrat.numero })}
      description={t('contrats.signer.description', {
        quantite: formatNumber(contrat.quantite),
        designation: contrat.designation,
        montant: formatMoney(contrat.montant),
        delai: contrat.delaiLivraisonJours,
      })}
      confirmLabel={t('contrats.signer.confirm')}
      onConfirm={async () => {
        await signer.mutateAsync().catch(() => undefined)
      }}
    />
  )
}
