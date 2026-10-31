import { ArrowRightLeft, History, OctagonAlert, Wrench } from 'lucide-react'
import { Link } from 'react-router-dom'
import { paths } from '@/app/routes'
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
    <span className="rounded-full bg-muted px-1.5 text-2xs font-semibold tabular-nums text-muted-foreground">
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
          <ArrowRightLeft aria-hidden />
          Affectations <Count value={affectations.length} />
        </TabsTrigger>
        <TabsTrigger value="maintenances">
          <Wrench aria-hidden />
          Maintenances <Count value={maintenances.length} />
        </TabsTrigger>
        <TabsTrigger value="pannes">
          <OctagonAlert aria-hidden />
          Pannes <Count value={pannes.length} />
        </TabsTrigger>
        <TabsTrigger value="mouvements">
          <History aria-hidden />
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
            <EmptyState
              size="compact"
              title="Aucune affectation"
              description="Ce matériel n'a jamais été affecté."
            />
          }
        />
        <p className="mt-2 text-xs text-muted-foreground">
          Vue d&apos;ensemble des occupations dans le{' '}
          <Link to={paths.planning} className="font-medium text-primary-emphasis hover:underline">
            planning des affectations
          </Link>
          .
        </p>
      </TabsContent>
      <TabsContent value="maintenances">
        <DataTable
          label="Maintenances du matériel"
          columns={maintenanceColumns}
          data={maintenances}
          getRowId={(row) => row.id}
          enableColumnVisibility={false}
          emptyState={
            <EmptyState
              size="compact"
              title="Aucune maintenance"
              description="Aucun ordre de travail enregistré."
            />
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
