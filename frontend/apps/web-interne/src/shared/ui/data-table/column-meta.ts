import type { Column, RowData } from '@tanstack/react-table'

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
  }
}

/** Libelle lisible d'une colonne (meta.label, header texte, sinon id). */
export function columnLabel<T>(column: Column<T>): string {
  const header = column.columnDef.header
  return column.columnDef.meta?.label ?? (typeof header === 'string' ? header : column.id)
}

export const alignClasses = { left: 'text-left', center: 'text-center', right: 'text-right' } as const
