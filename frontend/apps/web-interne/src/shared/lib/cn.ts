import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Compose des classes Tailwind en resolvant les conflits (la derniere gagne).
 * @example cn('px-2', condition && 'px-4') // "px-4"
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
