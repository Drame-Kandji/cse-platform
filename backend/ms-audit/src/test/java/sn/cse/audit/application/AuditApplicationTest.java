package sn.cse.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import sn.cse.audit.application.port.out.JournalAuditRepository;
import sn.cse.audit.application.service.ConsulterAuditService;
import sn.cse.audit.application.service.EnregistrerTraceService;
import sn.cse.audit.domain.model.AuditLog;
import sn.cse.audit.domain.model.CritereRechercheAudit;
import sn.cse.audit.domain.model.ParametresAudit;
import sn.cse.audit.domain.model.StatistiquesAudit;
import sn.cse.audit.domain.model.StatistiquesAudit.TotalParService;
import sn.cse.audit.domain.model.TraceAction;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

class AuditApplicationTest {

  private static final Instant MAINTENANT = Instant.parse("2026-10-30T12:00:00Z");
  private final Clock clock = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
  private final JournalEnMemoire journal = new JournalEnMemoire();
  private final EnregistrerTraceService enregistrer = new EnregistrerTraceService(journal, clock);
  private final ConsulterAuditService consulter =
      new ConsulterAuditService(journal, new ParametresAudit(7, 20), clock);

  private static TraceAction trace(UUID eventId, String service, Instant quand) {
    return new TraceAction(
        eventId, quand, service, "A", "E", "1", "u", "Nom", List.of("DAF"), "r", null);
  }

  @Test
  void should_journaliser_une_seule_fois_when_meme_evenement_recu_deux_fois() {
    UUID eventId = UUID.randomUUID();
    assertThat(enregistrer.enregistrer(trace(eventId, "ms-achats", MAINTENANT))).isTrue();
    assertThat(enregistrer.enregistrer(trace(eventId, "ms-achats", MAINTENANT))).isFalse();

    assertThat(journal.entrees).hasSize(1);
    assertThat(journal.entrees.get(0).enregistreLe()).isEqualTo(MAINTENANT);
  }

  @Test
  void should_calculer_statistiques_sur_fenetres_utc() {
    enregistrer.enregistrer(trace(UUID.randomUUID(), "ms-achats", MAINTENANT.minusSeconds(3600)));
    enregistrer.enregistrer(
        trace(UUID.randomUUID(), "ms-achats", Instant.parse("2026-10-24T00:00:00Z")));
    enregistrer.enregistrer(
        trace(UUID.randomUUID(), "ms-besoins", Instant.parse("2026-10-23T23:59:59Z")));

    StatistiquesAudit s = consulter.statistiques();

    assertThat(s.total()).isEqualTo(3);
    assertThat(s.aujourdhui()).isEqualTo(1);
    assertThat(s.septDerniersJours()).isEqualTo(2);
    assertThat(s.parService())
        .containsExactly(new TotalParService("ms-achats", 2), new TotalParService("ms-besoins", 1));
  }

  @Test
  void should_deleguer_recherche_when_texte_libre_valide() {
    enregistrer.enregistrer(trace(UUID.randomUUID(), "ms-achats", MAINTENANT));
    PageResponse<AuditLog> page =
        consulter.rechercher(
            new CritereRechercheAudit(null, null, null, "ms-achats", null, null, "r"),
            new PageQuery(0, 20, "occurredAt", false));
    assertThat(page.totalElements()).isEqualTo(1);
  }

  @Test
  void should_refuser_recherche_when_texte_trop_long() {
    CritereRechercheAudit c =
        new CritereRechercheAudit(null, null, null, null, null, null, "x".repeat(21));
    PageQuery page = new PageQuery(0, 20, "occurredAt", false);
    assertThatThrownBy(() -> consulter.rechercher(c, page))
        .isInstanceOf(InvalidRequestException.class);
  }

  /** Faux journal en mémoire, fidèle au contrat du port (ajout seul). */
  static final class JournalEnMemoire implements JournalAuditRepository {

    final List<AuditLog> entrees = new ArrayList<>();

    @Override
    public boolean existePourEvenement(UUID eventId) {
      return entrees.stream().anyMatch(e -> e.eventId().equals(eventId));
    }

    @Override
    public void ajouter(AuditLog entree) {
      entrees.add(entree);
    }

    @Override
    public PageResponse<AuditLog> rechercher(CritereRechercheAudit critere, PageQuery page) {
      List<AuditLog> filtres =
          entrees.stream()
              .filter(e -> critere.service() == null || critere.service().equals(e.service()))
              .sorted(Comparator.comparing(AuditLog::occurredAt).reversed())
              .toList();
      return PageResponse.of(filtres, page.page(), page.size(), filtres.size());
    }

    @Override
    public long compterTout() {
      return entrees.size();
    }

    @Override
    public long compterDepuis(Instant depuis) {
      return entrees.stream().filter(e -> !e.occurredAt().isBefore(depuis)).count();
    }

    @Override
    public List<TotalParService> totalParService() {
      Map<String, Long> parService =
          entrees.stream().collect(Collectors.groupingBy(AuditLog::service, Collectors.counting()));
      return parService.entrySet().stream()
          .map(e -> new TotalParService(e.getKey(), e.getValue()))
          .toList();
    }
  }
}
