import { Card, CardHeader } from '@/shared/ui/card'
import { kpiTables } from '../lib/kpi-tables'
import type { Tableau } from '../types'

/** Apercu des tables d'indicateurs telles qu'exportees (une carte par section presente). */
export function KpiTables({ tableau }: { tableau: Tableau }) {
  const tables = kpiTables(tableau)
  return (
    <div className="gap-6 md:columns-2 xl:columns-3">
      {tables.map((table) => (
        <Card key={table.id} data-section={table.id} className="mb-6 break-inside-avoid overflow-hidden">
          <CardHeader title={table.title} className="pb-3" />
          <table className="w-full text-sm">
            <caption className="sr-only">{table.title}</caption>
            <tbody className="divide-y divide-border border-t border-border">
              {table.rows.map((row) => (
                <tr key={row.label}>
                  <th scope="row" className="px-5 py-2 text-left font-normal text-muted-foreground">
                    {row.label}
                  </th>
                  <td className="px-5 py-2 text-right font-medium tabular-nums">{row.value}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      ))}
    </div>
  )
}
