import { useTranslation } from 'react-i18next'
import { Stepper, type Step } from '@/shared/ui/stepper'
import type { Besoin } from '../api/types'

/**
 * Workflow visuel du besoin (machine a etats B.3) :
 * EN_ATTENTE -> VALIDE -> AFFECTE_DIRECT | EN_ACHAT ; EN_ATTENTE -> REJETE.
 */
export function BesoinWorkflow({ besoin }: { besoin: Pick<Besoin, 'statut' | 'orientationEnAttente'> }) {
  const { t } = useTranslation('besoins')
  const soumis: Step = { id: 'soumis', label: t('steps.soumis'), description: t('steps.soumisDesc') }

  if (besoin.statut === 'REJETE') {
    return (
      <Stepper
        steps={[soumis, { id: 'rejete', label: t('steps.rejete'), description: t('steps.validationDesc') }]}
        current={1}
        errorIndex={1}
      />
    )
  }

  const validation: Step =
    besoin.statut === 'EN_ATTENTE'
      ? { id: 'validation', label: t('steps.validation'), description: t('steps.validationDesc') }
      : { id: 'validation', label: t('steps.valide'), description: t('steps.validationDesc') }
  const orientation: Step =
    besoin.statut === 'AFFECTE_DIRECT'
      ? { id: 'orientation', label: t('steps.affecte'), description: t('steps.affecteDesc') }
      : besoin.statut === 'EN_ACHAT'
        ? { id: 'orientation', label: t('steps.achat'), description: t('steps.achatDesc') }
        : {
            id: 'orientation',
            label: t('steps.orientation'),
            description: besoin.orientationEnAttente
              ? t('steps.orientationAttenteDesc')
              : t('steps.orientationDesc'),
          }
  const current = { EN_ATTENTE: 1, VALIDE: 2, EN_ACHAT: 2, AFFECTE_DIRECT: 3 }[besoin.statut]
  return <Stepper steps={[soumis, validation, orientation]} current={current} />
}
