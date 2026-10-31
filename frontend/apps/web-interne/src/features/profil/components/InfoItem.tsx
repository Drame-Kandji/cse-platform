import type { ReactNode } from 'react'

/** Paire libelle / valeur d'une liste de definitions. */
export function InfoItem({
  label,
  children,
  icon,
}: {
  label: string
  children: ReactNode
  icon?: ReactNode
}) {
  return (
    <div className="flex gap-3">
      {icon && <span className="mt-0.5 text-muted-foreground [&_svg]:size-4">{icon}</span>}
      <div className="grid min-w-0 gap-0.5">
        <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
        <dd className="break-words text-sm text-foreground">{children}</dd>
      </div>
    </div>
  )
}
