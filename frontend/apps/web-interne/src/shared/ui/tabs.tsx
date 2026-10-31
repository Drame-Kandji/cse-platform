import * as TabsPrimitive from '@radix-ui/react-tabs'
import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react'
import { cn } from '@/shared/lib/cn'

/**
 * Racine des onglets. `min-w-0` : dans une grille ou un flex, la largeur des onglets (non
 * retournables) ne doit pas elargir la page ; la barre defile horizontalement a la place.
 */
export const Tabs = forwardRef<
  ElementRef<typeof TabsPrimitive.Root>,
  ComponentPropsWithoutRef<typeof TabsPrimitive.Root>
>(function Tabs({ className, ...props }, ref) {
  return <TabsPrimitive.Root ref={ref} className={cn('min-w-0', className)} {...props} />
})

/** Barre d'onglets soulignee, defilable horizontalement sur mobile. */
export const TabsList = forwardRef<
  ElementRef<typeof TabsPrimitive.List>,
  ComponentPropsWithoutRef<typeof TabsPrimitive.List>
>(function TabsList({ className, ...props }, ref) {
  return (
    <TabsPrimitive.List
      ref={ref}
      className={cn('scrollbar-thin flex gap-1 overflow-x-auto border-b border-border', className)}
      {...props}
    />
  )
})

export const TabsTrigger = forwardRef<
  ElementRef<typeof TabsPrimitive.Trigger>,
  ComponentPropsWithoutRef<typeof TabsPrimitive.Trigger>
>(function TabsTrigger({ className, ...props }, ref) {
  return (
    <TabsPrimitive.Trigger
      ref={ref}
      className={cn(
        'relative -mb-px inline-flex h-10 shrink-0 items-center gap-2 whitespace-nowrap border-b-2 border-transparent px-3 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:rounded-md focus-visible:ring-offset-0 data-[state=active]:border-primary data-[state=active]:text-foreground [&_svg]:size-4',
        className,
      )}
      {...props}
    />
  )
})

export const TabsContent = forwardRef<
  ElementRef<typeof TabsPrimitive.Content>,
  ComponentPropsWithoutRef<typeof TabsPrimitive.Content>
>(function TabsContent({ className, ...props }, ref) {
  return (
    <TabsPrimitive.Content
      ref={ref}
      className={cn('pt-5 focus-visible:ring-offset-0', className)}
      {...props}
    />
  )
})
