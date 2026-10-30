package sn.cse.suivi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.command.LivraisonRecue;
import sn.cse.suivi.application.fake.Memoire;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.StatutAffectation;
import sn.cse.suivi.domain.model.TypeEngin;
import sn.cse.suivi.domain.service.PolitiqueAffectation;

class TypesLivraisonAlertesServicesTest extends ServiceTestSupport {

  private final GererTypesEnginsService types = new GererTypesEnginsService(m.types, m.materiels);
  private final EnregistrerLivraisonService livraison =
      new EnregistrerLivraisonService(
          m.materiels,
          m.affectations,
          m.mouvements,
          m.sequences,
          m.chargeur,
          m.evenements,
          m.parametres,
          m.clock);
  private final DeclencherAlertesService alertes =
      new DeclencherAlertesService(
          m.materiels,
          m.maintenances,
          m.affectations,
          m.chargeur,
          m.evenements,
          m.parametres,
          m.clock);

  private static DonneesTypeEngin donnees(
      String code, CategorieMateriel categorie, Integer heures) {
    return new DonneesTypeEngin(code, "Niveleuse", categorie, heures, 180, true);
  }

  private static LivraisonRecue livraison(UUID chantier, BigDecimal tarif) {
    return new LivraisonRecue(
        UUID.randomUUID(),
        "CTR-2026-001",
        UUID.randomUUID(),
        chantier,
        UUID.randomUUID(),
        "Camion benne 25 t",
        CategorieMateriel.MATERIEL_ROULANT,
        2,
        new BigDecimal("136000000"),
        tarif,
        Memoire.AUJOURDHUI.minusDays(1));
  }

  @Test
  void should_manage_engine_types_and_propagate_thresholds() {
    TypeEngin cree = types.creer(donnees("NIVELEUSE", CategorieMateriel.MATERIEL_ROULANT, 500));
    Materiel rattache =
        m.materiels.save(
            Materiel.enregistrer(
                description("NIV-01", cree.getId()),
                cree,
                0,
                Materiel.Origine.AUCUNE,
                Memoire.AUJOURDHUI));

    types.modifier(cree.getId(), donnees("NIVELEUSE", CategorieMateriel.MATERIEL_ROULANT, 300));

    assertThat(types.lister()).hasSize(1);
    assertThat(m.materiels.findById(rattache.getId()).orElseThrow().getSeuilMaintenanceHeures())
        .isEqualTo(300);
    assertThatThrownBy(() -> types.creer(donnees("NIVELEUSE", CategorieMateriel.OUTILLAGE, null)))
        .extracting("code")
        .isEqualTo("CODE_DEJA_UTILISE");
    assertThatThrownBy(
            () ->
                types.modifier(cree.getId(), donnees("NIVELEUSE", CategorieMateriel.OUTILLAGE, 1)))
        .extracting("code")
        .isEqualTo("TYPE_ENGIN_UTILISE");
    assertThatThrownBy(
            () -> types.modifier(UUID.randomUUID(), donnees("X", CategorieMateriel.OUTILLAGE, 1)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_register_delivered_units_and_affect_them_to_active_site() {
    Chantier chantier = chantier("CH-1");

    List<UUID> ids = livraison.enregistrer(livraison(chantier.getId(), new BigDecimal("180000")));

    assertThat(ids).hasSize(2);
    List<Materiel> livres = m.materiels.findAllById(ids);
    assertThat(livres)
        .extracting(Materiel::getCode)
        .containsExactlyInAnyOrder("ENG-2026-0001", "ENG-2026-0002");
    assertThat(livres).allMatch(x -> x.getEtat() == EtatMateriel.AFFECTE);
    assertThat(livres)
        .allMatch(x -> x.getDescription().valeurAcquisition().longValue() == 68_000_000);
    assertThat(livres).allMatch(x -> x.getTarifJournalier().longValue() == 180_000);
    for (UUID id : ids) {
      Affectation a = m.affectations.findActiveByMateriel(id).orElseThrow();
      assertThat(a.getStatut()).isEqualTo(StatutAffectation.EN_ATTENTE_RECEPTION);
      assertThat(a.getContratId()).isNotNull();
      assertThat(a.getDateFinPrevue()).isEqualTo(a.getDateDebut().plusDays(179));
    }
    assertThat(m.evenements.types())
        .containsExactly(
            EventTypes.MATERIEL_ENREGISTRE, EventTypes.MATERIEL_AFFECTE,
            EventTypes.MATERIEL_ENREGISTRE, EventTypes.MATERIEL_AFFECTE);
  }

  @Test
  void should_keep_delivered_units_in_park_when_site_unknown_or_closed() {
    Chantier termine = chantier("CH-9");
    termine.terminer(0);
    Chantier ferme = m.chantiers.save(termine);

    List<UUID> inconnu = livraison.enregistrer(livraison(UUID.randomUUID(), null));
    List<UUID> clos = livraison.enregistrer(livraison(ferme.getId(), null));
    List<UUID> sans = livraison.enregistrer(livraison(null, null));

    assertThat(m.materiels.findAllById(inconnu))
        .allMatch(x -> x.getEtat() == EtatMateriel.DISPONIBLE);
    assertThat(m.materiels.findAllById(clos)).allMatch(x -> x.getEtat() == EtatMateriel.DISPONIBLE);
    // Tarif déduit de la valeur unitaire (ratio configuré 0,2 %).
    assertThat(m.materiels.findAllById(sans))
        .allMatch(x -> x.getTarifJournalier().longValue() == 136_000);
  }

  @Test
  void should_reject_empty_delivery() {
    LivraisonRecue vide =
        new LivraisonRecue(
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            "x",
            CategorieMateriel.CONSOMMABLE,
            0,
            null,
            null,
            null);

    assertThatThrownBy(() -> livraison.enregistrer(vide))
        .isInstanceOf(IllegalArgumentException.class);
    LivraisonRecue sansMontant =
        new LivraisonRecue(
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            "Gants",
            CategorieMateriel.CONSOMMABLE,
            1,
            null,
            null,
            null);
    List<UUID> ids = livraison.enregistrer(sansMontant);
    assertThat(m.materiels.findAllById(ids).get(0).getTarifJournalier()).isEqualByComparingTo("0");
  }

  @Test
  void should_alert_preventive_maintenance_once_per_reminder_delay() {
    Materiel due = materiel("PEL-01");
    due.releverHeures(1600);
    m.materiels.save(due);
    materiel("PEL-02");

    assertThat(alertes.alerterMaintenancesPreventives()).isEqualTo(1);
    assertThat(alertes.alerterMaintenancesPreventives()).isZero();
    assertThat(m.evenements.publies.get(0).type()).isEqualTo(EventTypes.ALERTE_MAINTENANCE);
    assertThat(m.evenements.publies.get(0).motif()).contains("600 h").contains("seuil 500 h");
  }

  @Test
  void should_alert_end_of_affectation_at_j_minus_3_once() {
    Chantier chantier = chantier("CH-1");
    Materiel proche = materiel("PEL-01");
    m.affectations.save(
        PolitiqueAffectation.affecter(
            proche,
            chantier,
            null,
            new Periode(Memoire.AUJOURDHUI.minusDays(30), Memoire.AUJOURDHUI.plusDays(3)),
            List.of()));
    m.materiels.save(proche);
    affecter(materiel("PEL-02"), chantier);

    assertThat(alertes.alerterFinsAffectation()).isEqualTo(1);
    assertThat(alertes.alerterFinsAffectation()).isZero();
    assertThat(m.evenements.types()).containsExactly(EventTypes.FIN_AFFECTATION_PROCHAINE);
  }
}
