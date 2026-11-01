package sn.cse.events;

/** Types d'événements publiés par les microservices (voir docs/API_CONTRACT.md §11). */
public final class EventTypes {

  // ms-besoins
  public static final String BESOIN_SOUMIS = "BESOIN_SOUMIS";
  public static final String BESOIN_MODIFIE = "BESOIN_MODIFIE";
  public static final String BESOIN_VALIDE = "BESOIN_VALIDE";
  public static final String BESOIN_REJETE = "BESOIN_REJETE";
  public static final String BESOIN_AFFECTE_DIRECT = "BESOIN_AFFECTE_DIRECT";
  public static final String BESOIN_EN_ACHAT = "BESOIN_EN_ACHAT";

  // ms-suivi
  public static final String CHANTIER_CREE = "CHANTIER_CREE";
  public static final String CHANTIER_MODIFIE = "CHANTIER_MODIFIE";
  public static final String MATERIEL_ENREGISTRE = "MATERIEL_ENREGISTRE";
  public static final String MATERIEL_ETAT_CHANGE = "MATERIEL_ETAT_CHANGE";
  public static final String MATERIEL_REFORME_DEMANDEE = "MATERIEL_REFORME_DEMANDEE";
  public static final String MATERIEL_REFORME = "MATERIEL_REFORME";
  public static final String ALERTE_MAINTENANCE = "ALERTE_MAINTENANCE";
  public static final String MATERIEL_AFFECTE = "MATERIEL_AFFECTE";
  public static final String RECEPTION_CONFIRMEE = "RECEPTION_CONFIRMEE";
  public static final String AFFECTATION_TERMINEE = "AFFECTATION_TERMINEE";
  public static final String FIN_AFFECTATION_PROCHAINE = "FIN_AFFECTATION_PROCHAINE";
  public static final String MAINTENANCE_PLANIFIEE = "MAINTENANCE_PLANIFIEE";
  public static final String MAINTENANCE_DEMARREE = "MAINTENANCE_DEMARREE";
  public static final String MAINTENANCE_CLOTUREE = "MAINTENANCE_CLOTUREE";
  public static final String PANNE_SIGNALEE = "PANNE_SIGNALEE";

  // ms-fournisseurs
  public static final String FOURNISSEUR_INSCRIT = "FOURNISSEUR_INSCRIT";
  public static final String FOURNISSEUR_VALIDE = "FOURNISSEUR_VALIDE";
  public static final String FOURNISSEUR_SUSPENDU = "FOURNISSEUR_SUSPENDU";
  public static final String FOURNISSEUR_REACTIVE = "FOURNISSEUR_REACTIVE";
  public static final String FOURNISSEUR_BLACKLISTAGE_DEMANDE = "FOURNISSEUR_BLACKLISTAGE_DEMANDE";
  public static final String FOURNISSEUR_BLACKLISTE = "FOURNISSEUR_BLACKLISTE";
  public static final String FOURNISSEUR_BLACKLISTAGE_REFUSE = "FOURNISSEUR_BLACKLISTAGE_REFUSE";
  public static final String FOURNISSEUR_MODIFIE = "FOURNISSEUR_MODIFIE";
  public static final String SCORE_RECALCULE = "SCORE_RECALCULE";
  public static final String DOCUMENT_EXPIRE = "DOCUMENT_EXPIRE";
  public static final String LITIGE_OUVERT = "LITIGE_OUVERT";
  public static final String LITIGE_RESOLU = "LITIGE_RESOLU";

  // ms-achats
  public static final String AO_PUBLIE = "AO_PUBLIE";
  public static final String OFFRE_DEPOSEE = "OFFRE_DEPOSEE";
  public static final String AO_CLOTURE = "AO_CLOTURE";
  public static final String ATTRIBUTION_PROPOSEE = "ATTRIBUTION_PROPOSEE";
  public static final String ATTRIBUTION_REFUSEE = "ATTRIBUTION_REFUSEE";
  public static final String MARCHE_ATTRIBUE = "MARCHE_ATTRIBUE";
  public static final String CONTRAT_GENERE = "CONTRAT_GENERE";
  public static final String CONTRAT_ENVOYE_SIGNATURE = "CONTRAT_ENVOYE_SIGNATURE";
  public static final String CONTRAT_SIGNATURE_REFUSEE = "CONTRAT_SIGNATURE_REFUSEE";
  public static final String CONTRAT_SIGNE = "CONTRAT_SIGNE";
  public static final String ENGAGEMENT_DEMANDE = "ENGAGEMENT_DEMANDE";
  public static final String ENGAGEMENT_VALIDE = "ENGAGEMENT_VALIDE";
  public static final String ENGAGEMENT_REJETE = "ENGAGEMENT_REJETE";
  public static final String LIVRAISON_DEMARREE = "LIVRAISON_DEMARREE";
  public static final String LIVRAISON_EFFECTUEE = "LIVRAISON_EFFECTUEE";
  public static final String ALERTE_RETARD_LIVRAISON = "ALERTE_RETARD_LIVRAISON";
  public static final String CONTRAT_CLOTURE = "CONTRAT_CLOTURE";
  public static final String FACTURE_RECUE = "FACTURE_RECUE";
  public static final String FACTURE_MANDATEE = "FACTURE_MANDATEE";
  public static final String FACTURE_PAYEE = "FACTURE_PAYEE";
  public static final String FACTURE_REJETEE = "FACTURE_REJETEE";

  // ms-auth
  public static final String UTILISATEUR_CREE = "UTILISATEUR_CREE";
  public static final String UTILISATEUR_MODIFIE = "UTILISATEUR_MODIFIE";
  public static final String UTILISATEUR_SYNCHRONISE = "UTILISATEUR_SYNCHRONISE";

  // transverse
  public static final String AUDIT = "AUDIT";

  private EventTypes() {}
}
