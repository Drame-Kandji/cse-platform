import { useId } from 'react'
import { cn } from '@/shared/lib/cn'

export interface SparklineProps {
  values: readonly number[]
  className?: string
  /** Remplissage degrade sous la courbe. */
  area?: boolean
  height?: number
}

/**
 * Mini-courbe SVG sans dependance (tendance dans une StatCard).
 * La couleur suit `currentColor` : appliquer une classe `text-chart-1` par exemple.
 */
export function Sparkline({ values, className, area = true, height = 36 }: SparklineProps) {
  const gradientId = useId()
  if (values.length < 2) return null
  const width = 120
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  const step = width / (values.length - 1)
  const points = values.map(
    (value, index) => [index * step, height - 3 - ((value - min) / range) * (height - 6)] as const,
  )
  const line = points
    .map(([x, y], index) => `${index === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`)
    .join(' ')
  const last = points[points.length - 1]
  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
      className={cn('h-9 w-full overflow-visible text-chart-1', className)}
      aria-hidden
    >
      {area && (
        <>
          <defs>
            <linearGradient id={gradientId} x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor="currentColor" stopOpacity={0.18} />
              <stop offset="100%" stopColor="currentColor" stopOpacity={0} />
            </linearGradient>
          </defs>
          <path d={`${line} L${width},${height} L0,${height} Z`} fill={`url(#${gradientId})`} />
        </>
      )}
      <path
        d={line}
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
        vectorEffect="non-scaling-stroke"
      />
      {last && (
        <circle cx={last[0]} cy={last[1]} r={2.5} fill="currentColor" vectorEffect="non-scaling-stroke" />
      )}
    </svg>
  )
}
