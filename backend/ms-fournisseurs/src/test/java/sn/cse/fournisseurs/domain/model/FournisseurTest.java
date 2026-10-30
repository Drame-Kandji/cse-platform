package sn.cse.fournisseurs.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.fournisseurs.support.Fixtures.AUJOURDHUI;
import static sn.cse.fournisseurs.support.Fixtures.DG;
import static sn.cse.fournisseurs.support.Fixtures.MAINTENANT;
import static sn.cse.fournisseurs.support.Fixtures.RA;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.CseException;
import sn.cse.fournisseurs.domain.exception.CodesErreur;
import sn.cse.fournisseurs.support.Fixtures;

class FournisseurTest {

  @Test
  void should_creer_actif_avec_score_initial_when_referencement_par_ra() {
    Fournisseur f = Fixtures.actif();
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.ACTIF);
    assertThat(f.getPerformance().scorePerformance()).isEqualByComparingTo("50");
    assertThat(f.getPerformance().tauxLivraisonALHeure()).isEmpty();
    assertThat(f.getCoordonneesBancaires())
        .get()
        .extracting(CoordonneesBancaires::iban)
        .isEqualTo("SN08SN0100152000048500003035");
    assertThat(f.getProfil().email()).isEqualTo("contact@exemple.sn");
    assertThat(f.getVersion()).isZero();
  }

  @Test
  void should_creer_avec_identifiant_impose() {
    UUID id = UUID.randomUUID();
    Fournisseur f =
        Fournisseur.creerAvecIdentifiant(
            id, Fixtures.profil("X", "001234567 1A1"), null, true, BigDecimal.TEN, AUJOURDHUI);
    assertThat(f.getId()).isEqualTo(id);
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.EN_ATTENTE_VALIDATION);
    Fournisseur actif =
        Fournisseur.creerAvecIdentifiant(
            id, Fixtures.profil("X", "001234567 1A1"), null, false, BigDecimal.TEN, AUJOURDHUI);
    assertThat(actif.getStatut()).isEqualTo(StatutFournisseur.ACTIF);
  }

  @Test
  void should_valider_inscription_when_en_attente() {
    Fournisseur f = Fixtures.enAttente();
    f.validerInscription();
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.ACTIF);
  }

  @Test
  void should_refuser_validation_avec_409_when_deja_actif() {
    Fournisseur f = Fixtures.actif();
    assertThatThrownBy(f::validerInscription)
        .isInstanceOf(ConflictException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.TRANSITION_STATUT_INVALIDE);
  }

  @Test
  void should_suspendre_avec_motif_puis_reactiver() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("  Retards répétés  ");
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.SUSPENDU);
    assertThat(f.getMotifStatut()).contains("Retards répétés");
    f.reactiver();
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.ACTIF);
    assertThat(f.getMotifStatut()).isEmpty();
  }

  @Test
  void should_exiger_motif_when_suspension() {
    Fournisseur f = Fixtures.actif();
    assertThatThrownBy(() -> f.suspendre(" "))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("motif");
  }

  @Test
  void should_refuser_suspension_when_en_attente() {
    Fournisseur f = Fixtures.enAttente();
    assertThatThrownBy(() -> f.suspendre("motif")).isInstanceOf(ConflictException.class);
  }

  @Test
  void should_refuser_reactivation_when_actif() {
    Fournisseur f = Fixtures.actif();
    assertThatThrownBy(f::reactiver).isInstanceOf(ConflictException.class);
  }

  @Test
  void should_enregistrer_demande_sans_changer_statut_when_ra_demande_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("Non-conformités");
    f.demanderBlacklistage(RA, "Fraude documentaire", MAINTENANT);
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.SUSPENDU);
    assertThat(f.getDemandeBlacklistage())
        .get()
        .satisfies(
            d -> {
              assertThat(d.demandeurNom()).isEqualTo("Fatou Sarr");
              assertThat(d.motif()).isEqualTo("Fraude documentaire");
              assertThat(d.date()).isEqualTo(MAINTENANT);
            });
  }

  @Test
  void should_refuser_demande_when_fournisseur_actif_R09() {
    Fournisseur f = Fixtures.actif();
    assertThatThrownBy(() -> f.demanderBlacklistage(RA, "motif", MAINTENANT))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("ACTIF → BLACKLISTE");
  }

  @Test
  void should_refuser_double_demande_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("motif");
    f.demanderBlacklistage(RA, "motif", MAINTENANT);
    assertThatThrownBy(() -> f.demanderBlacklistage(RA, "autre", MAINTENANT))
        .isInstanceOf(ConflictException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.DEMANDE_BLACKLISTAGE_EN_COURS);
  }

  @Test
  void should_exiger_motif_when_demande_blacklistage_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("motif");
    assertThatThrownBy(() -> f.demanderBlacklistage(RA, null, MAINTENANT))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_blacklister_when_dg_approuve_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("motif");
    f.demanderBlacklistage(RA, "Fraude documentaire", MAINTENANT);
    DemandeBlacklistage demande = f.deciderBlacklistage(DG, true, "Confirmé par l'audit");
    assertThat(demande.motif()).isEqualTo("Fraude documentaire");
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.BLACKLISTE);
    assertThat(f.getMotifStatut()).contains("Fraude documentaire — Confirmé par l'audit");
    assertThat(f.getDemandeBlacklistage()).isEmpty();
  }

  @Test
  void should_blacklister_avec_motif_de_la_demande_when_dg_approuve_sans_commentaire_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("motif");
    f.demanderBlacklistage(RA, "Fraude", MAINTENANT);
    f.deciderBlacklistage(DG, true, null);
    assertThat(f.getMotifStatut()).contains("Fraude");
  }

  @Test
  void should_conserver_statut_when_dg_refuse_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("Suspension");
    f.demanderBlacklistage(RA, "Fraude", MAINTENANT);
    f.deciderBlacklistage(DG, false, "Preuves insuffisantes");
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.SUSPENDU);
    assertThat(f.getMotifStatut()).contains("Suspension");
    assertThat(f.getDemandeBlacklistage()).isEmpty();
    f.reactiver();
    assertThat(f.getStatut()).isEqualTo(StatutFournisseur.ACTIF);
  }

  @Test
  void should_refuser_decision_when_aucune_demande_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("motif");
    assertThatThrownBy(() -> f.deciderBlacklistage(DG, true, null))
        .isInstanceOf(ConflictException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.AUCUNE_DEMANDE_BLACKLISTAGE);
  }

  @Test
  void should_refuser_decision_par_le_demandeur_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("motif");
    f.demanderBlacklistage(RA, "motif", MAINTENANT);
    assertThatThrownBy(() -> f.deciderBlacklistage(RA, true, null))
        .isInstanceOf(BusinessRuleException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.DECISION_PAR_DEMANDEUR_INTERDITE);
  }

  @Test
  void should_refuser_reactivation_when_demande_en_attente_R09() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("motif");
    f.demanderBlacklistage(RA, "motif", MAINTENANT);
    assertThatThrownBy(f::reactiver)
        .isInstanceOf(ConflictException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.DEMANDE_BLACKLISTAGE_EN_COURS);
  }

  @Test
  void should_interdire_toute_modification_when_blackliste() {
    Fournisseur f = blackliste();
    assertThatThrownBy(() -> f.modifierProfil(Fixtures.profil("Y", "001234567 1A1"), null))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> f.ajouterDocument(TypeDocument.AUTRE, "doc", null))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> f.verifierCatalogueModifiable(CategorieMateriel.MATERIEL_ROULANT))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(f::reactiver).isInstanceOf(ConflictException.class);
  }

  @Test
  void should_conserver_coordonnees_when_modification_sans_coordonnees() {
    Fournisseur f = Fixtures.actif();
    f.modifierProfil(Fixtures.profil("Sahel Engins Nouvelle", "005123456 2G3"), null);
    assertThat(f.getProfil().raisonSociale()).isEqualTo("Sahel Engins Nouvelle");
    assertThat(f.getCoordonneesBancaires()).isPresent();
    f.modifierProfil(
        f.getProfil(), new CoordonneesBancaires("Autre banque", "SN99X0000000000000000000001"));
    assertThat(f.getCoordonneesBancaires())
        .get()
        .extracting(CoordonneesBancaires::banque)
        .isEqualTo("Autre banque");
  }

  @Test
  void should_lever_conflit_when_version_obsolete() {
    Fournisseur f = Fixtures.actif();
    f.verifierVersion(0);
    assertThatThrownBy(() -> f.verifierVersion(3))
        .isInstanceOf(ConflictException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.CONFLIT_CONCURRENCE);
  }

  @Test
  void should_compter_et_signaler_une_seule_fois_les_documents_expires() {
    Fournisseur f = Fixtures.actif();
    f.ajouterDocument(TypeDocument.NINEA, "NINEA", null);
    f.ajouterDocument(TypeDocument.QUITUS_FISCAL, "Quitus", AUJOURDHUI.minusDays(1));
    f.ajouterDocument(TypeDocument.ASSURANCE, "Assurance", AUJOURDHUI);
    assertThat(f.nombreDocumentsExpires(AUJOURDHUI)).isEqualTo(1);
    assertThat(f.signalerDocumentsExpires(AUJOURDHUI))
        .hasSize(1)
        .first()
        .extracting(DocumentAdministratif::getLibelle)
        .isEqualTo("Quitus");
    assertThat(f.signalerDocumentsExpires(AUJOURDHUI)).isEmpty();
    assertThat(f.nombreDocumentsExpires(AUJOURDHUI.plusDays(1))).isEqualTo(2);
  }

  @Test
  void should_suivre_la_ponctualite_des_livraisons_R07() {
    Fournisseur f = Fixtures.actif();
    assertThat(f.enregistrerLivraison(null, AUJOURDHUI)).isFalse();
    assertThat(f.enregistrerLivraison(AUJOURDHUI, AUJOURDHUI)).isTrue();
    assertThat(f.enregistrerLivraison(AUJOURDHUI, AUJOURDHUI.minusDays(2))).isTrue();
    assertThat(f.enregistrerLivraison(AUJOURDHUI, AUJOURDHUI.plusDays(1))).isTrue();
    assertThat(f.getPerformance().livraisonsTotal()).isEqualTo(3);
    assertThat(f.getPerformance().tauxLivraisonALHeure()).get().isEqualTo(new BigDecimal("66.7"));
  }

  @Test
  void should_indiquer_changement_when_score_actualise_R07() {
    Fournisseur f = Fixtures.actif();
    assertThat(f.actualiserScore(new BigDecimal("50.0"), 0)).isFalse();
    assertThat(f.actualiserScore(new BigDecimal("72.5"), 2)).isTrue();
    assertThat(f.getPerformance().scorePerformance()).isEqualByComparingTo("72.5");
    assertThat(f.getPerformance().nombreEvaluations()).isEqualTo(2);
  }

  @Test
  void should_refuser_evaluation_when_en_attente_validation() {
    assertThatThrownBy(() -> Fixtures.enAttente().verifierEvaluable())
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_refuser_catalogue_hors_specialites() {
    Fournisseur f = Fixtures.actif();
    f.verifierCatalogueModifiable(CategorieMateriel.MATERIEL_ROULANT);
    assertThatThrownBy(() -> f.verifierCatalogueModifiable(CategorieMateriel.CONSOMMABLE))
        .isInstanceOf(BusinessRuleException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.CATEGORIE_HORS_SPECIALITES);
  }

  private static Fournisseur blackliste() {
    Fournisseur f = Fixtures.actif();
    f.suspendre("motif");
    f.demanderBlacklistage(RA, "motif", MAINTENANT);
    f.deciderBlacklistage(DG, true, null);
    return f;
  }
}
