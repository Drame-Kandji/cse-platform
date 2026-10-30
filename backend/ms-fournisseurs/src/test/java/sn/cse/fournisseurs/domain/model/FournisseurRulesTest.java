package sn.cse.fournisseurs.domain.model;

import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FournisseurRulesTest {

    @Test
    void should_recalculer_score_fournisseur_R07() {
        Fournisseur f = new Fournisseur(
                UUID.randomUUID(), "Sénégal Équipements BTP", "SN-001", "RC-001",
                "contact@btp.sn", "+221338000000", "ENGINS_LOURDS", "ACTIF",
                80.0, false, null, Instant.now()
        );
        double score = f.recalculerScore(90.0, 90.0, 90.0, 90.0);
        assertThat(score).isEqualTo(85.0);
    }

    @Test
    void should_exiger_demande_avant_blacklistage_dg_R09() {
        Fournisseur f = new Fournisseur(
                UUID.randomUUID(), "Fournisseur Test", "SN-002", "RC-002",
                "test@btp.sn", "+221338000001", "OUTILLAGE", "SUSPENDU",
                50.0, false, null, Instant.now()
        );
        assertThatThrownBy(f::blacklisterParDg)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("R09");

        f.demanderBlacklistage("Livraisons non conformes répétées");
        f.blacklisterParDg();
        assertThat(f.getStatut()).isEqualTo("BLACKLISTE");
    }
}

