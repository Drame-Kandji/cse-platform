package sn.cse.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.auth.domain.Fixtures.CHANTIER_1;
import static sn.cse.auth.domain.Fixtures.CHANTIER_2;
import static sn.cse.auth.domain.Fixtures.CREATION;
import static sn.cse.auth.domain.Fixtures.FOURNISSEUR;
import static sn.cse.auth.domain.Fixtures.ID;
import static sn.cse.auth.domain.Fixtures.IDENTITE;
import static sn.cse.auth.domain.Fixtures.TOUS_ATTRIBUTS;
import static sn.cse.auth.domain.Fixtures.utilisateur;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import sn.cse.auth.domain.exception.AttributProfilInvalideException;
import sn.cse.auth.domain.model.AdministrateurSysteme;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.ChefDeChantier;
import sn.cse.auth.domain.model.DAF;
import sn.cse.auth.domain.model.DirectionGenerale;
import sn.cse.auth.domain.model.Fournisseur;
import sn.cse.auth.domain.model.PolitiqueOtp;
import sn.cse.auth.domain.model.ResponsableAchats;
import sn.cse.auth.domain.model.ResponsableMateriel;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;

class UtilisateurTest {

  @Test
  void should_instancier_la_sous_classe_du_role_when_fabrique() {
    assertThat(utilisateur(Role.CHEF_CHANTIER)).isInstanceOf(ChefDeChantier.class);
    assertThat(utilisateur(Role.RESPONSABLE_MATERIEL)).isInstanceOf(ResponsableMateriel.class);
    assertThat(utilisateur(Role.RESPONSABLE_ACHATS)).isInstanceOf(ResponsableAchats.class);
    assertThat(utilisateur(Role.DAF)).isInstanceOf(DAF.class);
    assertThat(utilisateur(Role.DIRECTION_GENERALE)).isInstanceOf(DirectionGenerale.class);
    assertThat(utilisateur(Role.ADMIN_SYSTEME)).isInstanceOf(AdministrateurSysteme.class);
    assertThat(utilisateur(Role.FOURNISSEUR)).isInstanceOf(Fournisseur.class);
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void should_respecter_le_contrat_commun_when_substitue(Role role) {
    // Liskov : toute sous-classe accepte n'importe quels attributs et conserve l'identité.
    Utilisateur u = utilisateur(role);
    assertThat(u.role()).isEqualTo(role);
    assertThat(u.id()).isEqualTo(ID);
    assertThat(u.identite()).isEqualTo(IDENTITE);
    assertThat(u.nomComplet()).isEqualTo("Mamadou Diop");
    assertThat(u.createdAt()).isEqualTo(CREATION);
    u.modifierAttributs(AttributsProfil.AUCUN);
    u.modifierAttributs(TOUS_ATTRIBUTS);
    u.definirActif(false);
    u.definirOtpConfigure(true);
    assertThat(u.actif()).isFalse();
    assertThat(u.otpConfigure()).isTrue();
    assertThat(u.instantane().role()).isEqualTo(role);
  }

  @Test
  void should_ne_retenir_que_les_attributs_du_profil_when_creation() {
    assertThat(utilisateur(Role.CHEF_CHANTIER).attributs())
        .isEqualTo(new AttributsProfil(Set.of(CHANTIER_1, CHANTIER_2), null, null, null, null));
    assertThat(utilisateur(Role.RESPONSABLE_MATERIEL).attributs().perimetreParc())
        .isEqualTo("NATIONAL");
    assertThat(utilisateur(Role.RESPONSABLE_ACHATS).attributs().seuilDelegation())
        .isEqualByComparingTo("150000000");
    assertThat(utilisateur(Role.DAF).attributs().budgetGere()).isEqualByComparingTo("5000000000");
    assertThat(utilisateur(Role.FOURNISSEUR).attributs().fournisseurId()).isEqualTo(FOURNISSEUR);
    assertThat(utilisateur(Role.DIRECTION_GENERALE).attributs()).isEqualTo(AttributsProfil.AUCUN);
    assertThat(utilisateur(Role.ADMIN_SYSTEME).attributs()).isEqualTo(AttributsProfil.AUCUN);
  }

  @Test
  void should_exposer_les_regles_specifiques_when_profil_concret() {
    ChefDeChantier chef = (ChefDeChantier) utilisateur(Role.CHEF_CHANTIER);
    assertThat(chef.estAffecteAu(CHANTIER_1)).isTrue();
    assertThat(chef.estAffecteAu(FOURNISSEUR)).isFalse();
    assertThat(chef.chantierIds()).containsExactlyInAnyOrder(CHANTIER_1, CHANTIER_2);

    ResponsableAchats ra = (ResponsableAchats) utilisateur(Role.RESPONSABLE_ACHATS);
    assertThat(ra.depasseSeuil(new BigDecimal("150000001"))).isTrue();
    assertThat(ra.depasseSeuil(new BigDecimal("150000000"))).isFalse();
    ra.modifierAttributs(AttributsProfil.AUCUN);
    assertThat(ra.seuilDelegation()).isNull();
    assertThat(ra.depasseSeuil(BigDecimal.TEN)).isFalse();

    Fournisseur f = (Fournisseur) utilisateur(Role.FOURNISSEUR);
    assertThat(f.estRattache()).isTrue();
    assertThat(f.fournisseurId()).isEqualTo(FOURNISSEUR);
    f.modifierAttributs(AttributsProfil.AUCUN);
    assertThat(f.estRattache()).isFalse();

    assertThat(((ResponsableMateriel) utilisateur(Role.RESPONSABLE_MATERIEL)).perimetreParc())
        .isEqualTo("NATIONAL");
    assertThat(((DAF) utilisateur(Role.DAF)).budgetGere()).isEqualByComparingTo("5000000000");
  }

  @Test
  void should_conserver_identite_et_creation_when_changement_de_role() {
    Utilisateur chef = utilisateur(Role.CHEF_CHANTIER);
    chef.definirActif(false);
    Utilisateur rm =
        chef.convertirEn(
            Role.RESPONSABLE_MATERIEL, new AttributsProfil(null, "REGION_DAKAR", null, null, null));
    assertThat(rm).isInstanceOf(ResponsableMateriel.class);
    assertThat(rm.id()).isEqualTo(ID);
    assertThat(rm.identite()).isEqualTo(IDENTITE);
    assertThat(rm.actif()).isFalse();
    assertThat(rm.createdAt()).isEqualTo(CREATION);
    assertThat(rm.attributs().perimetreParc()).isEqualTo("REGION_DAKAR");
    assertThat(rm.attributs().chantierIds()).isEmpty();
  }

  @Test
  void should_modifier_sur_place_when_role_inchange() {
    Utilisateur chef = utilisateur(Role.CHEF_CHANTIER);
    Utilisateur resultat =
        chef.convertirEn(
            Role.CHEF_CHANTIER, new AttributsProfil(Set.of(CHANTIER_1), null, null, null, null));
    assertThat(resultat).isSameAs(chef);
    assertThat(resultat.attributs().chantierIds()).containsExactly(CHANTIER_1);
  }

  @Test
  void should_refuser_une_identite_incomplete_when_nom_vide() {
    Utilisateur u = utilisateur(Role.DAF);
    assertThatThrownBy(
            () -> u.modifierIdentite(new Utilisateur.Identite("daf@cse.sn", "Ousmane", " ", null)))
        .isInstanceOf(AttributProfilInvalideException.class)
        .hasMessageContaining("nom");
    assertThatThrownBy(
            () -> u.modifierIdentite(new Utilisateur.Identite(null, "Ousmane", "Ba", null)))
        .isInstanceOf(AttributProfilInvalideException.class);
  }

  @Test
  void should_normaliser_identite_when_espaces_et_telephone_vide() {
    Utilisateur u = utilisateur(Role.DAF);
    u.modifierIdentite(new Utilisateur.Identite(" daf@cse.sn ", " Ousmane ", " Ba ", "  "));
    assertThat(u.email()).isEqualTo("daf@cse.sn");
    assertThat(u.prenom()).isEqualTo("Ousmane");
    assertThat(u.nom()).isEqualTo("Ba");
    assertThat(u.telephone()).isNull();
  }

  @Test
  void should_refuser_un_montant_negatif_when_attributs() {
    assertThatThrownBy(() -> new AttributsProfil(null, null, new BigDecimal("-1"), null, null))
        .isInstanceOf(AttributProfilInvalideException.class)
        .hasMessageContaining("seuilDelegation");
    assertThatThrownBy(() -> new AttributsProfil(null, null, null, new BigDecimal("-5"), null))
        .isInstanceOf(AttributProfilInvalideException.class)
        .hasMessageContaining("budgetGere");
  }

  @Test
  void should_normaliser_les_attributs_when_valeurs_vides() {
    AttributsProfil a = new AttributsProfil(null, "  ", null, null, null);
    assertThat(a.chantierIds()).isEmpty();
    assertThat(a.perimetreParc()).isNull();
  }

  @Test
  void should_detecter_un_changement_when_instantanes_compares() {
    Utilisateur u = utilisateur(Role.CHEF_CHANTIER);
    Utilisateur.Instantane avant = u.instantane();
    assertThat(u.instantane()).isEqualTo(avant);
    u.definirOtpConfigure(true);
    assertThat(u.instantane()).isNotEqualTo(avant);
  }

  @Test
  void should_exiger_otp_selon_la_politique_when_role_sensible() {
    PolitiqueOtp politique = new PolitiqueOtp(Set.of(Role.DAF, Role.ADMIN_SYSTEME));
    assertThat(utilisateur(Role.DAF).otpObligatoire(politique)).isTrue();
    assertThat(utilisateur(Role.CHEF_CHANTIER).otpObligatoire(politique)).isFalse();
    assertThat(new PolitiqueOtp(null).estObligatoire(Role.DAF)).isFalse();
    assertThat(new PolitiqueOtp(Set.of()).rolesObligatoires()).isEmpty();
  }
}
