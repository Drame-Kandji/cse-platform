import { startOfToday } from 'date-fns'
import type { ReactNode } from 'react'
import { useWatch } from 'react-hook-form'
import { describeStatus, labelOf } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDateTime, formatMoney, formatNumber } from '@/shared/lib/format'
import { DatePicker } from '@/shared/ui/date-picker'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { useFournisseursInvitables } from '../../hooks/useAppelsOffres'
import { instantLimite } from '../../schemas/appel-offre.schema'
import type { WizardForm } from './StepsBesoin'

/** Etape 5 : date et heure limites de depot (aucune offre acceptee apres : R02). */
export function StepDateLimite({ form }: { form: WizardForm }) {
  return (
    <div className="grid gap-4">
      <p className="text-sm text-muted-foreground">Aucune offre acceptée après cette échéance (R02).</p>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField control={form.control} name="dateLimite" label="Date limite" required>
          {({ field, control }) => (
            <DatePicker
              {...control}
              value={field.value ?? null}
              onChange={field.onChange}
              min={startOfToday()}
            />
          )}
        </FormField>
        <FormField
          control={form.control}
          name="heureLimite"
          label="Heure limite"
          required
          hint="Heure locale (GMT)"
        >
          {({ field, control }) => <Input {...control} {...field} type="time" />}
        </FormField>
      </div>
    </div>
  )
}

function Ligne({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid gap-0.5 py-2 sm:grid-cols-[11rem_1fr] sm:gap-4">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm text-foreground">{children}</dd>
    </div>
  )
}

/** Etape 6 : recapitulatif avant publication. */
export function StepRecapitulatif({ form }: { form: WizardForm }) {
  const values = useWatch({ control: form.control })
  const invitables = useFournisseursInvitables(values.categorie)
  const noms = (values.fournisseurIdsInvites ?? []).map(
    (id) => invitables.data?.content.find((f) => f.id === id)?.raisonSociale ?? id,
  )
  const criteres = (values.criteres ?? []).filter((c) => (c.poids ?? 0) > 0)
  return (
    <dl className="divide-y divide-border">
      <Ligne label="Objet">{values.objet}</Ligne>
      <Ligne label="Type">{values.type ? labelOf('typeAppelOffre', values.type) : EMPTY_VALUE}</Ligne>
      <Ligne label="Catégorie">
        {values.categorie ? labelOf('categorieMateriel', values.categorie) : EMPTY_VALUE}
      </Ligne>
      <Ligne label="Quantité">{formatNumber(values.quantite)}</Ligne>
      <Ligne label="Budget estimé">{formatMoney(values.budgetEstime)}</Ligne>
      <Ligne label="Critères">
        <ul className="flex flex-wrap gap-x-4 gap-y-1">
          {criteres.map((c) => (
            <li key={c.code} className="tabular-nums">
              {c.code ? describeStatus('critereEvaluation', c.code).label : ''} : <strong>{c.poids} %</strong>
            </li>
          ))}
        </ul>
      </Ligne>
      <Ligne label={`Fournisseurs invités (${noms.length})`}>{noms.join(', ') || EMPTY_VALUE}</Ligne>
      <Ligne label="Date limite">
        {values.dateLimite && values.heureLimite
          ? formatDateTime(instantLimite(values.dateLimite, values.heureLimite))
          : EMPTY_VALUE}
      </Ligne>
    </dl>
  )
}
