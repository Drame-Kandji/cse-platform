package sn.cse.besoins.domain.model;

import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BesoinMaterielRulesTest {

    @Test
    void should_exiger_justification_when_priorite_urgent_ou_critique_R08() {
        assertThatThrownBy(() -> new BesoinMateriel(
                UUID.randomUUID(), "Pelle Caterpillar 320D", 1, Priorite.URGENT,
                UUID.randomUUID(), UUID.randomUUID(), ""
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("R08");
    }

    @Test
    void should_valider_besoin_en_attente_et_bloquer_modification_R01_R05() {
        BesoinMateriel besoin = new BesoinMateriel(
                UUID.randomUUID(), "Pelle Caterpillar 320D", 1, Priorite.NORMAL,
                UUID.randomUUID(), UUID.randomUUID(), null
        );
        assertThat(besoin.estModifiable()).isTrue();
        besoin.valider();
        assertThat(besoin.getStatut()).isEqualTo(StatutBesoin.VALIDE);
        assertThat(besoin.estModifiable()).isFalse();
    }

    @Test
    void should_exiger_motif_pour_rejeter_besoin_R08() {
        BesoinMateriel besoin = new BesoinMateriel(
                UUID.randomUUID(), "Bulldozer D65", 1, Priorite.NORMAL,
                UUID.randomUUID(), UUID.randomUUID(), null
        );
        assertThatThrownBy(() -> besoin.rejeter(" "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("R08");

        besoin.rejeter("Budget insuffisant");
        assertThat(besoin.getStatut()).isEqualTo(StatutBesoin.REJETE);
        assertThat(besoin.getMotifRejet()).isEqualTo("Budget insuffisant");
    }

    @Test
    void should_transitionner_vers_affecte_direct_ou_en_achat_apres_validation() {
        BesoinMateriel b1 = new BesoinMateriel(
                UUID.randomUUID(), "Grue 50T", 1, Priorite.NORMAL,
                UUID.randomUUID(), UUID.randomUUID(), null
        );
        b1.valider();
        b1.marquerAffecteDirect();
        assertThat(b1.getStatut()).isEqualTo(StatutBesoin.AFFECTE_DIRECT);

        BesoinMateriel b2 = new BesoinMateriel(
                UUID.randomUUID(), "Niveleuse 140H", 1, Priorite.NORMAL,
                UUID.randomUUID(), UUID.randomUUID(), null
        );
        b2.valider();
        b2.marquerEnAchat();
        assertThat(b2.getStatut()).isEqualTo(StatutBesoin.EN_ACHAT);
    }
}

