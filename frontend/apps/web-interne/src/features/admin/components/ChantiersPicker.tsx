import { Search } from 'lucide-react'
import { useState } from 'react'
import { normalizeText } from '../lib/text'
import { Checkbox } from '@/shared/ui/checkbox'
import { ErrorState } from '@/shared/ui/error-state'
import { Input } from '@/shared/ui/input'
import { Skeleton } from '@/shared/ui/skeleton'
import { useChantiersOptions } from '../hooks/useUtilisateurs'

export interface ChantiersPickerProps {
  value: string[]
  onChange: (value: string[]) => void
  id?: string
  'aria-describedby'?: string
  'aria-invalid'?: boolean
}

/** Selection multiple de chantiers (recherche + cases a cocher), depuis `GET /api/chantiers`. */
export function ChantiersPicker({ value, onChange, id, ...aria }: ChantiersPickerProps) {
  const query = useChantiersOptions(true)
  const [search, setSearch] = useState('')
  if (query.isPending) return <Skeleton className="h-32 w-full" />
  if (query.isError)
    return <ErrorState size="compact" error={query.error} onRetry={() => void query.refetch()} />
  const needle = normalizeText(search)
  const chantiers = query.data.content.filter(
    (chantier) => !needle || normalizeText(`${chantier.code} ${chantier.nom}`).includes(needle),
  )
  const toggle = (chantierId: string, checked: boolean) => {
    onChange(checked ? [...value, chantierId] : value.filter((item) => item !== chantierId))
  }
  return (
    <div
      id={id}
      role="group"
      aria-label="Chantiers"
      {...aria}
      className="grid gap-2 rounded-lg border border-input p-2 aria-[invalid=true]:border-danger"
    >
      <Input
        type="search"
        aria-label="Rechercher un chantier"
        placeholder="Rechercher un chantier…"
        leftIcon={<Search />}
        value={search}
        onChange={(event) => {
          setSearch(event.target.value)
        }}
      />
      <ul className="scrollbar-thin grid max-h-48 gap-1 overflow-y-auto px-1">
        {chantiers.map((chantier) => (
          <li key={chantier.id} className="rounded-md px-1.5 py-1.5 hover:bg-muted">
            <Checkbox
              checked={value.includes(chantier.id)}
              onCheckedChange={(checked) => {
                toggle(chantier.id, checked === true)
              }}
              label={chantier.nom}
              description={`${chantier.code}${chantier.statut === 'TERMINE' ? ' · terminé' : ''}`}
            />
          </li>
        ))}
        {chantiers.length === 0 && (
          <li className="px-2 py-3 text-sm text-muted-foreground">Aucun chantier trouvé</li>
        )}
      </ul>
      <p className="px-1 text-xs text-muted-foreground">{value.length} chantier(s) sélectionné(s)</p>
    </div>
  )
}
