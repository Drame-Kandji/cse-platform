import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { describeStatus } from '@/shared/domain/labels'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import type { AppelOffre, Offre } from '../api/types'
import { useEvaluerOffre } from '../hooks/useAppelOffreActions'
import { evaluationOffreSchema, type EvaluationOffreInput } from '../schemas/appel-offre.schema'
import { NumberInput } from './NumberInput'
import { criteresManuels, nomOffre } from './offre-display'

interface Props {
  ao: AppelOffre
  offre: Offre | null
  onClose: () => void
}

/** Notation 0..100 des criteres manuels d'une offre (PRIX et DELAI sont calcules). */
export function EvaluerOffreDialog({ ao, offre, onClose }: Props) {
  return (
    <Dialog open={offre !== null} onOpenChange={(open) => !open && onClose()}>
      {offre && <Contenu key={offre.id} ao={ao} offre={offre} onClose={onClose} />}
    </Dialog>
  )
}

function Contenu({ ao, offre, onClose }: { ao: AppelOffre; offre: Offre; onClose: () => void }) {
  const evaluer = useEvaluerOffre(ao.id)
  const codes = criteresManuels(ao)
  const form = useForm<EvaluationOffreInput>({
    resolver: zodResolver(evaluationOffreSchema),
    defaultValues: Object.fromEntries(codes.map((code) => [code, offre.notes[code]])),
  })
  const submit = form.handleSubmit(async (values) => {
    try {
      await evaluer.mutateAsync({ offreId: offre.id, notes: values })
      onClose()
    } catch (error) {
      applyServerErrors(error, form.setError)
    }
  })
  return (
    <DialogContent
      title={`Évaluer l'offre ${nomOffre(offre, ao.statut)}`}
      description="Notes de 0 à 100. Le prix et le délai sont notés automatiquement."
    >
      <form noValidate onSubmit={(event) => void submit(event)} className="grid gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          {codes.map((code) => (
            <FormField
              key={code}
              control={form.control}
              name={code}
              label={describeStatus('critereEvaluation', code).label}
              hint={`Poids ${ao.criteres.find((c) => c.code === code)?.poids ?? 0} %`}
            >
              {({ field, control }) => (
                <NumberInput {...control} min={0} max={100} value={field.value} onChange={field.onChange} />
              )}
            </FormField>
          ))}
        </div>
        {codes.length === 0 && (
          <p className="text-sm text-muted-foreground">
            Aucun critère manuel n&apos;est pondéré pour cet appel d&apos;offres.
          </p>
        )}
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>
            Annuler
          </Button>
          <Button type="submit" loading={evaluer.isPending} disabled={codes.length === 0}>
            Enregistrer les notes
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  )
}
