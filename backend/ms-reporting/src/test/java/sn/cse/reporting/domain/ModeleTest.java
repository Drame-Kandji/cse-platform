package sn.cse.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;
import sn.cse.reporting.domain.model.EtatMateriel;
import sn.cse.reporting.domain.model.FormatExport;
import sn.cse.reporting.domain.model.Granularite;
import sn.cse.reporting.domain.model.Indicateur;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.Periode.Tranche;
import sn.cse.reporting.domain.model.ProfilTableau;
import sn.cse.reporting.domain.model.Section;
import sn.cse.reporting.domain.model.ValeursEnum;
import sn.cse.reporting.domain.service.PolitiqueAcces;
import sn.cse.reporting.domain.service.PolitiqueAcces.Acces;

/** Période, granularités, profils, indicateurs, formats et politique d'accès (B.13). */
class ModeleTest {

  private static final UUID C1 = UUID.randomUUID();
  private static final UUID C2 = UUID.randomUUID();

  private static CurrentUser utilisateur(Set<UUID> chantiers, String... roles) {
    return new CurrentUser(UUID.randomUUID(), "Test", "t@cse.sn", Set.of(roles), chantiers, null);
  }

  @Test
  void should_appliquer_90_derniers_jours_when_periode_absente() {
    LocalDate aujourdhui = LocalDate.of(2026, 10, 30);
    Periode p = Periode.resoudre(null, null, aujourdhui, 90, 1830);
    assertThat(p.au()).isEqualTo(aujourdhui);
    assertThat(p.jours()).isEqualTo(90);
    assertThat(p.du()).isEqualTo(LocalDate.of(2026, 8, 2));
    assertThat(Periode.resoudre(LocalDate.of(2026, 10, 1), null, aujourdhui, 90, 1830).jours())
        .isEqualTo(30);
  }

  @Test
  void should_refuser_periode_inversee_ou_trop_longue() {
    LocalDate j = LocalDate.of(2026, 10, 30);
    assertThatThrownBy(() -> Periode.resoudre(j, j.minusDays(1), j, 90, 1830))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("début");
    assertThatThrownBy(() -> Periode.resoudre(j.minusDays(400), j, j, 90, 366))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> new Periode(null, j)).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void should_calculer_intersection_et_partie_ecoulee() {
    Periode octobre = new Periode(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    assertThat(octobre.intersection(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 5)))
        .contains(new Periode(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)));
    assertThat(octobre.intersection(null, LocalDate.of(2026, 9, 30))).isEmpty();
    assertThat(octobre.ecouleeAu(LocalDate.of(2026, 10, 30)).orElseThrow().jours()).isEqualTo(30);
    assertThat(octobre.ecouleeAu(LocalDate.of(2026, 9, 1))).isEmpty();
    assertThat(octobre.contient(null)).isFalse();
  }

  @Test
  void should_decouper_par_semaine_iso_mois_et_jour() {
    Periode octobre = new Periode(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30));
    List<Tranche> semaines = octobre.decouper(Granularite.SEMAINE);
    assertThat(semaines).hasSize(5);
    assertThat(semaines.get(0).libelle()).isEqualTo("2026-W40");
    // La première tranche est tronquée au début de la période.
    assertThat(semaines.get(0).periode().du()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(semaines.get(4).periode().au()).isEqualTo(LocalDate.of(2026, 10, 30));

    Periode finAnnee = new Periode(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 4));
    assertThat(finAnnee.decouper(Granularite.SEMAINE))
        .extracting(Tranche::libelle)
        .containsExactly("2026-W53", "2027-W01");
    assertThat(finAnnee.decouper(Granularite.MOIS))
        .extracting(Tranche::libelle)
        .containsExactly("2026-12", "2027-01");
    assertThat(finAnnee.decouper(Granularite.JOUR)).hasSize(6);
    assertThat(finAnnee.decouper(Granularite.JOUR).get(0).libelle()).isEqualTo("2026-12-30");
  }

  @Test
  void should_definir_sections_par_profil_conformement_au_contrat() {
    assertThat(ProfilTableau.DG.sections()).containsExactlyInAnyOrder(Section.values());
    assertThat(ProfilTableau.RESPONSABLE_MATERIEL.sections())
        .containsExactlyInAnyOrder(Section.PARC, Section.BESOINS, Section.CHANTIERS);
    assertThat(ProfilTableau.RESPONSABLE_ACHATS.sections())
        .containsExactlyInAnyOrder(Section.ACHATS, Section.FOURNISSEURS);
    assertThat(ProfilTableau.DAF.sections())
        .containsExactlyInAnyOrder(Section.FINANCES, Section.ACHATS, Section.CHANTIERS);
    assertThat(ProfilTableau.CHEF_CHANTIER.sections())
        .containsExactlyInAnyOrder(Section.BESOINS, Section.PARC, Section.CHANTIERS);
    assertThat(ProfilTableau.depuis("direction_generale")).isEqualTo(ProfilTableau.DG);
    assertThat(ProfilTableau.depuis(" daf ")).isEqualTo(ProfilTableau.DAF);
    assertThatThrownBy(() -> ProfilTableau.depuis("ADMIN"))
        .isInstanceOf(InvalidRequestException.class);
    assertThat(ProfilTableau.DG.libelle()).isEqualTo("Direction Générale");
  }

  @Test
  void should_ouvrir_tous_les_profils_au_DG_et_son_seul_profil_aux_autres() {
    Acces dg =
        PolitiqueAcces.pourTableau(utilisateur(Set.of(), Roles.DIRECTION_GENERALE), null, null);
    assertThat(dg.profil()).isEqualTo(ProfilTableau.DG);
    assertThat(dg.perimetre().restreint()).isFalse();
    assertThat(
            PolitiqueAcces.pourTableau(
                    utilisateur(Set.of(), Roles.DIRECTION_GENERALE), "CHEF_CHANTIER", null)
                .profil())
        .isEqualTo(ProfilTableau.CHEF_CHANTIER);

    CurrentUser rm = utilisateur(Set.of(), Roles.RESPONSABLE_MATERIEL);
    assertThat(PolitiqueAcces.pourTableau(rm, null, null).profil())
        .isEqualTo(ProfilTableau.RESPONSABLE_MATERIEL);
    assertThatThrownBy(() -> PolitiqueAcces.pourTableau(rm, "DG", null))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("Direction Générale");
    assertThatThrownBy(() -> PolitiqueAcces.pourTableau(rm, "DAF", null))
        .isInstanceOf(ForbiddenOperationException.class);

    CurrentUser cumul = utilisateur(Set.of(), Roles.RESPONSABLE_ACHATS, Roles.DAF);
    assertThat(PolitiqueAcces.pourTableau(cumul, null, null).profil()).isEqualTo(ProfilTableau.DAF);
    assertThat(PolitiqueAcces.pourTableau(cumul, "RESPONSABLE_ACHATS", null).profil())
        .isEqualTo(ProfilTableau.RESPONSABLE_ACHATS);
  }

  @Test
  void should_refuser_tableau_when_role_sans_profil() {
    CurrentUser admin = utilisateur(Set.of(), Roles.ADMIN_SYSTEME);
    assertThatThrownBy(() -> PolitiqueAcces.pourTableau(admin, null, null))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  void should_limiter_le_chef_a_ses_chantiers() {
    CurrentUser chef = utilisateur(Set.of(C1), Roles.CHEF_CHANTIER);
    Acces acces = PolitiqueAcces.pourTableau(chef, null, null);
    assertThat(acces.profil()).isEqualTo(ProfilTableau.CHEF_CHANTIER);
    assertThat(acces.perimetre()).isEqualTo(Perimetre.chantiers(Set.of(C1)));
    assertThat(PolitiqueAcces.pourTableau(chef, null, C1).perimetre().chantierIds())
        .containsExactly(C1);
    assertThatThrownBy(() -> PolitiqueAcces.pourTableau(chef, null, C2))
        .isInstanceOf(ForbiddenOperationException.class)
        .extracting("code")
        .isEqualTo("CHANTIER_NON_AUTORISE");

    CurrentUser rm = utilisateur(Set.of(), Roles.RESPONSABLE_MATERIEL);
    assertThat(PolitiqueAcces.pourTableau(rm, null, C2).perimetre().chantierIds())
        .containsExactly(C2);
  }

  @Test
  void should_autoriser_indicateur_selon_sections_des_profils() {
    CurrentUser ra = utilisateur(Set.of(), Roles.RESPONSABLE_ACHATS);
    assertThat(PolitiqueAcces.pourSerie(ra, Indicateur.DELAI_APPROVISIONNEMENT, null).restreint())
        .isFalse();
    assertThatThrownBy(() -> PolitiqueAcces.pourSerie(ra, Indicateur.TAUX_DISPONIBILITE, null))
        .isInstanceOf(ForbiddenOperationException.class)
        .extracting("code")
        .isEqualTo("INDICATEUR_NON_AUTORISE");
    CurrentUser chef = utilisateur(Set.of(C1), Roles.CHEF_CHANTIER);
    assertThat(PolitiqueAcces.pourSerie(chef, Indicateur.BESOINS_SOUMIS, null).chantierIds())
        .containsExactly(C1);
    assertThat(Indicateur.depuis("taux_utilisation")).isEqualTo(Indicateur.TAUX_UTILISATION);
    assertThat(Indicateur.TAUX_UTILISATION.estRatio()).isTrue();
    assertThat(Indicateur.MONTANT_ENGAGE.estRatio()).isFalse();
    assertThatThrownBy(() -> Indicateur.depuis("X")).isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> Indicateur.depuis(null)).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void should_lire_formats_et_enums() {
    assertThat(FormatExport.depuis("PDF")).isEqualTo(FormatExport.PDF);
    assertThat(FormatExport.depuis("xlsx").extension()).isEqualTo("xlsx");
    assertThat(FormatExport.XLSX.typeMime()).contains("spreadsheetml");
    assertThatThrownBy(() -> FormatExport.depuis("csv"))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> FormatExport.depuis(null)).isInstanceOf(InvalidRequestException.class);
    assertThat(ValeursEnum.lire(EtatMateriel.class, "affecte")).isEqualTo(EtatMateriel.AFFECTE);
    assertThat(ValeursEnum.lire(EtatMateriel.class, " ")).isNull();
    assertThatThrownBy(() -> ValeursEnum.lire(EtatMateriel.class, "CASSE"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("EtatMateriel");
    assertThat(EtatMateriel.REFORME.libelle()).isEqualTo("Réformé");
    assertThat(Section.FINANCES.libelle()).isEqualTo("Finances");
  }

  @Test
  void should_valider_parametres_et_perimetre() {
    assertThatThrownBy(() -> new ParametresReporting(90, 30, 400, 5, 365))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(Perimetre.tous().inclut(null)).isTrue();
    assertThat(Perimetre.chantiers(Set.of(C1)).inclut(null)).isFalse();
    assertThat(Perimetre.chantiers(Set.of(C1)).inclut(C1)).isTrue();
    assertThat(new Perimetre(true, null).chantierIds()).isEmpty();
  }
}
