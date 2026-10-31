import { CheckCircle2, Copy, Eye, Filter, MoreHorizontal, Pencil, Trash2, XCircle } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { toast } from 'sonner'
import { optionsOf } from '@/shared/domain/labels'
import { formatMoney } from '@/shared/lib/format'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import { Button, IconButton } from '@/shared/ui/button'
import { Card, CardContent, CardFooter, CardHeader } from '@/shared/ui/card'
import { Checkbox } from '@/shared/ui/checkbox'
import { Dialog, DialogClose, DialogContent, DialogFooter, DialogTrigger } from '@/shared/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'
import { Field } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/shared/ui/popover'
import { Sheet, SheetContent, SheetTrigger } from '@/shared/ui/sheet'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { Tooltip } from '@/shared/ui/tooltip'
import { Demo, Section } from './Section'

const DETAIL_ROWS: readonly { label: string; value: ReactNode }[] = [
  { label: 'État', value: <StatusBadge kind="etatMateriel" value="DISPONIBLE" /> },
  { label: "Valeur d'acquisition", value: formatMoney(98_500_000) },
  { label: 'Localisation', value: 'Dépôt de Rufisque' },
  { label: 'Heures moteur', value: '4 312 h' },
]

/** Dialogues, panneaux, menus, onglets, cartes. */
export function OverlaysSection() {
  const [motif, setMotif] = useState('')
  return (
    <Section id="surfaces" title="Surfaces & superpositions">
      <Demo title="Dialog, Sheet, ConfirmDialog, DropdownMenu, Popover, Tooltip, Toasts">
        <div className="flex flex-wrap items-center gap-3">
          <Dialog>
            <DialogTrigger asChild>
              <Button variant="secondary">Ouvrir un dialogue</Button>
            </DialogTrigger>
            <DialogContent
              title="Prolonger l'affectation"
              description="La nouvelle date de fin sera notifiée au chef de chantier."
            >
              <div className="grid gap-4">
                <Field label="Nouvelle date de fin">{(p) => <Input {...p} type="date" />}</Field>
                <Field label="Commentaire" optional>
                  {(p) => <Textarea {...p} rows={3} />}
                </Field>
              </div>
              <DialogFooter>
                <DialogClose asChild>
                  <Button variant="secondary">Annuler</Button>
                </DialogClose>
                <DialogClose asChild>
                  <Button onClick={() => toast.success('Affectation prolongée')}>Prolonger</Button>
                </DialogClose>
              </DialogFooter>
            </DialogContent>
          </Dialog>
          <Sheet>
            <SheetTrigger asChild>
              <Button variant="secondary" leftIcon={<Eye />}>
                Panneau détail
              </Button>
            </SheetTrigger>
            <SheetContent
              title="Pelle hydraulique CAT 320"
              description="CSE-1013 · Matériel roulant"
              footer={
                <>
                  <Button variant="secondary">Historique</Button>
                  <Button>Affecter</Button>
                </>
              }
            >
              <dl className="grid gap-4 text-sm">
                {DETAIL_ROWS.map((row) => (
                  <div
                    key={row.label}
                    className="flex items-center justify-between gap-4 border-b border-border pb-3"
                  >
                    <dt className="text-muted-foreground">{row.label}</dt>
                    <dd className="font-medium tabular-nums">{row.value}</dd>
                  </div>
                ))}
              </dl>
            </SheetContent>
          </Sheet>
          <ConfirmDialog
            tone="danger"
            title="Rejeter ce besoin ?"
            description="Le chef de chantier sera notifié du rejet et du motif. Cette action est tracée dans le journal d'audit."
            confirmLabel="Rejeter le besoin"
            onConfirm={() =>
              new Promise<void>((resolve) => {
                window.setTimeout(() => {
                  toast.success('Besoin rejeté')
                  resolve()
                }, 800)
              })
            }
            trigger={
              <Button variant="danger" leftIcon={<XCircle />}>
                Rejeter
              </Button>
            }
          >
            <Field label="Motif" required>
              {(p) => (
                <Textarea
                  {...p}
                  rows={2}
                  value={motif}
                  onChange={(e) => {
                    setMotif(e.target.value)
                  }}
                />
              )}
            </Field>
          </ConfirmDialog>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <IconButton
                variant="secondary"
                label="Plus d'actions"
                icon={<MoreHorizontal />}
                tooltip={false}
              />
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuLabel>Actions</DropdownMenuLabel>
              <DropdownMenuItem icon={<Pencil />} shortcut="E">
                Modifier
              </DropdownMenuItem>
              <DropdownMenuItem icon={<Copy />}>Dupliquer</DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem icon={<Trash2 />} destructive>
                Supprimer
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="secondary" leftIcon={<Filter />}>
                Popover
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-64 p-4">
              <p className="mb-3 text-sm font-semibold">Priorités affichées</p>
              <div className="grid gap-2.5">
                {optionsOf('priorite').map((o) => (
                  <Checkbox key={o.value} label={o.label} defaultChecked />
                ))}
              </div>
            </PopoverContent>
          </Popover>
          <Tooltip content="Infobulle accessible au clavier">
            <Button variant="ghost">Survolez-moi</Button>
          </Tooltip>
          <Button
            variant="ghost"
            leftIcon={<CheckCircle2 />}
            onClick={() =>
              toast.success('Engagement validé', { description: 'Contrat CT-2026-014 · 48 500 000 FCFA' })
            }
          >
            Toast
          </Button>
        </div>
      </Demo>
      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-2">
        <Demo title="Tabs">
          <Tabs defaultValue="infos">
            <TabsList>
              <TabsTrigger value="infos">Informations</TabsTrigger>
              <TabsTrigger value="affectations">Affectations</TabsTrigger>
              <TabsTrigger value="maintenance">Maintenance</TabsTrigger>
              <TabsTrigger value="documents">Documents</TabsTrigger>
            </TabsList>
            <TabsContent value="infos" className="text-sm text-muted-foreground">
              Caractéristiques techniques et informations d'acquisition.
            </TabsContent>
            <TabsContent value="affectations" className="text-sm text-muted-foreground">
              Historique des affectations aux chantiers.
            </TabsContent>
            <TabsContent value="maintenance" className="text-sm text-muted-foreground">
              Ordres de travail et coûts.
            </TabsContent>
            <TabsContent value="documents" className="text-sm text-muted-foreground">
              Cartes grises, assurances, contrôles techniques.
            </TabsContent>
          </Tabs>
        </Demo>
        <Card>
          <CardHeader
            title="Card avec en-tête"
            description="Titre, description, action et pied."
            action={<IconButton size="icon-sm" label="Options" icon={<MoreHorizontal />} />}
          />
          <CardContent className="text-sm text-muted-foreground">
            Les cartes utilisent une bordure fine plutôt qu'une ombre marquée, pour rester calmes en contexte
            dense.
          </CardContent>
          <CardFooter>
            <Button variant="secondary" size="sm">
              Annuler
            </Button>
            <Button size="sm">Enregistrer</Button>
          </CardFooter>
        </Card>
      </div>
    </Section>
  )
}
