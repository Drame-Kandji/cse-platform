import { cva } from 'class-variance-authority'

/** Variantes visuelles du bouton (reutilisables pour styler un lien : `buttonVariants({ variant: 'secondary' })`). */
export const buttonVariants = cva(
  'relative inline-flex shrink-0 select-none items-center justify-center gap-2 whitespace-nowrap rounded-lg font-medium transition-[color,background-color,border-color,box-shadow,transform] duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background active:translate-y-px disabled:pointer-events-none disabled:opacity-50 [&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        primary: 'bg-primary text-primary-foreground shadow-xs hover:bg-primary/90',
        secondary:
          'border border-border-strong/70 bg-surface text-foreground shadow-xs hover:bg-muted hover:border-border-strong',
        ghost: 'text-foreground hover:bg-muted',
        subtle: 'bg-muted text-foreground hover:bg-muted/70',
        danger: 'bg-danger text-danger-foreground shadow-xs hover:bg-danger/90',
        accent: 'bg-accent text-accent-foreground shadow-xs hover:bg-accent/90',
        link: 'h-auto px-0 text-primary-emphasis underline-offset-4 hover:underline active:translate-y-0',
      },
      size: {
        sm: 'h-8 px-3 text-xs',
        md: 'h-10 px-4 text-sm sm:h-9',
        lg: 'h-11 px-5 text-sm',
        icon: 'size-10 sm:size-9',
        'icon-sm': 'size-8',
      },
    },
    defaultVariants: { variant: 'primary', size: 'md' },
  },
)
