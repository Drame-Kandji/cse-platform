import * as MenuPrimitive from '@radix-ui/react-dropdown-menu'
import { Check } from 'lucide-react'
import { forwardRef, type ComponentPropsWithoutRef, type ElementRef, type ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export const DropdownMenu = MenuPrimitive.Root
export const DropdownMenuTrigger = MenuPrimitive.Trigger
export const DropdownMenuGroup = MenuPrimitive.Group
export const DropdownMenuRadioGroup = MenuPrimitive.RadioGroup

/** Classes communes des surfaces flottantes (menus, popovers). */
export const floatingClasses =
  'z-50 min-w-44 animate-pop-in overflow-hidden rounded-xl border border-border bg-surface-raised p-1 text-foreground shadow-overlay outline-none focus-visible:ring-0 focus-visible:ring-offset-0'

const itemClasses =
  'relative flex h-9 cursor-pointer select-none items-center gap-2.5 rounded-md px-2.5 text-sm outline-none transition-colors data-[disabled]:pointer-events-none data-[highlighted]:bg-muted data-[disabled]:opacity-50 [&_svg]:size-4 [&_svg]:shrink-0 [&_svg]:text-muted-foreground'

export const DropdownMenuContent = forwardRef<
  ElementRef<typeof MenuPrimitive.Content>,
  ComponentPropsWithoutRef<typeof MenuPrimitive.Content>
>(function DropdownMenuContent({ className, sideOffset = 6, align = 'end', ...props }, ref) {
  return (
    <MenuPrimitive.Portal>
      <MenuPrimitive.Content
        ref={ref}
        sideOffset={sideOffset}
        align={align}
        className={cn(floatingClasses, className)}
        {...props}
      />
    </MenuPrimitive.Portal>
  )
})

export interface DropdownMenuItemProps extends ComponentPropsWithoutRef<typeof MenuPrimitive.Item> {
  icon?: ReactNode
  shortcut?: string
  /** Action destructive (texte rouge). */
  destructive?: boolean
}

export const DropdownMenuItem = forwardRef<ElementRef<typeof MenuPrimitive.Item>, DropdownMenuItemProps>(
  function DropdownMenuItem({ className, icon, shortcut, destructive, children, ...props }, ref) {
    return (
      <MenuPrimitive.Item
        ref={ref}
        className={cn(
          itemClasses,
          destructive && 'text-danger-emphasis [&_svg]:text-danger-emphasis',
          className,
        )}
        {...props}
      >
        {icon}
        <span className="flex-1 truncate">{children}</span>
        {shortcut && <span className="text-xs text-muted-foreground">{shortcut}</span>}
      </MenuPrimitive.Item>
    )
  },
)

export const DropdownMenuCheckboxItem = forwardRef<
  ElementRef<typeof MenuPrimitive.CheckboxItem>,
  ComponentPropsWithoutRef<typeof MenuPrimitive.CheckboxItem>
>(function DropdownMenuCheckboxItem({ className, children, ...props }, ref) {
  return (
    <MenuPrimitive.CheckboxItem ref={ref} className={cn(itemClasses, 'pl-8', className)} {...props}>
      <MenuPrimitive.ItemIndicator className="absolute left-2.5">
        <Check className="!text-primary-emphasis" aria-hidden />
      </MenuPrimitive.ItemIndicator>
      {children}
    </MenuPrimitive.CheckboxItem>
  )
})

export const DropdownMenuRadioItem = forwardRef<
  ElementRef<typeof MenuPrimitive.RadioItem>,
  ComponentPropsWithoutRef<typeof MenuPrimitive.RadioItem> & { icon?: ReactNode }
>(function DropdownMenuRadioItem({ className, children, icon, ...props }, ref) {
  return (
    <MenuPrimitive.RadioItem ref={ref} className={cn(itemClasses, className)} {...props}>
      {icon}
      <span className="flex-1">{children}</span>
      <MenuPrimitive.ItemIndicator>
        <Check className="!text-primary-emphasis" aria-hidden />
      </MenuPrimitive.ItemIndicator>
    </MenuPrimitive.RadioItem>
  )
})

export function DropdownMenuLabel({
  className,
  ...props
}: ComponentPropsWithoutRef<typeof MenuPrimitive.Label>) {
  return (
    <MenuPrimitive.Label
      className={cn('px-2.5 py-1.5 text-xs font-medium text-muted-foreground', className)}
      {...props}
    />
  )
}

export function DropdownMenuSeparator({
  className,
  ...props
}: ComponentPropsWithoutRef<typeof MenuPrimitive.Separator>) {
  return <MenuPrimitive.Separator className={cn('-mx-1 my-1 h-px bg-border', className)} {...props} />
}
