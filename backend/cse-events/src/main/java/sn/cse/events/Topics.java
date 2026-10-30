package sn.cse.events;

/** Noms des topics Kafka de la plateforme (un topic par contexte métier + audit). */
public final class Topics {

  public static final String BESOINS = "cse.besoins.events";
  public static final String SUIVI = "cse.suivi.events";
  public static final String FOURNISSEURS = "cse.fournisseurs.events";
  public static final String ACHATS = "cse.achats.events";
  public static final String AUTH = "cse.auth.events";
  public static final String AUDIT = "cse.audit.events";

  /** Suffixe des topics de lettres mortes (Dead Letter Topic). */
  public static final String DLT_SUFFIX = ".DLT";

  private Topics() {}
}
