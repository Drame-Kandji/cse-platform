import type { Tableau } from '../types'
import { CategorieChart } from './charts/CategorieChart'
import { FinanceChart } from './charts/FinanceChart'
import { PrioriteDonut } from './charts/PrioriteDonut'
import { TopFournisseurs } from './TopFournisseurs'

/**
 * Graphiques d'analyse detaillee (deplaces du tableau de bord) : parc par categorie, besoins par
 * priorite, execution budgetaire et fournisseurs (le cycle d'achat est dans les tables), selon les sections presentes.
 */
export function AnalyseCharts({ tableau }: { tableau: Tableau }) {
  const { parc, besoins, finances, fournisseurs } = tableau
  if (!parc && !besoins && !finances && !fournisseurs) return null
  return (
    <div className="grid grid-flow-row-dense gap-6 lg:grid-cols-3">
      {parc && parc.parCategorie.length > 0 && (
        <div className="lg:col-span-2" data-section="parc-categories">
          <CategorieChart categories={parc.parCategorie} />
        </div>
      )}
      {besoins && (
        <div data-section="besoins">
          <PrioriteDonut besoins={besoins} />
        </div>
      )}
      {finances && (
        <div className="lg:col-span-2" data-section="finances">
          <FinanceChart finances={finances} />
        </div>
      )}
      {fournisseurs && (
        <div data-section="fournisseurs">
          <TopFournisseurs fournisseurs={fournisseurs} />
        </div>
      )}
    </div>
  )
}
