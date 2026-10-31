import { ArrowUpRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { ETAT_MATERIEL, labelOf } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { StatusBadge } from '@/shared/ui/status-badge'
import { effectifGroupe, type CarteGroupe } from '../lib/grouping'
import { ETAT_BG } from '../lib/tiles'

const MAX_LIGNES = 8

/** Contenu d'infobulle d'un point : chantier, repartition par etat et liens vers les fiches engins. */
export function GroupePopup({ groupe }: { groupe: CarteGroupe }) {
  const { chantier, materiels } = groupe
  const visibles = materiels.slice(0, MAX_LIGNES)
  const reste = materiels.length - visibles.length
  return (
    <div className="grid w-64 gap-2.5 font-sans text-foreground">
      {chantier ? (
        <div className="grid gap-1">
          <div className="flex items-start justify-between gap-2">
            <p className="text-sm font-semibold leading-tight">{chantier.libelle}</p>
            {chantier.statut && <StatusBadge kind="statutChantier" value={chantier.statut} size="sm" />}
          </div>
          <p className="font-mono text-2xs text-muted-foreground">{chantier.code}</p>
        </div>
      ) : (
        <p className="text-sm font-semibold">Matériels hors chantier</p>
      )}
      <p className="text-xs text-muted-foreground">
        {effectifGroupe(groupe)} matériel(s)
        {materiels.length < effectifGroupe(groupe) && ' (filtres appliqués)'}
      </p>
      {materiels.length > 0 && (
        <div className="flex flex-wrap gap-x-3 gap-y-1 text-2xs text-muted-foreground">
          {ETAT_MATERIEL.filter((etat) => groupe.parEtat[etat] > 0).map((etat) => (
            <span key={etat} className="inline-flex items-center gap-1">
              <span className={cn('size-2 rounded-full', ETAT_BG[etat])} aria-hidden />
              {labelOf('etatMateriel', etat)} : {groupe.parEtat[etat]}
            </span>
          ))}
        </div>
      )}
      {visibles.length > 0 && (
        <ul className="grid max-h-56 gap-0.5 overflow-y-auto border-t border-border pt-2">
          {visibles.map((materiel) => (
            <li key={materiel.id}>
              <Link
                to={to.materielDetail(materiel.id)}
                className="group flex items-center gap-2 rounded-md px-1.5 py-1 text-xs hover:bg-muted"
              >
                <span
                  className={cn(
                    'size-2 shrink-0 rounded-full',
                    materiel.etat ? ETAT_BG[materiel.etat] : 'bg-border-strong',
                  )}
                  aria-hidden
                />
                <span className="min-w-0 flex-1 truncate">
                  <span className="font-mono text-muted-foreground">{materiel.code}</span> {materiel.libelle}
                </span>
                {materiel.etat && <span className="sr-only">{labelOf('etatMateriel', materiel.etat)}</span>}
                <ArrowUpRight
                  className="size-3.5 shrink-0 text-muted-foreground group-hover:text-foreground"
                  aria-hidden
                />
              </Link>
            </li>
          ))}
          {reste > 0 && <li className="px-1.5 pt-1 text-2xs text-muted-foreground">et {reste} autre(s)…</li>}
        </ul>
      )}
    </div>
  )
}
