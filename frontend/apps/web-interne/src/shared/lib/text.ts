type Nullable<T> = T | null | undefined

/** Initiales d'un nom complet : "Moussa Ndiaye" => "MN". */
export function initials(name: Nullable<string>): string {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/).filter(Boolean)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? '') : ''
  return (first + last).toUpperCase() || '?'
}
