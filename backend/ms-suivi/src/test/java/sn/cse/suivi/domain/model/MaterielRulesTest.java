package sn.cse.suivi.domain.model;

import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaterielRulesTest {

    @Test
    void should_affecter_uniquement_si_disponible_R04() {
        Materiel m = new Materiel(UUID.randomUUID(), "CSE-PEL-001", "Pelle Caterpillar 320D", "PELLE");
        UUID chantierId = UUID.randomUUID();

        assertThat(m.estAffectable()).isTrue();
        m.affecter(chantierId);
        assertThat(m.getEtat()).isEqualTo(EtatMateriel.AFFECTE);
        assertThat(m.getChantierActuelId()).isEqualTo(chantierId);

        assertThatThrownBy(() -> m.affecter(UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_bloquer_changement_etat_apres_reforme_R13() {
        Materiel m = new Materiel(UUID.randomUUID(), "CSE-GRU-001", "Grue Liebherr", "GRUE");
        m.reformer();
        assertThat(m.getEtat()).isEqualTo(EtatMateriel.REFORME);

        assertThatThrownBy(() -> m.affecter(UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(m::liberer)
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(m::mettreEnMaintenance)
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_gerer_cycle_maintenance_R04() {
        Materiel m = new Materiel(UUID.randomUUID(), "CSE-BUL-001", "Bulldozer Komatsu", "BULLDOZER");
        m.mettreEnMaintenance();
        assertThat(m.getEtat()).isEqualTo(EtatMateriel.EN_MAINTENANCE);
        assertThat(m.estAffectable()).isFalse();

        m.sortirMaintenance();
        assertThat(m.getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
        assertThat(m.estAffectable()).isTrue();
    }
}

