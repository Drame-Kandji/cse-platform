import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
  /** Rendu de secours ; recoit l'erreur et une fonction de reinitialisation. */
  fallback?: (error: Error, reset: () => void) => ReactNode
}

interface State {
  error: Error | null
}

/**
 * Filet de securite global (hors routeur) : evite l'ecran blanc si un fournisseur plante.
 * Les erreurs de page sont gerees par l'errorElement du routeur.
 */
export class ErrorBoundary extends Component<Props, State> {
  override state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  override componentDidCatch(error: Error, info: ErrorInfo): void {
    if (import.meta.env.DEV) console.error('ErrorBoundary', error, info.componentStack)
  }

  private readonly reset = () => {
    this.setState({ error: null })
  }

  override render(): ReactNode {
    const { error } = this.state
    if (!error) return this.props.children
    if (this.props.fallback) return this.props.fallback(error, this.reset)
    return (
      <div role="alert" className="grid min-h-dvh place-items-center bg-background p-6 text-center">
        <div className="max-w-md">
          <h1 className="text-xl font-semibold">L'application a rencontré un problème</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Rechargez la page. Si le problème persiste, contactez le support informatique.
          </p>
          <button
            type="button"
            onClick={() => {
              window.location.reload()
            }}
            className="mt-6 h-10 rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground"
          >
            Recharger la page
          </button>
        </div>
      </div>
    )
  }
}
