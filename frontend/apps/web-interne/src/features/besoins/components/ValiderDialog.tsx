import { addDays, format, parseISO } from 'date-fns'
import { CheckCircle2, ShoppingCart } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'
import { Button } from '@/shared/ui/button'
import { Checkbox } from '@/shared/ui/checkbox'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { Field } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { Skeleton } from '@/shared/ui/skeleton'
import { softToneClasses } from '@/shared/ui/tone'
import type { Besoin, DisponibiliteParams } from '../api/types'
import { useValiderBesoin } from '../hooks/useBesoinMutations'
import { useDisponibilite } from '../hooks/useBesoins'

type BesoinAValider = Pick<
  Besoin,
  'id' | 'reference' | 'categorie' | 'designation' | 'quantite' | 'dateBesoin' | 'dureeUtilisationJours'
>

/** Periode d'utilisation demandee : [dateBesoin, dateBesoin + duree]. */
function disponibiliteParams(besoin: BesoinAValider): DisponibiliteParams {
  const debut = parseISO(besoin.dateBesoin)
  return {
    categorie: besoin.categorie,
    designation: besoin.designation,
    quantite: besoin.quantite,
    du: besoin.dateBesoin,
    au: format(addDays(debut, besoin.dureeUtilisationJours), 'yyyy-MM-dd'),
  }
}

interface Props {
  besoin: BesoinAValider
  open: boolean
  onOpenChange: (open: boolean) => void
}

/** Validation RM (R01) : disponibilite du parc, choix facultatif des materiels, commentaire. */
export function ValiderDialog({ besoin, open, onOpenChange }: Props) {
  const { t } = useTranslation('besoins')
  const [commentaire, setCommentaire] = useState('')
  const [choix, setChoix] = useState<string[]>([])
  const dispo = useDisponibilite(open ? disponibiliteParams(besoin) : null)
  const valider = useValiderBesoin()
  const data = dispo.data

  const toggle = (id: string, checked: boolean) => {
    setChoix((current) =>
      checked
        ? current.length < besoin.quantite
          ? [...current, id]
          : current
        : current.filter((x) => x !== id),
    )
  }

  const confirmer = () => {
    valider.mutate(
      {
        id: besoin.id,
        commentaire: commentaire.trim() || undefined,
        materielIds: choix.length > 0 ? choix : undefined,
      },
      {
        onSuccess: () => {
          setChoix([])
          setCommentaire('')
          onOpenChange(false)
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !valider.isPending && onOpenChange(next)}>
      <DialogContent
        size="lg"
        title={t('valider.title', { reference: besoin.reference })}
      >
        <div className="grid gap-4">
          <section aria-label={t('valider.disponibilite')} className="grid gap-3">
            {dispo.isPending ? (
              <Skeleton className="h-14 w-full" />
            ) : dispo.isError || !data ? (
              <p className="rounded-lg bg-muted/60 p-3 text-sm text-muted-foreground">
                {t('valider.indisponible')}
              </p>
            ) : (
              <>
                <p
                  className={cn(
                    'flex items-start gap-2 rounded-lg p-3 text-sm ring-1 ring-inset',
                    softToneClasses.primary,
                  )}
                >
                  {data.disponible ? (
                    <CheckCircle2 className="mt-0.5 size-4 shrink-0" aria-hidden />
                  ) : (
                    <ShoppingCart className="mt-0.5 size-4 shrink-0" aria-hidden />
                  )}
                  {t(data.disponible ? 'valider.suffisant' : 'valider.insuffisant', {
                    dispo: data.quantiteDisponible,
                    demande: data.quantiteDemandee,
                  })}
                </p>
                {data.disponible && data.materiels.length > 0 && (
                  <fieldset className="grid gap-2">
                    <legend className="mb-1 text-sm font-medium" title={t('valider.choixHint', { max: besoin.quantite })}>
                      {t('valider.choixMateriels')}
                    </legend>
                    <ul className="scrollbar-thin grid max-h-56 gap-1.5 overflow-y-auto">
                      {data.materiels.map((materiel) => {
                        const checked = choix.includes(materiel.id)
                        return (
                          <li key={materiel.id} className="rounded-lg border border-border p-2.5">
                            <Checkbox
                              checked={checked}
                              disabled={!checked && choix.length >= besoin.quantite}
                              onCheckedChange={(value) => {
                                toggle(materiel.id, value === true)
                              }}
                              label={`${materiel.code} · ${materiel.designation}`}
                              description={materiel.chantierActuelNom ?? materiel.marque ?? undefined}
                            />
                          </li>
                        )
                      })}
                    </ul>
                  </fieldset>
                )}
              </>
            )}
          </section>
          <Field label={t('valider.commentaire')} optional>
            {(control) => (
              <Textarea
                {...control}
                rows={2}
                value={commentaire}
                onChange={(event) => {
                  setCommentaire(event.target.value)
                }}
                placeholder={t('valider.commentairePlaceholder')}
              />
            )}
          </Field>
        </div>
        <DialogFooter>
          <Button
            variant="secondary"
            disabled={valider.isPending}
            onClick={() => {
              onOpenChange(false)
            }}
          >
            {t('form.cancel')}
          </Button>
          <Button loading={valider.isPending} onClick={confirmer} leftIcon={<CheckCircle2 />}>
            {t('valider.confirmer')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
