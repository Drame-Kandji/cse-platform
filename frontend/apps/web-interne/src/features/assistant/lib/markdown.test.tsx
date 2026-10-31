import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Markdown } from '../components/Markdown'
import { parseMarkdownBlocks } from './markdown'

describe('parseMarkdownBlocks', () => {
  it('distingue paragraphes, listes a puces et listes numerotees', () => {
    const blocks = parseMarkdownBlocks('Bonjour\nsuite\n\n- un\n- deux\n\n1. premier\n2. second')
    expect(blocks).toEqual([
      { kind: 'paragraph', lines: ['Bonjour', 'suite'] },
      { kind: 'list', ordered: false, items: ['un', 'deux'] },
      { kind: 'list', ordered: true, items: ['premier', 'second'] },
    ])
  })
})

describe('Markdown', () => {
  it('rend le gras, le code en ligne et les listes', () => {
    const { container } = render(
      <Markdown source={'**3 pelles** disponibles : `PEL-001`\n\n- Dakar\n- Thiès'} />,
    )
    expect(container.querySelector('strong')).toHaveTextContent('3 pelles')
    expect(container.querySelector('code')).toHaveTextContent('PEL-001')
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual(['Dakar', 'Thiès'])
  })

  it("n'interprete jamais de HTML (aucune injection)", () => {
    const { container } = render(
      <Markdown source={'<img src=x onerror="alert(1)"> <script>alert(2)</script> **<b>gras</b>**'} />,
    )
    expect(container.querySelector('img')).toBeNull()
    expect(container.querySelector('script')).toBeNull()
    expect(container.querySelector('b')).toBeNull()
    expect(container.textContent).toContain('<script>alert(2)</script>')
    expect(container.querySelector('strong')).toHaveTextContent('<b>gras</b>')
  })
})
