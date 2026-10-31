import { describeStatus } from '@/shared/domain/labels'
import type { MaterielDetail } from '@/shared/domain/types'
import { Card, CardContent } from '@/shared/ui/card'
import { DataTable } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { Timeline } from '@/shared/ui/timeline'
import { affectationColumns, maintenanceColumns, mouvementTitle, panneColumns } from './historiqueColumns'

function Count({ value }: { value: number }) {
  return (
    <span className="text-xs tabular-nums text-muted-foreground">
      {value}
    </span>
  )
}

/** Onglets d'historique de la fiche engin (20 derniers elements de chaque type). */
export function HistoriqueTabs({ materiel }: { materiel: MaterielDetail }) {
  const { affectations, maintenances, pannes, mouvements } = materiel
  return (
    <Tabs defaultValue="affectations">
      <TabsList aria-label="Historique du matériel">
        <TabsTrigger value="affectations">
          Affectations <Count value={affectations.length} />
        </TabsTrigger>
        <TabsTrigger value="maintenances">
          Maintenances <Count value={maintenances.length} />
        </TabsTrigger>
        <TabsTrigger value="pannes">
          Pannes <Count value={pannes.length} />
        </TabsTrigger>
        <TabsTrigger value="mouvements">
          Mouvements <Count value={mouvements.length} />
        </TabsTrigger>
      </TabsList>
      <TabsContent value="affectations">
        <DataTable
          label="Affectations du matériel"
          columns={affectationColumns}
          data={affectations}
          getRowId={(row) => row.id}
          enableColumnVisibility={false}
          emptyState={
<EmptyState size="compact" title="Aucune affectation" />
          }
        />
      </TabsContent>
      <TabsContent value="maintenances">
        <DataTable
          label="Maintenances du matériel"
          columns={maintenanceColumns}
          data={maintenances}
          getRowId={(row) => row.id}
          enableColumnVisibility={false}
          emptyState={
<EmptyState size="compact" title="Aucune maintenance" />
          }
        />
      </TabsContent>
      <TabsContent value="pannes">
        <DataTable
          label="Pannes du matériel"
          columns={panneColumns}
          data={pannes}
          getRowId={(row) => row.id}
          enableColumnVisibility={false}
          emptyState={<EmptyState size="compact" title="Aucune panne signalée" />}
        />
      </TabsContent>
      <TabsContent value="mouvements">
        <Card>
          <CardContent>
            {mouvements.length === 0 ? (
              <EmptyState size="compact" title="Aucun mouvement" />
            ) : (
              <Timeline
                items={mouvements.map((mouvement) => ({
                  id: mouvement.id,
                  title: mouvementTitle(
                    mouvement.type,
                    mouvement.chantierOrigineNom,
                    mouvement.chantierDestinationNom,
                  ),
                  date: mouvement.date,
                  actor: mouvement.auteurNom ?? undefined,
                  tone: describeStatus('typeMouvement', mouvement.type).tone,
                  description: mouvement.commentaire ?? undefined,
                }))}
              />
            )}
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
  )
}
