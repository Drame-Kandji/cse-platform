package sn.cse.besoins.infrastructure.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;
import sn.cse.besoins.application.port.out.BesoinRepository;
import sn.cse.besoins.application.port.out.GenerateurReference;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;
import sn.cse.besoins.domain.model.EvenementBesoin;
import sn.cse.besoins.domain.validation.ChaineValidation;
import sn.cse.besoins.infrastructure.messaging.OutboxBesoinEventPublisher;
import sn.cse.common.outbox.OutboxWriter;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.security.KeycloakJwtConverter;

/**
 * Données de démonstration (profil {@code dev}) : environ 40 besoins répartis sur les 9 derniers
 * mois, tous statuts et priorités, historiques cohérents. Passe par l'agrégat (règles appliquées)
 * et l'outbox : chaque événement est daté de l'instant de l'action simulée et attribué à son
 * auteur, afin que reporting, notification et audit reçoivent un historique réaliste. Idempotent :
 * rien n'est fait si le premier besoin de démonstration existe. ms-suivi n'est jamais appelé.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "cse.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);
  private static final LocalTime HEURE_SOUMISSION = LocalTime.of(8, 0);
  private static final Duration DELAI_ORIENTATION = Duration.ofMinutes(1);
  private static final String JETON_DEMO = "seed";

  private final BesoinRepository repository;
  private final ChaineValidation chaine;
  private final GenerateurReference references;
  private final TransactionOperations transactions;
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final Clock clock;
  private final String source;

  /**
   * @param repository dépôt des besoins
   * @param chaine chaîne de validation
   * @param references générateur de références
   * @param transactions transactions
   * @param jdbc accès JDBC (outbox)
   * @param mapper sérialiseur
   * @param clock horloge
   * @param source nom du service
   */
  public DevDataSeeder(
      BesoinRepository repository,
      ChaineValidation chaine,
      GenerateurReference references,
      TransactionOperations transactions,
      JdbcTemplate jdbc,
      ObjectMapper mapper,
      Clock clock,
      @Value("${spring.application.name}") String source) {
    this.repository = repository;
    this.chaine = chaine;
    this.references = references;
    this.transactions = transactions;
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.clock = clock;
    this.source = source;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (repository.existe(JeuDeDonneesBesoins.besoinId(1))) {
      LOG.info("Données de démonstration des besoins déjà présentes : rien à faire");
      return;
    }
    List<ScenarioBesoin> scenarios = JeuDeDonneesBesoins.scenarios();
    // Ordre chronologique : les références suivent l'ordre de soumission.
    scenarios.stream()
        .sorted((a, b) -> Integer.compare(b.joursAvant(), a.joursAvant()))
        .forEach(s -> transactions.executeWithoutResult(statut -> jouer(s)));
    LOG.info("{} besoins de démonstration créés", scenarios.size());
  }

  private void jouer(ScenarioBesoin s) {
    Instant maintenant = Instant.now(clock);
    LocalDate jourSoumission =
        LocalDate.ofInstant(maintenant, ZoneOffset.UTC).minusDays(s.joursAvant());
    Instant soumission =
        jourSoumission
            .atTime(HEURE_SOUMISSION)
            .toInstant(ZoneOffset.UTC)
            .plusSeconds(s.numero() * 60L);
    if (soumission.isAfter(maintenant)) {
      soumission = maintenant.minusSeconds(60);
    }
    Instant decision = soumission.plus(Duration.ofHours(s.heuresDecision()));
    Acteur demandeur = JeuDeDonneesBesoins.demandeur(s.chantier());
    Acteur rm = JeuDeDonneesBesoins.IBRAHIMA_FALL;

    BesoinMateriel besoin =
        BesoinMateriel.soumettre(
            JeuDeDonneesBesoins.besoinId(s.numero()),
            references.prochaineReference(jourSoumission.getYear()),
            JeuDeDonneesBesoins.chantierId(s.chantier()),
            JeuDeDonneesBesoins.CHANTIERS.get(s.chantier()),
            demandeur,
            new DescriptionBesoin(
                s.designation(),
                s.categorie(),
                null,
                s.quantite(),
                s.specifications(),
                s.priorite(),
                s.dureeJours(),
                jourSoumission.plusDays(s.delaiBesoinJours()),
                s.justification()),
            soumission);
    publier(besoin, demandeur);

    Instant orientation = decision.plus(DELAI_ORIENTATION);
    switch (s.issue()) {
      case EN_ATTENTE -> {
        // Aucune décision.
      }
      case REJETE -> {
        besoin.rejeter(chaine, rm, s.motifRejet(), decision);
        publier(besoin, rm);
      }
      case VALIDE_ORIENTATION_EN_ATTENTE -> {
        valider(besoin, rm, decision);
        besoin.suspendreOrientation(
            List.of(),
            rm,
            "Service de suivi du parc indisponible : orientation à relancer.",
            orientation);
      }
      case AFFECTE_DIRECT -> {
        valider(besoin, rm, decision);
        besoin.affecterDirectement(
            JeuDeDonneesBesoins.materielsAffectes(s.numero()), rm, orientation);
        publier(besoin, rm);
      }
      case EN_ACHAT -> {
        valider(besoin, rm, decision);
        besoin.orienterVersAchat(
            List.of(),
            rm,
            "0 matériel(s) disponible(s) pour "
                + s.quantite()
                + " demandé(s) : transmission au service Achats.",
            orientation);
        publier(besoin, rm);
      }
    }
    repository.enregistrer(besoin);
  }

  private void valider(BesoinMateriel besoin, Acteur rm, Instant decision) {
    besoin.valider(chaine, rm, "Besoin conforme au planning du chantier.", decision);
    publier(besoin, rm);
  }

  /** Publie les événements en attente, datés de leur instant et attribués à leur auteur. */
  private void publier(BesoinMateriel besoin, Acteur auteur) {
    List<EvenementBesoin> evenements = besoin.extraireEvenements();
    try {
      SecurityContextHolder.getContext().setAuthentication(authentification(auteur));
      for (EvenementBesoin evenement : evenements) {
        OutboxWriter ecrivain =
            new OutboxWriter(
                jdbc,
                mapper,
                new CurrentUserProvider(),
                Clock.fixed(evenement.date(), ZoneOffset.UTC),
                source);
        new OutboxBesoinEventPublisher(ecrivain).publier(besoin, evenement);
      }
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private static JwtAuthenticationToken authentification(Acteur acteur) {
    Instant emission = Instant.EPOCH;
    Jwt jwt =
        Jwt.withTokenValue(JETON_DEMO)
            .header("alg", "none")
            .subject(acteur.id().toString())
            .claim("name", acteur.nom())
            .claim("realm_access", Map.of("roles", List.copyOf(acteur.roles())))
            .issuedAt(emission)
            .expiresAt(emission.plus(Duration.ofDays(1)))
            .build();
    return new JwtAuthenticationToken(jwt, KeycloakJwtConverter.extraireAutorites(jwt));
  }
}
