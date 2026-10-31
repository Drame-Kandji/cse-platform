import { useState } from 'react'
import { useDebouncedValue } from '@/shared/lib/hooks'
import { Combobox } from '@/shared/ui/combobox'
import { useUtilisateurs } from '../hooks/useUtilisateurs'

/** Filtre utilisateur du journal (recherche serveur sur les comptes). */
export function AuditUtilisateurFilter({
  value,
  onChange,
}: {
  value: string | undefined
  onChange: (value: string | undefined) => void
}) {
  const [search, setSearch] = useState('')
  const q = useDebouncedValue(search)
  const query = useUtilisateurs({ q: q || undefined, page: 0, size: 20 })
  return (
    <Combobox
      className="sm:w-56"
      placeholder="Tous les utilisateurs"
      searchPlaceholder="Rechercher un utilisateur…"
      emptyText="Aucun utilisateur"
      loading={query.isFetching}
      onSearchChange={setSearch}
      value={value ?? null}
      onValueChange={(next) => {
        onChange(next ?? undefined)
      }}
      options={(query.data?.content ?? []).map((user) => ({
        value: user.id,
        label: `${user.prenom} ${user.nom}`,
        description: user.email,
      }))}
    />
  )
}
