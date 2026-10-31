import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { LogoMark } from '../layout/Logo'
import { ThemeToggle } from '../layout/ThemeToggle'
import { paths } from '../routes'
import { ActionsSection } from './ActionsSection'
import { ChartsSection } from './ChartsSection'
import { DataSection } from './DataSection'
import { FormsSection } from './FormsSection'
import { FoundationsSection } from './FoundationsSection'
import { OverlaysSection } from './OverlaysSection'
import { WorkflowSection } from './WorkflowSection'

const ANCHORS = [
  ['fondations', 'Fondations'],
  ['actions', 'Actions'],
  ['formulaires', 'Formulaires'],
  ['donnees', 'Données'],
  ['surfaces', 'Surfaces'],
  ['workflow', 'Workflow'],
  ['graphiques', 'Graphiques'],
] as const

/** Vitrine du design system (route /design, developpement uniquement). */
export function Component() {
  return (
    <div className="min-h-dvh bg-background">
      <header className="sticky top-0 z-20 border-b border-border bg-background/85 backdrop-blur-md">
        <div className="mx-auto flex h-14 max-w-7xl items-center gap-3 px-4 sm:px-6">
          <LogoMark className="size-7" />
          <p className="text-sm font-semibold">Design system CSE</p>
          <Badge tone="warning" size="sm" className="hidden sm:inline-flex">
            Développement
          </Badge>
          <nav aria-label="Sections" className="ml-4 hidden gap-1 lg:flex">
            {ANCHORS.map(([id, label]) => (
              <a
                key={id}
                href={`#${id}`}
                className="rounded-md px-2.5 py-1.5 text-sm text-muted-foreground hover:bg-muted hover:text-foreground"
              >
                {label}
              </a>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-1">
            <ThemeToggle />
            <Button asChild variant="secondary" size="sm" leftIcon={<ArrowLeft />}>
              <Link to={paths.dashboard}>Application</Link>
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto grid max-w-7xl grid-cols-[minmax(0,1fr)] gap-12 px-4 py-10 sm:px-6">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">
            Composants de la plateforme interne
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Chaque composant vit dans src/shared/ui. Basculez le thème pour vérifier le rendu clair et sombre.
            Conventions : CONVENTIONS.md.
          </p>
        </div>
        <FoundationsSection />
        <ActionsSection />
        <FormsSection />
        <DataSection />
        <OverlaysSection />
        <WorkflowSection />
        <ChartsSection />
      </main>
    </div>
  )
}
