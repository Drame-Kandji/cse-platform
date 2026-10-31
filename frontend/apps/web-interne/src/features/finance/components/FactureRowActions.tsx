import { Banknote, FileCheck2, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { formatMoney } from '@/shared/lib/format'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import { Button } from '@/shared/ui/button'
import type { Facture } from '../api/types'
import { usePayerFacture } from '../hooks/useFinanceMutations'

interface Props {
  facture: Facture
  onMandater: (facture: Facture) => void
  onRejeter: (facture: Facture) => void
}

/** Actions DAF selon le statut : RECUE -> mandater/rejeter, MANDATEE -> payer. */
export function FactureRowActions({ facture, onMandater, onRejeter }: Props) {
  const { t } = useTranslation('finance')
  const payer = usePayerFacture()
  if (facture.statut === 'RECUE') {
    return (
      <div
        className="flex justify-end gap-2"
        role="group"
        aria-label={t('factures.actions', { numero: facture.numero })}
      >
        <Button
          size="sm"
          variant="secondary"
          leftIcon={<X />}
          onClick={() => {
            onRejeter(facture)
          }}
        >
          {t('factures.rejeter')}
        </Button>
        <Button
          size="sm"
          leftIcon={<FileCheck2 />}
          onClick={() => {
            onMandater(facture)
          }}
        >
          {t('factures.mandater')}
        </Button>
      </div>
    )
  }
  if (facture.statut === 'MANDATEE') {
    return (
      <div className="flex justify-end">
        <ConfirmDialog
          title={t('factures.payerTitle')}
          description={t('factures.payerDescription', {
            numero: facture.numero,
            montant: formatMoney(facture.montantTtc),
          })}
          confirmLabel={t('factures.payer')}
          onConfirm={async () => {
            await payer.mutateAsync(facture.id)
          }}
          trigger={
            <Button size="sm" leftIcon={<Banknote />}>
              {t('factures.payer')}
            </Button>
          }
        />
      </div>
    )
  }
  return null
}
