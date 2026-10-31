import type { Column, ColumnMeta, RowData } from '@tanstack/react-table'
import type { CSSProperties } from 'react'

declare module '@tanstack/react-table' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- signature imposee par TanStack
  interface ColumnMeta<TData extends RowData, TValue> {
    /** Libelle texte (menu Colonnes, vue mobile) si `header` n'est pas une chaine. */
    label?: string
    /** Alignement ; "right" pour les montants et quantites. */
    align?: 'left' | 'center' | 'right'
    headerClassName?: string
    cellClassName?: string
    /** Masque la colonne dans la vue cartes mobile. */
    hideOnMobile?: boolean
    /** Colonne utilisee comme titre de la carte mobile. */
    mobilePrimary?: boolean
    /**
     * Interdit le retour a la ligne (identifiants, references, dates, montants, badges).
     * Vrai par defaut pour `align: 'right'`.
     */
    nowrap?: boolean
    /** Largeur minimale de la colonne en px (texte libre : designation, chantier...). */
    minWidth?: number
    /**
     * Colonne secondaire masquee par defaut sous ce point de rupture (`xl` = 1280 px, `2xl` = 1536 px).
     * L'utilisateur peut la reafficher via le menu Colonnes.
     */
    hideBelow?: 'xl' | '2xl'
  }
}

/** Libelle lisible d'une colonne (meta.label, header texte, sinon id). */
export function columnLabel<T>(column: Column<T>): string {
  const header = column.columnDef.header
  return column.columnDef.meta?.label ?? (typeof header === 'string' ? header : column.id)
}

export const alignClasses = { left: 'text-left', center: 'text-center', right: 'text-right' } as const

/** Classes et style d'une cellule (en-tete ou corps) selon la meta de colonne. */
export function columnBoxProps(meta: ColumnMeta<unknown, unknown> | undefined): {
  className: string | undefined
  style: CSSProperties | undefined
} {
  if (!meta) return { className: undefined, style: undefined }
  const nowrap = meta.nowrap ?? meta.align === 'right'
  const style: CSSProperties = {}
  if (meta.minWidth !== undefined) style.minWidth = meta.minWidth
  return {
    className: nowrap ? 'whitespace-nowrap' : undefined,
    style: Object.keys(style).length > 0 ? style : undefined,
  }
}
