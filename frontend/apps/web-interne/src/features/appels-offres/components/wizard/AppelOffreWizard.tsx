import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft, ArrowRight, Send } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { getErrorMessage, isApiError } from '@/shared/api'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Sheet, SheetContent } from '@/shared/ui/sheet'
import { Stepper } from '@/shared/ui/stepper'
import type { AppelOffre, BesoinAAcheter } from '../../api/types'
import { useCreerAppelOffre } from '../../hooks/useAppelOffreActions'
import {
  appelOffreSchema,
  CHAMPS_PAR_ETAPE,
  CRITERES_PAR_DEFAUT,
  instantLimite,
  invitesManquants,
  type AppelOffreFormInput,
  type AppelOffreFormOutput,
} from '../../schemas/appel-offre.schema'
import { CriteresEditor } from './CriteresEditor'
import { FournisseursPicker } from './FournisseursPicker'
import { StepObjet, StepSpecifications } from './StepsBesoin'
import { StepDateLimite, StepRecapitulatif } from './StepsFinales'

const ETAPES = [
  { id: 'objet', label: 'Objet & besoin' },
  { id: 'specs', label: 'Spécifications & budget' },
  { id: 'criteres', label: 'Critères pondérés' },
  { id: 'fournisseurs', label: 'Fournisseurs invités' },
  { id: 'date', label: 'Date limite' },
  { id: 'recap', label: 'Récapitulatif' },
] as const

function valeursInitiales(besoin?: BesoinAAcheter): AppelOffreFormInput {
  return {
    besoinId: besoin?.besoinId,
    objet: besoin ? `Acquisition : ${besoin.designation}` : '',
    description: besoin ? `Besoin ${besoin.reference} · chantier ${besoin.chantierNom}` : '',
    type: 'APPEL_OFFRES',
    categorie: besoin?.categorie ?? 'MATERIEL_ROULANT',
    quantite: besoin?.quantite ?? 1,
    specifications: besoin?.specifications ?? '',
    budgetEstime: undefined,
    criteres: CRITERES_PAR_DEFAUT.map((c) => ({ ...c })),
    fournisseurIdsInvites: [],
    dateLimite: null,
    heureLimite: '17:00',
  }
}

const vide = (value: string | undefined) => (value && value.trim() !== '' ? value.trim() : undefined)

interface WizardProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Besoin a l'origine de l'AO (pre-remplissage). */
  besoin?: BesoinAAcheter
  onCreated: (ao: AppelOffre) => void
}

/** Assistant de creation d'un appel d'offres (6 etapes, validation par etape). */
export function AppelOffreWizard({ open, onOpenChange, besoin, onCreated }: WizardProps) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      {open && <WizardContent key={besoin?.besoinId ?? 'libre'} besoin={besoin} onCreated={onCreated} />}
    </Sheet>
  )
}

function WizardContent({ besoin, onCreated }: Pick<WizardProps, 'besoin' | 'onCreated'>) {
  const [etape, setEtape] = useState(0)
  const creer = useCreerAppelOffre()
  const form = useForm<AppelOffreFormInput, unknown, AppelOffreFormOutput>({
    resolver: zodResolver(appelOffreSchema),
    defaultValues: valeursInitiales(besoin),
  })
  const derniere = etape === ETAPES.length - 1

  const suivant = async () => {
    const ok = await form.trigger([...(CHAMPS_PAR_ETAPE[etape] ?? [])])
    if (ok && etape === 3) {
      const message = invitesManquants(form.getValues('type'), form.getValues('fournisseurIdsInvites'))
      if (message) {
        form.setError('fournisseurIdsInvites', { type: 'manual', message })
        return
      }
    }
    if (ok) setEtape((value) => Math.min(value + 1, ETAPES.length - 1))
  }

  const publier = form.handleSubmit(async (values) => {
    try {
      const ao = await creer.mutateAsync({
        besoinId: values.besoinId,
        objet: values.objet,
        description: vide(values.description),
        type: values.type,
        categorie: values.categorie,
        quantite: values.quantite,
        specifications: vide(values.specifications),
        budgetEstime: values.budgetEstime,
        dateLimite: instantLimite(values.dateLimite, values.heureLimite),
        criteres: values.criteres.filter((c) => c.poids > 0),
        fournisseurIdsInvites: values.fournisseurIdsInvites,
      })
      onCreated(ao)
    } catch (error) {
      if (applyServerErrors(error, form.setError)) return
      toast.error(
        isApiError(error) && error.code === 'FOURNISSEURS_INDISPONIBLE'
          ? "Le référentiel fournisseurs est momentanément indisponible : l'éligibilité des invités ne peut pas être vérifiée (R14). Réessayez dans quelques instants."
          : getErrorMessage(error),
      )
    }
  })

  return (
    <SheetContent
      title="Lancer un appel d'offres"
      description={`Étape ${etape + 1} sur ${ETAPES.length} · ${ETAPES[etape]?.label ?? ''}`}
      size="xl"
      footer={
        <>
          <Button
            variant="secondary"
            leftIcon={<ArrowLeft />}
            disabled={etape === 0 || creer.isPending}
            onClick={() => {
              setEtape((value) => Math.max(0, value - 1))
            }}
          >
            Précédent
          </Button>
          {derniere ? (
            <Button leftIcon={<Send />} loading={creer.isPending} onClick={() => void publier()}>
              Publier l&apos;appel d&apos;offres
            </Button>
          ) : (
            <Button rightIcon={<ArrowRight />} onClick={() => void suivant()}>
              Suivant
            </Button>
          )}
        </>
      }
    >
      <Stepper steps={ETAPES} current={etape} className="mb-6 hidden md:flex" />
      <form
        noValidate
        onSubmit={(event) => {
          event.preventDefault()
          if (derniere) void publier()
          else void suivant()
        }}
      >
        {etape === 0 && <StepObjet form={form} besoinReference={besoin?.reference} />}
        {etape === 1 && <StepSpecifications form={form} />}
        {etape === 2 && <CriteresEditor form={form} />}
        {etape === 3 && <FournisseursPicker form={form} />}
        {etape === 4 && <StepDateLimite form={form} />}
        {etape === 5 && <StepRecapitulatif form={form} />}
      </form>
    </SheetContent>
  )
}
