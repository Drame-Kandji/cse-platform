package sn.cse.fournisseurs.infrastructure.seed;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.service.ChargeurFournisseur;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.CoordonneesBancaires;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;
import sn.cse.fournisseurs.domain.model.TypeDocument;
import sn.cse.fournisseurs.domain.service.ScorePolicy;

/**
 * Jeu de démonstration (profil {@code dev}, idempotent), volontairement réduit : 5 fournisseurs
 * actifs du contrat §12 (noms fictifs), inscrits le premier jour du mois courant, avec leurs
 * documents administratifs valides. Aucune évaluation, aucun litige ni catalogue : la démonstration
 * crée elle-même son historique. Les écritures passent par le domaine et l'outbox afin d'alimenter
 * reporting, notification et audit.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "cse.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

  static final UUID F1 = id(1);
  static final UUID F2 = id(2);
  static final UUID F3 = id(3);
  static final UUID F4 = id(4);
  static final UUID F5 = id(5);

  private static final String SENEGAL = "Sénégal";

  private final FournisseurRepository fournisseurs;
  private final ChargeurFournisseur chargeur;
  private final FournisseurEventPublisher evenements;
  private final ScorePolicy politique;
  private final TransactionTemplate transaction;
  private final Clock clock;

  /**
   * @param fournisseurs dépôt des fournisseurs
   * @param chargeur chargement des fournisseurs
   * @param evenements publication outbox
   * @param politique politique de score
   * @param transaction gabarit transactionnel
   * @param clock horloge
   */
  public DevDataSeeder(
      FournisseurRepository fournisseurs,
      ChargeurFournisseur chargeur,
      FournisseurEventPublisher evenements,
      ScorePolicy politique,
      TransactionTemplate transaction,
      Clock clock) {
    this.fournisseurs = fournisseurs;
    this.chargeur = chargeur;
    this.evenements = evenements;
    this.politique = politique;
    this.transaction = transaction;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (fournisseurs.parId(F1).isPresent()) {
      LOG.info("Jeu de démonstration fournisseurs déjà présent : rien à faire");
      return;
    }
    transaction.executeWithoutResult(statut -> semer());
    LOG.info("Jeu de démonstration fournisseurs créé (5 fournisseurs)");
  }

  private void semer() {
    referentiel();
    documents();
  }

  // --- Référentiel ------------------------------------------------------------------------------

  private void referentiel() {
    creer(
        F1,
        new ProfilFournisseur(
            "Sahel Engins SA",
            "005123456 2G3",
            "SN-DKR-2015-B-12345",
            "Km 4,5 boulevard du Centenaire de la Commune",
            "Dakar",
            SENEGAL,
            "+221 33 849 12 34",
            "contact@sahel-engins.sn",
            "Abdoulaye Mbaye",
            "Directeur commercial",
            EnumSet.of(CategorieMateriel.MATERIEL_ROULANT),
            List.of("ISO 9001:2015", "Concessionnaire agréé engins TP")),
        new CoordonneesBancaires("Banque Sahélienne de Commerce", "SN08SN0100152000048500003035"),
        false);
    creer(
        F2,
        new ProfilFournisseur(
            "Teranga Équipements BTP",
            "006234567 2V1",
            "SN-DKR-2017-B-23456",
            "Zone industrielle de Mbao, route de Rufisque",
            "Dakar",
            SENEGAL,
            "+221 33 879 45 60",
            "commercial@teranga-equipements.sn",
            "Mariama Diallo",
            "Responsable grands comptes",
            EnumSet.of(CategorieMateriel.MATERIEL_ROULANT, CategorieMateriel.OUTILLAGE),
            List.of("ISO 9001:2015")),
        new CoordonneesBancaires("Crédit Teranga", "SN12K01001520000256000012345"),
        false);
    creer(
        F3,
        new ProfilFournisseur(
            "Dakar Outillage Pro",
            "004345678 1A2",
            "SN-DKR-2012-B-04567",
            "Avenue Blaise Diagne, Médina",
            "Dakar",
            SENEGAL,
            "+221 33 822 31 77",
            "ventes@dakar-outillage.sn",
            "Ousmane Gueye",
            "Gérant",
            EnumSet.of(CategorieMateriel.OUTILLAGE),
            List.of()),
        new CoordonneesBancaires("Banque Sahélienne de Commerce", "SN08SN0100152000099100004567"),
        false);
    creer(
        F4,
        new ProfilFournisseur(
            "Cap-Vert Informatique",
            "007456789 2C4",
            "SN-DKR-2019-B-34567",
            "Rue Carnot x Joseph Gomis, Plateau",
            "Dakar",
            SENEGAL,
            "+221 33 889 60 10",
            "pro@capvert-informatique.sn",
            "Aïssatou Faye",
            "Chargée d'affaires",
            EnumSet.of(CategorieMateriel.EQUIPEMENT_INFORMATIQUE),
            List.of("Partenaire constructeur Gold")),
        new CoordonneesBancaires("Crédit Teranga", "SN12K01001520000256000067890"),
        false);
    creer(
        F5,
        new ProfilFournisseur(
            "Atlantique Consommables SARL",
            "005567890 2B5",
            "SN-THS-2016-B-00412",
            "Route de Mbour, quartier Grand Thiès",
            "Thiès",
            SENEGAL,
            "+221 33 951 22 48",
            "commandes@atlantique-consommables.sn",
            "Pape Ndiaye",
            "Directeur des opérations",
            EnumSet.of(CategorieMateriel.CONSOMMABLE),
            List.of("Agrément distributeur ciment")),
        new CoordonneesBancaires("Banque du Cayor", "SN15C02002030000100200030040"),
        false);
  }

  private void creer(
      UUID id, ProfilFournisseur profil, CoordonneesBancaires coordonnees, boolean enAttente) {
    Fournisseur fournisseur =
        Fournisseur.creerAvecIdentifiant(
            id,
            profil,
            coordonnees,
            enAttente,
            politique.scoreInitial(),
            aujourdhui().withDayOfMonth(1));
    evenements.publierFournisseur(
        EventTypes.FOURNISSEUR_INSCRIT, fournisseurs.sauvegarder(fournisseur), null);
  }

  // --- Documents --------------------------------------------------------------------------------

  private void documents() {
    LocalDate jour = aujourdhui();
    for (UUID id : identifiants()) {
      Fournisseur f = chargeur.charger(id);
      f.ajouterDocument(TypeDocument.NINEA, "Attestation NINEA", null);
      f.ajouterDocument(TypeDocument.RCCM, "Extrait RCCM " + f.getProfil().rccm(), null);
      f.ajouterDocument(TypeDocument.QUITUS_FISCAL, "Quitus fiscal DGID", jour.plusMonths(6));
      f.ajouterDocument(
          TypeDocument.ASSURANCE, "Assurance responsabilité civile", jour.plusYears(1));
      fournisseurs.sauvegarder(f);
    }
  }

  // --- Utilitaires ------------------------------------------------------------------------------

  private LocalDate aujourdhui() {
    return LocalDate.now(clock);
  }

  private static UUID id(int n) {
    return UUID.fromString(String.format("f0000000-0000-0000-0000-%012d", n));
  }

  /**
   * @return identifiants des fournisseurs de démonstration
   */
  static Set<UUID> identifiants() {
    return Set.of(F1, F2, F3, F4, F5);
  }
}
