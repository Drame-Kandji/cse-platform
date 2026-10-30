package sn.cse.besoins.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sn.cse.besoins.Fixtures.CHANTIER_1;
import static sn.cse.besoins.Fixtures.CHANTIER_2;
import static sn.cse.besoins.Fixtures.CHANTIER_3;
import static sn.cse.besoins.Fixtures.MAINTENANT;
import static sn.cse.besoins.Fixtures.USER_CHEF;
import static sn.cse.besoins.Fixtures.USER_RA;
import static sn.cse.besoins.Fixtures.USER_RM;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.cse.besoins.Fixtures;
import sn.cse.besoins.application.command.JournaliserAffectationCommand;
import sn.cse.besoins.application.port.out.BesoinQueryPort;
import sn.cse.besoins.application.query.CriteresRechercheBesoins;
import sn.cse.besoins.application.query.StatistiquesBesoins;
import sn.cse.besoins.domain.model.ActionHistorique;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.HistoriqueBesoin;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.service.BesoinAConsolider;
import sn.cse.besoins.domain.service.ConsolidationBesoins;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

class LecturesBesoinsServiceTest {

  private final Fakes.Depot depot = new Fakes.Depot();
  private final BesoinQueryPort requetes = mock(BesoinQueryPort.class);
  private final ConsulterBesoinsService consultation =
      new ConsulterBesoinsService(
          new EnregistrementBesoin(depot, new Fakes.Publication()), requetes);
  private static final PageQuery PAGE = new PageQuery(0, 20, "createdAt", false);

  private static CriteresRechercheBesoins criteres(UUID chantier) {
    return new CriteresRechercheBesoins(chantier, null, null, null, " pelle ", null, null, null);
  }

  @Test
  void should_restreindre_la_recherche_du_chef_a_ses_chantiers() {
    when(requetes.rechercher(any(), any())).thenReturn(PageResponse.of(List.of(), 0, 20, 0));

    consultation.rechercher(criteres(null), PAGE, USER_CHEF);

    ArgumentCaptor<CriteresRechercheBesoins> c =
        ArgumentCaptor.forClass(CriteresRechercheBesoins.class);
    verify(requetes).rechercher(c.capture(), eq(PAGE));
    assertThat(c.getValue().chantiersAutorises()).containsExactlyInAnyOrder(CHANTIER_1, CHANTIER_2);
    assertThat(c.getValue().q()).isEqualTo("pelle");
  }

  @Test
  void should_refuser_au_chef_un_filtre_sur_un_chantier_non_attribue() {
    assertThatThrownBy(() -> consultation.rechercher(criteres(CHANTIER_3), PAGE, USER_CHEF))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  void should_ne_pas_restreindre_le_responsable_materiel() {
    when(requetes.rechercher(any(), any())).thenReturn(PageResponse.of(List.of(), 0, 20, 0));
    consultation.rechercher(criteres(CHANTIER_3), PAGE, USER_RM);
    ArgumentCaptor<CriteresRechercheBesoins> c =
        ArgumentCaptor.forClass(CriteresRechercheBesoins.class);
    verify(requetes).rechercher(c.capture(), eq(PAGE));
    assertThat(c.getValue().chantiersAutorises()).isNull();
  }

  @Test
  void should_controler_l_acces_au_detail_et_a_l_historique() {
    BesoinMateriel b = depot.ajouter(Fixtures.besoinEnAttente(1));
    assertThat(consultation.obtenir(b.id(), USER_CHEF).reference()).isEqualTo("BES-2026-0001");
    assertThat(consultation.historique(b.id(), USER_RA))
        .extracting(HistoriqueBesoin::action)
        .containsExactly(ActionHistorique.SOUMISSION);

    var autreChef =
        new sn.cse.common.security.CurrentUser(
            UUID.randomUUID(),
            "Awa Ndiaye",
            null,
            Set.of("CHEF_CHANTIER"),
            Set.of(CHANTIER_3),
            null);
    assertThatThrownBy(() -> consultation.obtenir(b.id(), autreChef))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasFieldOrPropertyWithValue("code", "ACCES_CHANTIER_REFUSE");
  }

  @Test
  void should_consolider_les_besoins_en_attente_et_valides() {
    ConsoliderBesoinsService service =
        new ConsoliderBesoinsService(
            requetes, new ConsolidationBesoins(30), new ParametresBesoins(500));
    LocalDate d = LocalDate.parse("2026-11-10");
    when(requetes.aConsolider(any(), anyInt()))
        .thenReturn(
            List.of(
                new BesoinAConsolider(
                    UUID.randomUUID(),
                    CHANTIER_1,
                    "A",
                    CategorieMateriel.OUTILLAGE,
                    "Bétonnière 350 L",
                    1,
                    Priorite.NORMAL,
                    d),
                new BesoinAConsolider(
                    UUID.randomUUID(),
                    CHANTIER_1,
                    "A",
                    CategorieMateriel.OUTILLAGE,
                    "Betonniere 350L",
                    2,
                    Priorite.URGENT,
                    d.plusDays(3))));

    assertThat(service.consolider(null))
        .singleElement()
        .satisfies(l -> assertThat(l.doublonPotentiel()).isTrue());
    verify(requetes).aConsolider(EnumSet.of(StatutBesoin.EN_ATTENTE, StatutBesoin.VALIDE), 500);
    service.consolider(StatutBesoin.VALIDE);
    verify(requetes).aConsolider(EnumSet.of(StatutBesoin.VALIDE), 500);
    assertThatThrownBy(() -> service.consolider(StatutBesoin.REJETE))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> new ParametresBesoins(0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_filtrer_les_statistiques_du_chef() {
    StatistiquesBesoinsService service = new StatistiquesBesoinsService(requetes);
    when(requetes.statistiques(any()))
        .thenReturn(new StatistiquesBesoins(3, Map.of(StatutBesoin.VALIDE, 3L), null, 12.5));
    when(requetes.statistiques(isNull())).thenReturn(new StatistiquesBesoins(0, null, null, null));

    StatistiquesBesoins chef = service.statistiques(USER_CHEF);
    assertThat(chef.parStatut())
        .containsEntry(StatutBesoin.VALIDE, 3L)
        .containsEntry(StatutBesoin.REJETE, 0L)
        .hasSize(5);
    assertThat(chef.parPriorite()).hasSize(3).containsEntry(Priorite.NORMAL, 0L);
    verify(requetes).statistiques(Set.of(CHANTIER_1, CHANTIER_2));
    assertThat(service.statistiques(USER_RM).total()).isZero();
  }

  @Test
  void should_journaliser_une_affectation_sur_un_besoin_connu_uniquement() {
    JournaliserAffectationService service = new JournaliserAffectationService(depot);
    BesoinMateriel b = depot.ajouter(Fixtures.besoinEnAttente(1));
    LocalDate debut = LocalDate.parse("2026-11-01");

    boolean fait =
        service.journaliser(
            new JournaliserAffectationCommand(
                b.id(),
                "PEL-001",
                "Pelle hydraulique",
                "Extension autoroute",
                debut,
                debut.plusDays(30),
                Fixtures.RM_ID,
                "Ibrahima Fall",
                MAINTENANT));
    boolean inconnu =
        service.journaliser(
            new JournaliserAffectationCommand(
                UUID.randomUUID(), "X", "Y", "Z", debut, debut, null, null, MAINTENANT));

    assertThat(fait).isTrue();
    assertThat(inconnu).isFalse();
    assertThat(depot.trouver(b.id()).orElseThrow().historique())
        .last()
        .satisfies(
            h -> {
              assertThat(h.action()).isEqualTo(ActionHistorique.MATERIEL_AFFECTE);
              assertThat(h.commentaire()).contains("PEL-001").contains("2026-12-01");
              assertThat(h.auteurNom()).isEqualTo("Ibrahima Fall");
            });
  }
}
