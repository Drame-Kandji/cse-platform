import { KeyRound, MoreHorizontal, Pencil, Power, ShieldCheck, ShieldOff, UserPlus } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { describeStatus, ROLES } from '@/shared/domain/labels'
import { formatDate } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/avatar'
import { Badge } from '@/shared/ui/badge'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import { Button, IconButton } from '@/shared/ui/button'
import { DataTable, useTableState, type ColumnDef } from '@/shared/ui/data-table'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'
import { FilterBar } from '@/shared/ui/filter-bar'
import { PageHeader } from '@/shared/ui/page-header'
import { Select } from '@/shared/ui/select'
import { ResetPasswordDialog } from '../components/ResetPasswordDialog'
import { RolesOverview } from '../components/RolesOverview'
import { UserFormDialog } from '../components/UserFormDialog'
import { useChangerStatut, useUtilisateurs } from '../hooks/useUtilisateurs'
import type { Utilisateur } from '../types'

const TOUS = 'TOUS'
const ROLE_FILTER = [
  { value: TOUS, label: 'Tous les rôles' },
  ...ROLES.map((role) => ({ value: role, label: describeStatus('role', role).label })),
  { value: 'FOURNISSEUR', label: 'Fournisseur' },
]
const STATUT_FILTER = [
  { value: TOUS, label: 'Tous les statuts' },
  { value: 'true', label: 'Actifs' },
  { value: 'false', label: 'Désactivés' },
]

interface RowActions {
  onEdit: (user: Utilisateur) => void
  onReset: (user: Utilisateur) => void
  onToggle: (user: Utilisateur) => void
}

function columns(actions: RowActions): ColumnDef<Utilisateur>[] {
  return [
    {
      id: 'nom',
      header: 'Utilisateur',
      enableHiding: false,
      meta: { mobilePrimary: true, minWidth: 220 },
      cell: ({ row }) => {
        const user = row.original
        return (
          <span className="flex items-center gap-3">
            <Avatar name={`${user.prenom} ${user.nom}`} size="sm" />
            <span className="grid min-w-0">
              <span className="truncate font-medium">
                {user.prenom} {user.nom}
              </span>
              <span className="truncate text-xs text-muted-foreground">{user.email}</span>
            </span>
          </span>
        )
      },
    },
    {
      accessorKey: 'role',
      header: 'Rôle',
      meta: { nowrap: true },
      cell: ({ row }) => {
        const role = describeStatus('role', row.original.role)
        return (
          <Badge tone={role.tone} size="sm">
            {row.original.role === 'FOURNISSEUR' ? 'Fournisseur' : role.label}
          </Badge>
        )
      },
    },
    {
      accessorKey: 'actif',
      header: 'Statut',
      meta: { nowrap: true },
      cell: ({ row }) => (
        <Badge tone={row.original.actif ? 'success' : 'neutral'} dot size="sm">
          {row.original.actif ? 'Actif' : 'Désactivé'}
        </Badge>
      ),
    },
    {
      accessorKey: 'otpConfigure',
      header: 'OTP',
      meta: { hideOnMobile: true, nowrap: true, hideBelow: 'xl' },
      cell: ({ row }) =>
        row.original.otpConfigure ? (
          <Badge tone="info" size="sm">
            <ShieldCheck aria-hidden /> Configuré
          </Badge>
        ) : (
          <Badge tone="warning" variant="outline" size="sm">
            <ShieldOff aria-hidden /> Non configuré
          </Badge>
        ),
    },
    {
      accessorKey: 'createdAt',
      header: 'Créé le',
      meta: { hideOnMobile: true, nowrap: true },
      cell: ({ row }) => formatDate(row.original.createdAt),
    },
    {
      id: 'actions',
      header: () => <span className="sr-only">Actions</span>,
      enableHiding: false,
      meta: { align: 'right', nowrap: true },
      cell: ({ row }) => {
        const user = row.original
        return (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <IconButton
                label={`Actions pour ${user.prenom} ${user.nom}`}
                icon={<MoreHorizontal />}
                size="icon-sm"
                tooltip={false}
              />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuItem
                icon={<Pencil />}
                onSelect={() => {
                  actions.onEdit(user)
                }}
              >
                Modifier
              </DropdownMenuItem>
              <DropdownMenuItem
                icon={<KeyRound />}
                onSelect={() => {
                  actions.onReset(user)
                }}
              >
                Réinitialiser le mot de passe
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                icon={<Power />}
                destructive={user.actif}
                onSelect={() => {
                  actions.onToggle(user)
                }}
              >
                {user.actif ? 'Désactiver le compte' : 'Réactiver le compte'}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )
      },
    },
  ]
}

/** Administration des comptes : roles, filtres, creation / edition, statut, mot de passe. */
export function Component() {
  const [params, setParams] = useSearchParams()
  const table = useTableState()
  const role = params.get('role') ?? undefined
  const actifParam = params.get('actif')
  const q = params.get('q') ?? ''
  const actif = actifParam === 'true' ? true : actifParam === 'false' ? false : undefined
  const query = useUtilisateurs({ role, actif, q: q || undefined, ...table.pageParams })
  const changerStatut = useChangerStatut()

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Utilisateur | null>(null)
  const [resetting, setResetting] = useState<Utilisateur | null>(null)
  const [toggling, setToggling] = useState<Utilisateur | null>(null)

  const setFilter = (key: string, value: string | undefined) => {
    setParams(
      (current) => {
        const next = new URLSearchParams(current)
        if (value) next.set(key, value)
        else next.delete(key)
        next.delete('page')
        return next
      },
      { replace: true },
    )
  }

  const cols = columns({
    onEdit: (user) => {
      setEditing(user)
      setFormOpen(true)
    },
    onReset: setResetting,
    onToggle: setToggling,
  })

  return (
    <>
      <PageHeader
        title="Utilisateurs"
        description="Comptes de la plateforme, rôles et attributs métier (synchronisés avec Keycloak)."
        actions={
          <Button
            leftIcon={<UserPlus />}
            onClick={() => {
              setEditing(null)
              setFormOpen(true)
            }}
          >
            Nouvel utilisateur
          </Button>
        }
      />
      <div className="grid gap-6">
        <RolesOverview
          selected={role}
          onSelect={(value) => {
            setFilter('role', value)
          }}
        />
        <DataTable
          label="Utilisateurs"
          columns={cols}
          data={query.data}
          getRowId={(user) => user.id}
          isLoading={query.isPending}
          isFetching={query.isFetching}
          error={query.error}
          onRetry={() => void query.refetch()}
          pagination={table.pagination}
          onPaginationChange={table.onPaginationChange}
          sorting={table.sorting}
          onSortingChange={table.onSortingChange}
          toolbar={
            <FilterBar
              search={q}
              onSearchChange={(value) => {
                setFilter('q', value || undefined)
              }}
              searchPlaceholder="Nom, prénom ou e-mail…"
              canReset={Boolean(role ?? actifParam ?? q)}
              onReset={() => {
                setParams(new URLSearchParams(), { replace: true })
              }}
            >
              <Select
                aria-label="Filtrer par rôle"
                className="w-auto min-w-44"
                options={ROLE_FILTER}
                value={role ?? TOUS}
                onValueChange={(value) => {
                  setFilter('role', value === TOUS ? undefined : value)
                }}
              />
              <Select
                aria-label="Filtrer par statut"
                className="w-auto min-w-40"
                options={STATUT_FILTER}
                value={actifParam ?? TOUS}
                onValueChange={(value) => {
                  setFilter('actif', value === TOUS ? undefined : value)
                }}
              />
            </FilterBar>
          }
        />
      </div>

      <UserFormDialog open={formOpen} onOpenChange={setFormOpen} utilisateur={editing} />
      <ResetPasswordDialog
        utilisateur={resetting}
        onOpenChange={(open) => {
          if (!open) setResetting(null)
        }}
      />
      <ConfirmDialog
        open={toggling !== null}
        onOpenChange={(open) => {
          if (!open) setToggling(null)
        }}
        tone={toggling?.actif ? 'danger' : 'default'}
        title={toggling?.actif ? 'Désactiver ce compte ?' : 'Réactiver ce compte ?'}
        description={
          toggling?.actif
            ? `${toggling.prenom} ${toggling.nom} ne pourra plus se connecter. Ses données et son historique sont conservés.`
            : `${toggling?.prenom ?? ''} ${toggling?.nom ?? ''} pourra de nouveau se connecter.`
        }
        confirmLabel={toggling?.actif ? 'Désactiver' : 'Réactiver'}
        onConfirm={async () => {
          if (!toggling) return
          await changerStatut.mutateAsync({ id: toggling.id, actif: !toggling.actif }).catch(() => undefined)
        }}
      />
    </>
  )
}
