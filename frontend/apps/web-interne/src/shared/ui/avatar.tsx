import * as AvatarPrimitive from '@radix-ui/react-avatar'
import { cn } from '@/shared/lib/cn'
import { initials } from '@/shared/lib/text'

const sizes = { sm: 'size-7 text-2xs', md: 'size-9 text-xs', lg: 'size-12 text-sm' } as const
/** Charte bleu + blanc : une seule teinte pour toutes les initiales. */
const AVATAR_TONE = 'bg-primary/10 text-primary-emphasis'

export interface AvatarProps {
  name: string
  src?: string
  size?: keyof typeof sizes
  className?: string
}

/** Avatar : photo si disponible, sinon initiales sur fond bleu pale. */
export function Avatar({ name, src, size = 'md', className }: AvatarProps) {
  return (
    <AvatarPrimitive.Root
      className={cn('relative inline-flex shrink-0 overflow-hidden rounded-full', sizes[size], className)}
    >
      {src && <AvatarPrimitive.Image src={src} alt={name} className="size-full object-cover" />}
      <AvatarPrimitive.Fallback
        className={cn('grid size-full place-items-center font-semibold', AVATAR_TONE)}
        aria-label={name}
      >
        {initials(name)}
      </AvatarPrimitive.Fallback>
    </AvatarPrimitive.Root>
  )
}
