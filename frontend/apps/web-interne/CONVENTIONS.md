# Conventions : Plateforme interne CSE (`@cse/web-interne`)

Ce document fixe les règles que **toute** page ou fonctionnalité doit suivre. Le socle (coquille, auth,
HTTP, design system) est stable : on l'utilise, on ne le contourne pas. En cas de doute, regarder la
vitrine `/design` (dev uniquement) et le code de `src/shared/`.

## 1. Démarrer

```bash
pnpm --filter @cse/web-interne dev       # Keycloak (http://localhost:8180, realm cse)
pnpm --filter @cse/web-interne dev:ui    # VITE_AUTH_MODE=dev : sélecteur de rôle, sans Keycloak ni jeton
pnpm --filter @cse/web-interne typecheck | lint | test | build
```

- Port 5173, `/api` proxifié vers la passerelle `http://localhost:8080`.
- Variables : voir `.env.example`. Toute nouvelle variable est déclarée dans `src/vite-env.d.ts`
  **et** validée dans `src/shared/config/env.ts` (zod). Jamais `import.meta.env.X` ailleurs : utiliser `env`.
- Le mode `dev` est refusé hors `vite dev` et exclu du build de production.

## 2. Arborescence

```
src/
  app/                 câblage : router.tsx, routes.ts, access.ts, navigation.ts, providers, layout/, pages/ (403/404/login)
    showcase/          vitrine /design (dev uniquement)
  features/<module>/   une fonctionnalité métier
    api/               fonctions d'appel HTTP + clés de requête   (<module>.api.ts, <module>.keys.ts)
    hooks/             hooks TanStack Query                       (useBesoins.ts, useCreerBesoin.ts)
    components/        composants propres au module
    pages/             pages routées (export `Component`)
    schemas/           schémas zod des formulaires
    types.ts           DTO du module (miroir des records Java)
  shared/
    api/               client HTTP, ApiError, PageResponse, createQueryKeys, QueryClient
    auth/              AuthProvider, useAuth, RequireRole, Can
    config/env.ts      variables d'environnement typées
    domain/labels.ts   enums métier + libellés FR + tons (StatusBadge, filtres, selects)
    i18n/              i18next (namespaces typés)
    lib/               cn, format (FCFA, dates FR), hooks, forms, storage, text
    theme/             thème clair/sombre
    ui/                design system (voir §8)
  locales/fr/*.json    textes d'interface
  test/                setup Vitest + renderWithProviders
```

Règles d'import : `features/*` peut importer `shared/*` et `app/routes`, `app/access` ; **jamais** un autre
feature (passer par `shared/` si c'est partagé) ni `app/layout`. `shared/*` n'importe jamais `features/*`
ni `app/*`. Alias `@/` = `src/`. Pas de `any` (ESLint `strictTypeChecked`), fichiers < ~200 lignes,
TSDoc en français sur les hooks et utilitaires exportés.

## 3. Ajouter une page

1. Créer `src/features/<module>/pages/XxxPage.tsx` qui **exporte `Component`** (chargé paresseusement) :
   ```tsx
   export function Component() {
     return <><PageHeader title="Besoins" actions={<Button>…</Button>} />…</>
   }
   ```
2. Chemin dans `app/routes.ts` (`paths.xxx`, et `to.xxxDetail(id)` pour les liens paramétrés).
   Jamais d'URL en dur dans un composant : `navigate(to.besoinDetail(b.id))`.
3. Droits dans `app/access.ts` (liste de rôles ; liste vide = tout utilisateur connecté).
4. Route dans `app/router.tsx` avec les helpers existants :
   `guarded(access.xxx, [page(paths.xxx, () => import('@/features/…/pages/XxxPage'), 'items.xxx')])`.
   Le 3ᵉ argument est la clé de fil d'Ariane (`navigation:items.*` ou `navigation:crumbs.*`).
5. Entrée de menu dans `app/navigation.ts` (+ libellé dans `locales/fr/navigation.json`). La sidebar, le
   tiroir mobile et la palette Ctrl/Cmd+K se mettent à jour seuls, filtrés par rôle.
6. Remplacer le placeholder `ComingSoon` existant : ne pas l'utiliser pour du contenu définitif.

`RequireRole` n'est qu'ergonomique : la sécurité réelle est côté API (`@PreAuthorize`). Pour masquer
une action : `<Can roles={access.validation}>…</Can>` ou `useHasRole([...])`. Utilisateur courant :
`useCurrentUser()` (`id`, `name`, `email`, `roles`, `chantierIds`).

## 4. Appels API et hooks

Couche HTTP : `shared/api` (fetch typé, sans axios). Elle ajoute le jeton Bearer (rafraîchi s'il expire
dans < 30 s), un `X-Correlation-Id`, et lève une `ApiError` (ProblemDetail RFC 7807 analysé : `status`,
`code`, `title`, `detail`, `traceId`, `fieldErrors`, `userMessage`). Statut 0 = erreur réseau.

```ts
// features/besoins/api/besoins.keys.ts
export const besoinKeys = createQueryKeys('besoins')

// features/besoins/api/besoins.api.ts
export const besoinsApi = {
  lister: (params: BesoinFiltres & PageParams, signal?: AbortSignal) =>
    http.get<PageResponse<Besoin>>('/besoins', { query: params, signal }),
  detail: (id: string) => http.get<Besoin>(`/besoins/${id}`),
  creer: (body: BesoinCreation) => http.post<Besoin>('/besoins', body),
  exporter: (params: BesoinFiltres) => downloadFile('/besoins/export', 'besoins.xlsx', { query: params }),
}

// features/besoins/hooks/useBesoins.ts
/** Liste paginee des besoins. */
export function useBesoins(params: BesoinFiltres & PageParams) {
  return useQuery({
    queryKey: besoinKeys.list(params),
    queryFn: ({ signal }) => besoinsApi.lister(params, signal),
    placeholderData: keepPreviousData,
  })
}

/** Creation d'un besoin (toast de succes automatique via meta). */
export function useCreerBesoin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: besoinsApi.creer,
    meta: { successMessage: 'Besoin enregistré' },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: besoinKeys.lists() }),
  })
}
```

- Clés : **toujours** via `createQueryKeys('<module>')` → `all`, `lists()`, `list(params)`,
  `details()`, `detail(id)`, `custom(...)`. Invalider au niveau le plus fin possible.
- Règles globales (`shared/api/query-client.ts`) : jamais de retry sur 4xx, 2 retries sinon,
  `staleTime` 30 s, pas de refetch au focus (bande passante chantier).
- Erreur de mutation → toast FR automatique. Si le formulaire gère l'erreur : `meta: { silentError: true }`
  puis `applyServerErrors(error, form.setError)` (`shared/lib/forms.ts`) pour les erreurs de champs 400/422.
- Erreur de requête dans une page : `<ErrorState error={query.error} onRetry={() => void query.refetch()} />`.
- Pagination : `PageResponse<T>` (`page` base 0, `size` défaut 20, max 50). Tri Spring `champ,asc|desc`.
- Exports PDF/XLSX : `downloadFile(...)` puis `saveBlob(file.blob, file.filename)`.

## 5. Tableaux (`@/shared/ui/data-table`)

```tsx
const columns: ColumnDef<Besoin>[] = [
  { accessorKey: 'reference', header: 'Référence', enableSorting: true, enableHiding: false },
  { accessorKey: 'designation', header: 'Désignation', meta: { mobilePrimary: true } },
  { accessorKey: 'statut', header: 'Statut', cell: ({ row }) => <StatusBadge kind="statutBesoin" value={row.original.statut} /> },
  { accessorKey: 'montant', header: 'Montant', meta: { align: 'right' }, cell: ({ row }) => formatMoney(row.original.montant) },
]

const table = useTableState({ defaultSort: [{ id: 'dateCreation', desc: true }] }) // état synchronisé avec l'URL
const query = useBesoins({ ...filtres, ...table.pageParams })

<DataTable label="Besoins" columns={columns} data={query.data} isLoading={query.isPending}
  isFetching={query.isFetching} error={query.error} onRetry={() => void query.refetch()}
  pagination={table.pagination} onPaginationChange={table.onPaginationChange}
  sorting={table.sorting} onSortingChange={table.onSortingChange}
  onRowClick={(b) => navigate(to.besoinDetail(b.id))} toolbar={<FilterBar … />} />
```

- Typer `ColumnDef<T>[]` explicitement ; lire les valeurs via `row.original` (pas de `any`).
- Tri **désactivé par défaut** : `enableSorting: true` seulement sur les champs whitelistés côté API ;
  l'`id`/`accessorKey` de colonne = nom du champ de tri backend.
- `meta` : `label`, `align`, `hideOnMobile`, `mobilePrimary`, `headerClassName`, `cellClassName`.
- Sous 768 px, le tableau devient une liste de cartes (personnalisable via `mobileCard`). Avec
  `onRowClick`, ne pas mettre d'éléments interactifs dans les cellules.
- `data` peut aussi être un tableau simple (tri et pagination locaux).
- Filtres : `<FilterBar search onSearchChange (débouncé 300 ms) canReset onReset>` +
  `<FilterChip label options={optionsOf('statutBesoin')} value onChange />` (1 ou 2 visibles), les autres
  dans `moreFilters={…} moreCount={n}` (bouton « Filtres »). Remettre la page à 0 quand
  un filtre change (`table.onPaginationChange((p) => ({ ...p, pageIndex: 0 }))`).

## 6. Formulaires (react-hook-form + zod)

```tsx
// features/besoins/schemas/besoin.schema.ts
export const besoinSchema = z.object({
  designation: z.string().trim().min(3, 'Au moins 3 caractères'),
  priorite: z.enum(PRIORITE),
  dateBesoin: z.date().nullable().refine((d) => d !== null, 'Date obligatoire'),
})

const form = useForm<z.input<typeof besoinSchema>, unknown, z.output<typeof besoinSchema>>({
  resolver: zodResolver(besoinSchema),
  defaultValues: { designation: '', priorite: 'NORMAL', dateBesoin: null },
})

<FormField control={form.control} name="priorite" label="Priorité" required>
  {({ field, control }) => <Select {...control} options={optionsOf('priorite')} value={field.value} onValueChange={field.onChange} />}
</FormField>
```

- `FormField` relie libellé, aide, erreur et attributs ARIA ; toujours étaler `control` sur le champ.
- Hors RHF : `<Field label error>{(p) => <Input {...p} />}</Field>`.
- Messages zod **en français**, rédigés dans le schéma. Dates envoyées à l'API via `toIsoDate()`.
- Contrôles disponibles : `Input`, `Textarea`, `Select`, `Combobox` (recherche, option serveur
  `onSearchChange`), `DatePicker`, `DateRangePicker`, `Checkbox`, `Switch`, `RadioGroup` (`variant="card"`).
- Actions destructives / irréversibles : `<ConfirmDialog tone="danger" onConfirm={async () => …} />`.

## 7. Libellés métier et StatusBadge

Tous les enums backend (valeurs exactes Java) vivent dans `src/shared/domain/labels.ts` avec leur
libellé français et leur ton. Ajouter un enum ou une valeur :

1. constante `export const STATUT_X = ['A', 'B'] as const` + type ;
2. entrée dans `STATUS_MAPS` : `statutX: { A: { label: 'Libellé', tone: 'success' }, … } satisfies DescriptorMap<StatutX>` ;
3. utiliser `<StatusBadge kind="statutX" value={dto.statut} />`, `optionsOf('statutX')`, `labelOf(...)`.

Tons d'un état (`StatusTone`) : **`neutral`** par défaut (gris discret), **`primary`** (bleu) pour
« actif / en cours / à traiter », **`danger`** (rouge) seulement pour erreur, rejet, critique, retard.
Une valeur inconnue s'affiche brute en neutre. Le test `status-badge.test.tsx` vérifie que chaque valeur a
un libellé et un ton parmi ces trois.

## 8. Design system (`src/shared/ui`)

**Charte « simple et moderne » : bleu + blanc.** Règles visuelles, à respecter sur chaque écran :

- **Couleurs** : surfaces blanches sur fond à peine teinté, texte quasi noir + un seul gris
  (`muted-foreground`), bordures très claires. **Un seul bleu** (`primary`) pour l'action principale, le
  lien, le focus, l'élément actif / sélectionné et **tous** les graphiques (`chart-1..5` = nuances de
  bleu, `chart-6` = gris « autres »). **Rouge** (`danger`) uniquement pour erreur, destructif, critique,
  retard. Ni vert, ni orange, ni jaune : `success`/`info`/`warning`/`accent` sont des alias du bleu
  (gardés pour compatibilité, ne pas en ajouter). Sombre : même logique (bleu sur quasi noir).
- **Moins de texte** : `PageHeader` = titre (+ action principale) ; plus de phrase d'accroche
  (`subtitle` seulement pour une donnée courte : chantier, NINEA). Pas de lignes d'aide décoratives
  (« Données consolidées… ») : une info secondaire va en infobulle (`title`) ou dans le détail.
- **Moins d'information** : KPI (`StatCard`) = libellé + grand chiffre + au plus une ligne discrète ;
  pas de tuile d'icône ni de sparkline (opt-in `showSparkline`) ni de delta coloré ; `alert` (rouge) pour
  une vraie alerte. Tableau de bord : 4 KPI, un graphique, « À traiter », lien vers Rapports.
- **Listes** : au-dessus du tableau, des compteurs texte `CountTabs` (pas de rangée de cartes) ;
  tableau ≤ 5 colonnes (le `DataTable` masque au-delà de 6 par défaut, `maxVisibleColumns`), info
  secondaire en 2ᵉ ligne grise ou dans le détail, pas de jauge colorée (texte, rouge seulement si dû).
  Filtres : recherche + 1 ou 2 filtres visibles, le reste dans `FilterBar moreFilters` (bouton « Filtres »).
- **Chrome calme** : plat (pas d'ombre sur les cartes), bordures fines, `rounded-xl`, grille de 8 px ;
  sidebar sans intitulés de groupe (simples filets), icônes grises, élément actif bleu sur fond bleu pâle.
- **Typographie** (Inter) : titre de page `text-2xl`, titre de carte `text-base`, corps `text-sm`,
  petit `text-xs`. Badges = pastilles `rounded-full` sans point coloré.

Importer depuis `@/shared/ui` (barrel) ou le fichier direct ; les composants lourds par leur chemin :
`@/shared/ui/data-table`, `@/shared/ui/chart` + `@/shared/ui/chart-theme` (Recharts).

- **Couleurs : uniquement les jetons** (`bg-surface`, `text-muted-foreground`, `border-border`,
  `bg-primary`, `text-danger-emphasis`, `bg-chart-1`…). Aucune couleur Tailwind brute (`bg-blue-500`,
  `text-slate-600`) ni hex dans les composants : `scripts/check-palette.mjs` le refuse. `*-emphasis` =
  variante texte AA sur fond teinté.
  Classes de ton prêtes : `softToneClasses`, `dotToneClasses`, `textToneClasses` (`shared/ui/tone.ts`).
- Cartes : `Card` / `CardHeader` / `CardContent` / `CardFooter`, coins `rounded-xl`, bordure fine.
- Chiffres : `tabular-nums` ; montants `formatMoney` (FCFA, fr-SN, sans décimales) ; dates
  `formatDate`/`formatDateTime`/`formatRelative` (date-fns, fr) : `shared/lib/format.ts`.
- Pages : `PageHeader` en tête, puis contenu ; états `Skeleton`, `EmptyState`, `ErrorState` ; KPI
  `StatCard` ; compteurs de liste `CountTabs` ; historique `Timeline` ; workflow `Stepper` ; détail latéral `Sheet` ;
  modales `Dialog` ; menus `DropdownMenu` ; `Tabs`, `Tooltip`, `Avatar`, `Badge`.
- Graphiques : `ChartContainer` (libellé accessible obligatoire), `axisProps`, `gridProps`,
  `cursorProps`, `<Tooltip content={<ChartTooltipContent valueFormatter={formatMoney} />} />`,
  `ChartLegend` (≥ 2 séries), couleurs `chartColor(i)` dans l'ordre fixe (bleus puis gris ; max 6 séries,
  jamais deux axes Y ; rouge `rgb(var(--danger))` seulement pour une valeur critique).
- Cartes géographiques : react-leaflet + `CircleMarker` (pas d'icônes image) ; importer
  `leaflet/dist/leaflet.css` dans le composant carte uniquement.
- Icônes : `lucide-react`, `aria-hidden` si décoratives ; bouton icône seul = `IconButton label="…"`.
- Toasts : `toast.success/error` de `sonner` (les erreurs de mutation sont déjà notifiées).
- Accessibilité : focus visible conservé, cibles tactiles ≥ 40 px sur mobile, libellés reliés,
  aucune information portée par la seule couleur. **Aucun emoji** (`scripts/check-no-emoji.mjs`).
- Coquille : `useShell()` permet d'ouvrir la palette (`setPaletteOpen`) ou l'assistant (`setAssistantOpen`).

## 9. Textes et i18n

- Namespaces : `common` (actions, états, tableau, coquille), `navigation`, `auth`. Clés typées.
- Nouveau module : créer `src/locales/fr/<module>.json`, l'ajouter à `resources.fr` dans
  `src/shared/i18n/index.ts`, puis `const { t } = useTranslation('<module>')`.
- Les libellés d'enums restent dans `shared/domain/labels.ts` (pas dans les JSON).

## 10. Tests

- Vitest + Testing Library + MSW (`msw/node`). Fichiers `*.test.ts(x)` à côté du code.
- `renderWithProviders(ui, { user: testUser(['DAF']), route: '/finance' })` (`src/test/render.tsx`)
  fournit routeur mémoire, QueryClient isolé et session simulée.
- Mock HTTP : `server.use(http.get(\`${window.location.origin}/api/...\`, () => HttpResponse.json(...)))`
  (voir `shared/api/http.test.ts`). Couvrir : hooks, schémas, mapping d'erreurs, garde de rôle.
- Avant de livrer : `typecheck`, `lint`, `test`, `build`, puis `node ../../scripts/check-no-emoji.mjs`
  et `node ../../scripts/check-palette.mjs` depuis `frontend/apps/web-interne`.
