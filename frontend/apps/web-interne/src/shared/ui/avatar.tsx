import * as AvatarPrimitive from '@radix-ui/react-avatar'
import { cn } from '@/shared/lib/cn'
import { initials } from '@/shared/lib/text'

const sizes = { sm: 'size-7 text-2xs', md: 'size-9 text-xs', lg: 'size-12 text-sm' } as const
const palettes = [
  'bg-chart-1/15 text-primary-emphasis',
  'bg-chart-2/15 text-accent-emphasis',
  'bg-chart-3/15 text-success-emphasis',
  'bg-chart-5/15 text-danger-emphasis',
  'bg-info/15 text-info-emphasis',
] as const

function paletteFor(name: string): string {
  let hash = 0
  for (const char of name) hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  return palettes[hash % palettes.length] ?? palettes[0]
}

export interface AvatarProps {
  name: string
  src?: string
  size?: keyof typeof sizes
  className?: string
}

/** Avatar : photo si disponible, sinon initiales sur une teinte stable derivee du nom. */
export function Avatar({ name, src, size = 'md', className }: AvatarProps) {
  return (
    <AvatarPrimitive.Root
      className={cn('relative inline-flex shrink-0 overflow-hidden rounded-full', sizes[size], className)}
    >
      {src && <AvatarPrimitive.Image src={src} alt={name} className="size-full object-cover" />}
      <AvatarPrimitive.Fallback
        className={cn('grid size-full place-items-center font-semibold', paletteFor(name))}
        aria-label={name}
      >
        {initials(name)}
      </AvatarPrimitive.Fallback>
    </AvatarPrimitive.Root>
  )
}
