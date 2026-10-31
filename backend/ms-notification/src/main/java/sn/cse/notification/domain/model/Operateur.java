package sn.cse.notification.domain.model;

/** Opérateurs disponibles dans les conditions des règles de routage. */
public enum Operateur {
  /** Valeur égale (comparaison textuelle, insensible à la casse). */
  EGAL,
  /** Valeur différente ou absente. */
  DIFFERENT,
  /** Valeur numérique strictement inférieure. */
  INFERIEUR,
  /** Valeur numérique strictement supérieure. */
  SUPERIEUR,
  /** Booléen vrai. */
  VRAI,
  /** Booléen faux ou absent. */
  FAUX,
  /** Champ présent et non vide. */
  PRESENT,
  /** Texte commençant par la valeur (insensible à la casse). */
  COMMENCE_PAR
}
