import * as TooltipPrimitive from '@radix-ui/react-tooltip'
import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export const TooltipProvider = TooltipPrimitive.Provider

export interface TooltipProps {
  content: ReactNode
  children: ReactNode
  side?: 'top' | 'right' | 'bottom' | 'left'
  align?: 'start' | 'center' | 'end'
  /** Desactive l'infobulle (ex. sidebar deployee). */
  disabled?: boolean
  className?: string
}

/** Infobulle accessible (clavier + survol). L'enfant doit accepter une ref. */
export function Tooltip({
  content,
  children,
  side = 'top',
  align = 'center',
  disabled,
  className,
}: TooltipProps) {
  if (disabled) return <>{children}</>
  return (
    <TooltipPrimitive.Root>
      <TooltipPrimitive.Trigger asChild>{children}</TooltipPrimitive.Trigger>
      <TooltipPrimitive.Portal>
        <TooltipPrimitive.Content
          side={side}
          align={align}
          sideOffset={6}
          className={cn(
            'z-50 max-w-xs animate-pop-in rounded-md bg-foreground px-2 py-1 text-xs font-medium text-background shadow-overlay',
            className,
          )}
        >
          {content}
        </TooltipPrimitive.Content>
      </TooltipPrimitive.Portal>
    </TooltipPrimitive.Root>
  )
}
