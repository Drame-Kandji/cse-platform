import { Download, FileText } from 'lucide-react'
import { labelOf, TYPE_PROCES_VERBAL, type StatutAO, type TypeProcesVerbal } from '@/shared/domain/labels'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import type { AppelOffre } from '../api/types'
import { useTelechargerPv } from '../hooks/useAppelOffreActions'

const DISPONIBILITE: Record<TypeProcesVerbal, { disponible: (s: StatutAO) => boolean; aide: string }> = {
  OUVERTURE: {
    disponible: (s) => s !== 'PUBLIE',
    aide: "Établi à la clôture : liste des offres reçues et levée de l'anonymat.",
  },
  EVALUATION: {
    disponible: (s) => s === 'ATTRIBUE',
    aide: 'Établi après attribution : notes, classement et décision motivée.',
  },
}

/** Onglet Proces-verbaux : telechargement des PDF d'ouverture et d'evaluation. */
export function ProcesVerbauxTab({ ao }: { ao: AppelOffre }) {
  const telecharger = useTelechargerPv(ao.id, ao.reference)
  return (
    <ul className="grid gap-3 md:grid-cols-2">
      {TYPE_PROCES_VERBAL.map((type) => {
        const regle = DISPONIBILITE[type]
        const disponible = regle.disponible(ao.statut)
        const enCours = telecharger.isPending && telecharger.variables === type
        return (
          <li key={type}>
            <Card className="flex h-full flex-col gap-3 p-5">
              <div className="flex items-start gap-3">
                <span className="grid size-10 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground">
                  <FileText className="size-5" aria-hidden />
                </span>
                <div className="grid gap-1">
                  <p className="text-sm font-semibold">{labelOf('typeProcesVerbal', type)}</p>
                  <p className="text-sm text-muted-foreground">{regle.aide}</p>
                </div>
              </div>
              <div className="mt-auto flex flex-wrap items-center gap-3">
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
                {!disponible && (
                  <span className="text-xs text-muted-foreground">
                    {type === 'OUVERTURE' ? 'Disponible après la clôture' : "Disponible après l'attribution"}
                  </span>
                )}
              </div>
            </Card>
          </li>
        )
      })}
    </ul>
  )
}
