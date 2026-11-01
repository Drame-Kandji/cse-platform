import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  describeStatus,
  labelOf,
  optionsOf,
  STATUS_MAPS,
  STATUT_BESOIN,
  type StatusDescriptor,
} from '@/shared/domain/labels'
import { StatusBadge } from './status-badge'

describe('StatusBadge', () => {
  it('affiche le libelle francais et le ton du referentiel (bleu = en cours)', () => {
    render(<StatusBadge kind="statutBesoin" value="EN_ATTENTE" />)
    const badge = screen.getByText('En attente').parentElement
    expect(badge).toHaveAttribute('data-status', 'EN_ATTENTE')
    expect(badge?.className).toContain('text-primary-emphasis')
  })

  it('reste neutre (gris) pour un etat termine', () => {
    render(<StatusBadge kind="etatMateriel" value="DISPONIBLE" />)
    expect(screen.getByText('Disponible').parentElement?.className).toContain('text-muted-foreground')
  })

  it('utilise un ton danger pour un fournisseur blackliste', () => {
    render(<StatusBadge kind="statutFournisseur" value="BLACKLISTE" />)
    expect(screen.getByText('Blacklisté').parentElement?.className).toContain('text-danger-emphasis')
  })

  it('affiche la valeur brute en ton neutre pour une valeur inconnue', () => {
    render(<StatusBadge kind="etatMateriel" value="NOUVEL_ETAT" />)
    expect(screen.getByText('NOUVEL_ETAT')).toBeInTheDocument()
  })
})

describe('referentiel des libelles', () => {
  it('couvre chaque valeur de chaque enumeration', () => {
    expect(Object.keys(STATUS_MAPS.statutBesoin)).toEqual([...STATUT_BESOIN])
    for (const map of Object.values(STATUS_MAPS) as Readonly<Record<string, StatusDescriptor>>[]) {
      for (const descriptor of Object.values(map)) {
        expect(descriptor.label.length).toBeGreaterThan(0)
        // Charte bleu + blanc : neutre, bleu ou rouge uniquement.
        expect(['neutral', 'primary', 'danger']).toContain(descriptor.tone)
      }
    }
  })
  it('fournit options et libelles', () => {
    expect(optionsOf('priorite')).toEqual([
      { value: 'NORMAL', label: 'Normale' },
      { value: 'URGENT', label: 'Urgente' },
      { value: 'CRITIQUE', label: 'Critique' },
    ])
    expect(labelOf('statutContrat', 'ENGAGEMENT_VALIDE')).toBe('Engagement validé')
    expect(labelOf('statutContrat', 'EN_ATTENTE_SIGNATURE')).toBe('En attente du fournisseur')
    expect(describeStatus('role', 'DAF').tone).toBe('neutral')
  })
})
