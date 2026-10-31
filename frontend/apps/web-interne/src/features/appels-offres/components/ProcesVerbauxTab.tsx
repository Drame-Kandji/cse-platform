import { Download } from 'lucide-react'
import { labelOf, TYPE_PROCES_VERBAL, type StatutAO, type TypeProcesVerbal } from '@/shared/domain/labels'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import type { AppelOffre } from '../api/types'
import { useTelechargerPv } from '../hooks/useAppelOffreActions'

const DISPONIBILITE: Record<TypeProcesVerbal, (s: StatutAO) => boolean> = {
  OUVERTURE: (s) => s !== 'PUBLIE',
  EVALUATION: (s) => s === 'ATTRIBUE',
}

/** Onglet Proces-verbaux : telechargement des PDF d'ouverture et d'evaluation. */
export function ProcesVerbauxTab({ ao }: { ao: AppelOffre }) {
  const telecharger = useTelechargerPv(ao.id, ao.reference)
  return (
    <Card>
      <ul className="divide-y divide-border">
        {TYPE_PROCES_VERBAL.map((type) => {
          const disponible = DISPONIBILITE[type](ao.statut)
          const enCours = telecharger.isPending && telecharger.variables === type
          return (
            <li key={type} className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:px-5">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">{labelOf('typeProcesVerbal', type)}</p>
                {!disponible && (
                  <p className="text-xs text-muted-foreground">
                    {type === 'OUVERTURE' ? 'Disponible après la clôture' : "Disponible après l'attribution"}
                  </p>
                )}
              </div>
              <Button
                variant="secondary"
                size="sm"
                leftIcon={<Download />}
                loading={enCours}
                disabled={!disponible || enCours}
                onClick={() => {
                  telecharger.mutate(type)
                }}
              >
                Télécharger le PDF
              </Button>
            </li>
          )
        })}
      </ul>
    </Card>
  )
}
