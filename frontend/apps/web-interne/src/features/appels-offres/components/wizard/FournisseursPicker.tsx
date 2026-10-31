import { Users } from 'lucide-react'
import { useEffect } from 'react'
import { useWatch } from 'react-hook-form'
import { labelOf } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Checkbox } from '@/shared/ui/checkbox'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { useFournisseursInvitables } from '../../hooks/useAppelsOffres'
import type { WizardForm } from './StepsBesoin'

/** Barre de score de performance 0..100 (valeur toujours affichee en texte). */
export function ScoreMeter({ score }: { score: number }) {
  const tone = score >= 75 ? 'bg-success' : score >= 50 ? 'bg-warning' : 'bg-danger'
  return (
    <span className="flex items-center gap-2">
      <span className="h-1.5 w-16 overflow-hidden rounded-full bg-muted" aria-hidden>
        <span
          className={cn('block h-full rounded-full', tone)}
          style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
        />
      </span>
      <span className="text-xs font-medium tabular-nums">{formatNumber(score)}/100</span>
    </span>
  )
}

/** Etape 4 : choix des fournisseurs invites (ACTIFS de la specialite, R14). */
export function FournisseursPicker({ form }: { form: WizardForm }) {
  const categorie = useWatch({ control: form.control, name: 'categorie' })
  const type = useWatch({ control: form.control, name: 'type' })
  const selection = useWatch({ control: form.control, name: 'fournisseurIdsInvites' })
  const query = useFournisseursInvitables(categorie)
  const fournisseurs = query.data?.content ?? []
  const erreur = form.formState.errors.fournisseurIdsInvites?.message

  useEffect(() => {
    if (!query.data) return
    const ids = new Set(query.data.content.map((f) => f.id))
    const filtres = selection.filter((id) => ids.has(id))
    if (filtres.length !== selection.length) form.setValue('fournisseurIdsInvites', filtres)
  }, [query.data, selection, form])

  const toggle = (id: string, checked: boolean) => {
    const next = checked ? [...selection, id] : selection.filter((value) => value !== id)
    form.setValue('fournisseurIdsInvites', next, { shouldDirty: true, shouldValidate: Boolean(erreur) })
  }

  if (query.isPending) {
    return (
      <div className="grid gap-2">
        {[0, 1, 2].map((index) => (
          <Skeleton key={index} className="h-14 w-full" />
        ))}
      </div>
    )
  }
  if (query.isError)
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} size="compact" />

  return (
    <div className="grid gap-3">
      <p className="text-sm text-muted-foreground">
        Fournisseurs actifs · {labelOf('categorieMateriel', categorie)} (R14) ·{' '}
        {type === 'DEMANDE_PRIX' ? 'au moins un invité requis' : 'invitations facultatives'}
      </p>
      {fournisseurs.length === 0 ? (
        <EmptyState
          size="compact"
          icon={<Users aria-hidden />}
          title="Aucun fournisseur actif pour cette catégorie"
        />
      ) : (
        <ul className="grid gap-2" aria-label="Fournisseurs invitables">
          {fournisseurs.map((f) => {
            const checked = selection.includes(f.id)
            return (
              <li
                key={f.id}
                className={cn(
                  'flex items-center gap-3 rounded-lg border px-3 py-2.5 transition-colors',
                  checked ? 'border-primary/40 bg-primary/5' : 'border-border hover:bg-muted/40',
                )}
              >
                <Checkbox
                  id={`invite-${f.id}`}
                  checked={checked}
                  onCheckedChange={(value) => {
                    toggle(f.id, value === true)
                  }}
                />
                <label
                  htmlFor={`invite-${f.id}`}
                  className="flex min-w-0 flex-1 cursor-pointer items-center gap-3"
                >
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium">{f.raisonSociale}</span>
                    <span className="block text-xs text-muted-foreground">
                      {f.ville ?? 'Ville non renseignée'} · {f.nombreEvaluations} évaluation(s)
                    </span>
                  </span>
                  <ScoreMeter score={f.scorePerformance} />
                </label>
              </li>
            )
          })}
        </ul>
      )}
      <p className="text-sm tabular-nums text-muted-foreground" aria-live="polite">
        {selection.length} fournisseur(s) sélectionné(s)
      </p>
      {erreur && (
        <p role="alert" className="text-sm font-medium text-danger-emphasis">
          {erreur}
        </p>
      )}
    </div>
  )
}
