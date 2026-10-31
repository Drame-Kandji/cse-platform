import { useEffect, useState } from 'react'
import type { Tone } from '@/shared/domain/labels'
import { toDate } from '@/shared/lib/format'

const MINUTE = 60_000
const HEURE = 60 * MINUTE
const JOUR = 24 * HEURE

/**
 * Temps restant avant une echeance : texte lisible et ton d'urgence.
 * @example decompte('2026-11-02T17:00:00Z', now) // { label: 'J-2 · 5 h', tone: 'warning', expire: false }
 */
export function decompte(echeance: string, now: number): { label: string; tone: Tone; expire: boolean } {
  const cible = toDate(echeance)?.getTime() ?? now
  const reste = cible - now
  if (reste <= 0) return { label: 'Échéance dépassée', tone: 'neutral', expire: true }
  const jours = Math.floor(reste / JOUR)
  const heures = Math.floor((reste % JOUR) / HEURE)
  const minutes = Math.floor((reste % HEURE) / MINUTE)
  const label =
    jours > 0
      ? `J-${jours} · ${heures} h`
      : heures > 0
        ? `${heures} h ${String(minutes).padStart(2, '0')} min`
        : `${Math.max(minutes, 1)} min`
  const tone: Tone = reste < JOUR ? 'danger' : reste < 3 * JOUR ? 'warning' : 'info'
  return { label: `Reste ${label}`, tone, expire: false }
}

/** Horloge rafraichie toutes les 30 secondes (decomptes). */
export function useNow(intervalMs = 30_000): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = window.setInterval(() => {
      setNow(Date.now())
    }, intervalMs)
    return () => {
      window.clearInterval(timer)
    }
  }, [intervalMs])
  return now
}
