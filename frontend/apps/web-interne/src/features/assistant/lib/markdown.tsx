import { Fragment, type ReactNode } from 'react'

/** Bloc de markdown simple reconnu par le moteur de rendu. */
export type MarkdownBlock =
  | { readonly kind: 'paragraph'; readonly lines: readonly string[] }
  | { readonly kind: 'heading'; readonly text: string }
  | { readonly kind: 'list'; readonly ordered: boolean; readonly items: readonly string[] }

const BULLET = /^\s*[-*•]\s+(.*)$/
const ORDERED = /^\s*\d+[.)]\s+(.*)$/
const HEADING = /^\s{0,3}#{1,6}\s+(.*)$/

/**
 * Decoupe un texte markdown simple en blocs : paragraphes (separes par une ligne vide),
 * titres `#`, listes a puces (`-`, `*`) ou numerotees (`1.`).
 */
export function parseMarkdownBlocks(source: string): MarkdownBlock[] {
  const blocks: MarkdownBlock[] = []
  const state: { paragraph: string[]; list: { ordered: boolean; items: string[] } | null } = {
    paragraph: [],
    list: null,
  }

  const flush = () => {
    if (state.paragraph.length) blocks.push({ kind: 'paragraph', lines: state.paragraph })
    if (state.list) blocks.push({ kind: 'list', ordered: state.list.ordered, items: state.list.items })
    state.paragraph = []
    state.list = null
  }

  for (const rawLine of source.replace(/\r\n?/g, '\n').split('\n')) {
    const line = rawLine.trimEnd()
    if (line.trim() === '') {
      flush()
      continue
    }
    const heading = HEADING.exec(line)
    const bullet = BULLET.exec(line)
    const ordered = bullet ? null : ORDERED.exec(line)
    const itemText = (bullet ?? ordered)?.[1]
    if (heading?.[1] !== undefined) {
      flush()
      blocks.push({ kind: 'heading', text: heading[1] })
    } else if (itemText !== undefined) {
      const isOrdered = ordered !== null
      if (state.paragraph.length || (state.list && state.list.ordered !== isOrdered)) flush()
      state.list ??= { ordered: isOrdered, items: [] }
      state.list.items.push(itemText)
    } else if (state.list && /^\s{2,}/.test(line)) {
      // Continuation indentee du dernier element de liste.
      const items = state.list.items
      items[items.length - 1] = `${items[items.length - 1] ?? ''} ${line.trim()}`
    } else {
      if (state.list) flush()
      state.paragraph.push(line.trim())
    }
  }
  flush()
  return blocks
}

const INLINE = /(\*\*[^*\n]+?\*\*|__[^_\n]+?__|`[^`\n]+?`)/g

/**
 * Rendu des styles en ligne : **gras**, __gras__ et `code`. Tout le reste est rendu comme texte
 * (React echappe le contenu : aucune injection HTML possible).
 */
export function renderInline(text: string): ReactNode[] {
  return text.split(INLINE).map((part, index) => {
    if (
      part.length > 4 &&
      ((part.startsWith('**') && part.endsWith('**')) || (part.startsWith('__') && part.endsWith('__')))
    ) {
      return (
        <strong key={index} className="font-semibold text-foreground">
          {part.slice(2, -2)}
        </strong>
      )
    }
    if (part.length > 2 && part.startsWith('`') && part.endsWith('`')) {
      return (
        <code key={index} className="rounded bg-muted px-1 py-0.5 font-mono text-[0.85em]">
          {part.slice(1, -1)}
        </code>
      )
    }
    return <Fragment key={index}>{part}</Fragment>
  })
}
