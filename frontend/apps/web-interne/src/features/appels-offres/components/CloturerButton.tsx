import { Lock } from 'lucide-react'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import { Button } from '@/shared/ui/button'
import type { AppelOffre } from '../api/types'
import { useCloturerAppelOffre } from '../hooks/useAppelOffreActions'
import { useNow } from './decompte'
import { dateLimiteDepassee } from './offre-display'

/** Cloture de l'AO par le RA (confirmation ; leve l'anonymat). */
export function CloturerButton({ ao }: { ao: AppelOffre }) {
  const cloturer = useCloturerAppelOffre(ao.id)
  const now = useNow()
  const anticipee = !dateLimiteDepassee(ao.dateLimite, now)
  return (
    <ConfirmDialog
      tone="danger"
      title="Clôturer l'appel d'offres ?"
      description={
        anticipee
          ? "La date limite n'est pas atteinte : plus aucune offre ne pourra être déposée. La clôture lève l'anonymat des offres et ouvre l'évaluation."
          : "La clôture lève l'anonymat des offres et ouvre l'évaluation. Cette action est irréversible."
      }
      confirmLabel="Clôturer"
      onConfirm={async () => {
        await cloturer.mutateAsync()
      }}
      trigger={
        <Button variant="secondary" leftIcon={<Lock />}>
          Clôturer
        </Button>
      }
    />
  )
}
