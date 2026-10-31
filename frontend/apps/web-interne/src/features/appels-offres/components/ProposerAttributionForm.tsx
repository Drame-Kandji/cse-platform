import { zodResolver } from '@hookform/resolvers/zod'
import { Send } from 'lucide-react'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { applyServerErrors } from '@/shared/lib/forms'
import { formatMoney, formatNumber } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardFooter, CardHeader } from '@/shared/ui/card'
import { ErrorState } from '@/shared/ui/error-state'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import { Skeleton } from '@/shared/ui/skeleton'
import type { AppelOffre } from '../api/types'
import { useAttribuer } from '../hooks/useAppelOffreActions'
import { useComparatif } from '../hooks/useAppelsOffres'
import { attributionSchema, type AttributionInput } from '../schemas/appel-offre.schema'
import { nomOffre } from './offre-display'

/** Proposition d'attribution motivee par le RA (offre recommandee preselectionnee). */
export function ProposerAttributionForm({ ao }: { ao: AppelOffre }) {
  const query = useComparatif(ao.id)
  const attribuer = useAttribuer(ao.id)
  const form = useForm<AttributionInput>({
    resolver: zodResolver(attributionSchema),
    defaultValues: { offreId: '', justification: '' },
  })
  const recommandation = query.data?.recommandationOffreId
  useEffect(() => {
    if (recommandation && !form.getValues('offreId')) form.setValue('offreId', recommandation)
  }, [recommandation, form])

  const submit = form.handleSubmit(async (values) => {
    try {
      await attribuer.mutateAsync({ offreId: values.offreId, justification: values.justification.trim() })
    } catch (error) {
      applyServerErrors(error, form.setError)
    }
  })

  if (query.isPending) return <Skeleton className="h-64 w-full" />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />

  const options = query.data.offres.map((offre) => ({
    value: offre.id,
    label: `${offre.rang ? `${offre.rang}. ` : ''}${nomOffre(offre, ao.statut)}`,
    description: `${formatMoney(offre.montantPropose)} · ${formatNumber(offre.delaiLivraisonJours)} j · score ${formatNumber(offre.scoreGlobal, { decimals: 1 })}/100${offre.id === recommandation ? ' · recommandée' : ''}`,
  }))

  return (
    <Card>
      <CardHeader
        title="Proposer l'attribution"
        description="Choisissez l'offre retenue et motivez la décision. Elle sera soumise au circuit d'approbation (R11)."
      />
      <form noValidate onSubmit={(event) => void submit(event)}>
        <CardContent className="grid gap-4">
          <FormField control={form.control} name="offreId" label="Offre retenue" required>
            {({ field, control }) => (
              <RadioGroup
                {...control}
                variant="card"
                options={options}
                value={field.value || undefined}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="justification"
            label="Justification"
            required
            hint="Expliquez le choix, notamment s'il ne s'agit pas de l'offre recommandée."
          >
            {({ field, control }) => <Textarea {...control} {...field} rows={4} />}
          </FormField>
        </CardContent>
        <CardFooter>
          <Button
            type="submit"
            leftIcon={<Send />}
            loading={attribuer.isPending}
            disabled={options.length === 0}
          >
            Proposer l&apos;attribution
          </Button>
        </CardFooter>
      </form>
    </Card>
  )
}
