package sn.cse.suivi.domain.model;

/** Gravité d'une panne signalée. */
public enum GravitePanne {
  /** N'empêche pas l'utilisation. */
  MINEURE,
  /** Utilisation dégradée. */
  MAJEURE,
  /** Matériel inutilisable. */
  BLOQUANTE
}
