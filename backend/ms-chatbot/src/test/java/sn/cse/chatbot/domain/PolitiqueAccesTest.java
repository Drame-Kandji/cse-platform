package sn.cse.chatbot.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import sn.cse.chatbot.domain.model.ContexteDonnees;
import sn.cse.chatbot.domain.model.Section;
import sn.cse.chatbot.domain.service.PolitiqueAcces;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.security.Roles;

class PolitiqueAccesTest {

  private final PolitiqueAcces politique = new PolitiqueAcces(Donnees.ACCES, Donnees.MONTANTS);

  private final ContexteDonnees complet =
      ContexteDonnees.vide()
          .avecMateriels(List.of(Donnees.NIVELEUSE))
          .avecBesoins(List.of(Donnees.BESOIN_ATTENTE))
          .avecContrats(List.of(Donnees.CONTRAT))
          .avecAppelsOffres(List.of(Donnees.AO))
          .avecFournisseurs(List.of(Donnees.SAHEL))
          .avecMaintenances(List.of(Donnees.VIDANGE));

  @Test
  void should_refuse_non_internal_accounts() {
    assertThatThrownBy(() -> politique.verifierRole(Donnees.user(Roles.FOURNISSEUR)))
        .isInstanceOf(ForbiddenOperationException.class);
    assertThatThrownBy(() -> politique.verifierRole(Donnees.user()))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  void should_list_sections_allowed_by_role() {
    assertThat(politique.verifierRole(Donnees.chef()))
        .contains(Section.MATERIELS, Section.BESOINS, Section.AFFECTATIONS, Section.PROCEDURES)
        .doesNotContain(Section.CONTRATS, Section.APPELS_OFFRES, Section.FOURNISSEURS);
    assertThat(politique.verifierRole(Donnees.user(Roles.DAF)))
        .contains(Section.CONTRATS, Section.FOURNISSEURS)
        .doesNotContain(Section.BESOINS);
    assertThat(politique.verifierRole(Donnees.user(Roles.ADMIN_SYSTEME)))
        .containsOnly(
            Section.MATERIELS,
            Section.MAINTENANCES,
            Section.AFFECTATIONS,
            Section.CHANTIERS,
            Section.PROCEDURES);
  }

  @Test
  void should_drop_contract_sections_and_amounts_for_chef_de_chantier() {
    ContexteDonnees filtre = politique.appliquerFiltreRole(complet, Donnees.chef());
    assertThat(filtre.contrats()).isEmpty();
    assertThat(filtre.appelsOffres()).isEmpty();
    assertThat(filtre.fournisseurs()).isEmpty();
    assertThat(filtre.masquees())
        .containsExactlyInAnyOrder(Section.CONTRATS, Section.APPELS_OFFRES, Section.FOURNISSEURS);
    assertThat(filtre.besoins()).hasSize(1);
    assertThat(filtre.maintenances()).singleElement().satisfies(m -> assertThat(m.cout()).isNull());
  }

  @Test
  void should_keep_amounts_for_finance_roles() {
    ContexteDonnees filtre = politique.appliquerFiltreRole(complet, Donnees.user(Roles.DAF));
    assertThat(filtre.contrats())
        .singleElement()
        .satisfies(k -> assertThat(k.montant()).isNotNull());
    assertThat(filtre.besoins()).isEmpty();
    assertThat(filtre.masquees()).containsExactly(Section.BESOINS);
  }

  @Test
  void should_hide_amounts_for_admin() {
    ContexteDonnees filtre =
        politique.appliquerFiltreRole(complet, Donnees.user(Roles.ADMIN_SYSTEME));
    assertThat(filtre.maintenances()).singleElement().satisfies(m -> assertThat(m.cout()).isNull());
    assertThat(filtre.contrats()).isEmpty();
  }
}
