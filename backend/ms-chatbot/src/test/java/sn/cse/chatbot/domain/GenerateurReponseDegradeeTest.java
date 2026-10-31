package sn.cse.chatbot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import sn.cse.chatbot.domain.model.AnalyseQuestion;
import sn.cse.chatbot.domain.model.ContexteDonnees;
import sn.cse.chatbot.domain.model.Procedure;
import sn.cse.chatbot.domain.model.Section;
import sn.cse.chatbot.domain.service.ClassificateurIntention;
import sn.cse.chatbot.domain.service.GenerateurReponseDegradee;

class GenerateurReponseDegradeeTest {

  private final ClassificateurIntention classificateur = new ClassificateurIntention();
  private final GenerateurReponseDegradee generateur = new GenerateurReponseDegradee(10);

  private String repondre(String question, ContexteDonnees c) {
    AnalyseQuestion a = classificateur.analyser(question);
    return generateur.generer(a, c, List.of("Où se trouve la pelle hydraulique ?"));
  }

  @Test
  void should_list_engins_with_state_chantier_and_gps_when_localising() {
    String r =
        repondre(
            "Où sont les niveleuses ?",
            ContexteDonnees.vide().avecMateriels(List.of(Donnees.NIVELEUSE, Donnees.PELLE)));
    assertThat(r)
        .startsWith("**Localisation** — 2 matériels trouvés :")
        .contains(
            "- **NIV-002** — Niveleuse CAT 140K : sur le chantier **Contournement de Thiès RN2** (affecté) — GPS 14.791, -16.9256")
        .contains("- **PEL-001** — Pelle hydraulique 20 t : au dépôt (non affecté) (disponible)");
  }

  @Test
  void should_say_nothing_found_or_unavailable() {
    assertThat(repondre("Où est la grue ?", ContexteDonnees.vide()))
        .isEqualTo(
            "Je n'ai trouvé aucun matériel correspondant à « grue » parmi les données auxquelles vous avez accès.");
    assertThat(
            repondre(
                "Où est la grue ?",
                ContexteDonnees.builder().indisponible(Section.MATERIELS).build()))
        .contains("momentanément indisponible");
    assertThat(
            repondre(
                "Où en est le contrat CTR-2026-003 ?",
                ContexteDonnees.builder().masquee(Section.CONTRATS).build()))
        .isEqualTo("Ces informations ne sont pas accessibles à votre profil.");
  }

  @Test
  void should_compare_requested_quantity_with_available_fleet() {
    ContexteDonnees c =
        ContexteDonnees.vide().avecMateriels(List.of(Donnees.PELLE, Donnees.NIVELEUSE));
    String r = repondre("Avons-nous 3 pelles disponibles ?", c);
    assertThat(r)
        .contains("**Disponibilité — pelle** — 1 matériel disponible :")
        .contains("PEL-001")
        .contains("maintenance préventive due")
        .doesNotContain("NIV-002")
        .contains("Seulement 1 disponible(s) pour 3 demandé(s)");
    assertThat(repondre("1 pelle disponible ?", c))
        .contains("La quantité demandée (1) est couverte par le parc.");
    assertThat(repondre("Des pelles disponibles ?", ContexteDonnees.vide()))
        .contains("aucun matériel disponible correspondant à « pelle »");
  }

  @Test
  void should_explain_besoin_status_with_motif_and_validator() {
    String r =
        repondre(
            "Où en sont mes besoins ?",
            ContexteDonnees.vide()
                .avecBesoins(List.of(Donnees.BESOIN_REJETE, Donnees.BESOIN_ATTENTE)));
    assertThat(r)
        .contains(
            "**BES-2026-0012** — 1 × Compacteur (chantier Pont de la lagune) : rejeté — motif : Doublon avec BES-2026-0010 par Ibrahima Fall")
        .contains(
            "**BES-2026-0013** — 2 × Bétonnière (chantier Thiès) : en attente de validation par le Responsable Matériel — priorité normale, besoin au 09/11/2026");
    assertThat(repondre("Statut de BES-2026-0099", ContexteDonnees.vide()))
        .contains("aucun besoin correspondant à « BES-2026-0099 »");
  }

  @Test
  void should_describe_contracts_and_tenders() {
    String r =
        repondre(
            "Où en est la livraison ?",
            ContexteDonnees.vide()
                .avecContrats(List.of(Donnees.CONTRAT))
                .avecAppelsOffres(List.of(Donnees.AO)));
    assertThat(r)
        .contains(
            "**CTR-2026-003** — Fourniture de 2 pelles (Sahel Engins SA) : en cours de livraison, livraison prévue le 15/11/2026 — 180 000 000 FCFA")
        .contains(
            "**AO-2026-001** — Acquisition de compacteurs : publié (offres en cours de réception), 3 offre(s), date limite le 20/11/2026 à 12:00 — attribué pour 90 000 000 FCFA");
    String sansMontant =
        repondre(
            "Statut du contrat CTR-2026-003",
            ContexteDonnees.vide().avecContrats(List.of(Donnees.CONTRAT)).sansMontants());
    assertThat(sansMontant).doesNotContain("FCFA");
  }

  @Test
  void should_render_maintenance_history_and_planning() {
    assertThat(
            repondre(
                "Historique de maintenance de NIV-002",
                ContexteDonnees.vide().avecMaintenances(List.of(Donnees.VIDANGE))))
        .contains(
            "**OT-2026-0007** — NIV-002 Niveleuse CAT 140K : maintenance préventive, clôturée, planifiée le 01/09/2026, terminée le 02/09/2026 — Vidange 500 h — coût 450 000 FCFA");
    assertThat(
            repondre(
                "Planning du chantier de Thiès",
                ContexteDonnees.vide().avecAffectations(List.of(Donnees.AFFECTATION))))
        .contains(
            "**NIV-002** — Niveleuse CAT 140K → Contournement de Thiès RN2 du 01/10/2026 au 31/12/2026 (en cours)");
    assertThat(repondre("Planning du chantier de Kaolack", ContexteDonnees.vide()))
        .contains("Je n'ai trouvé aucune affectation correspondant à « Kaolack »");
  }

  @Test
  void should_rank_suppliers() {
    assertThat(
            repondre(
                "Fournisseurs de matériel roulant",
                ContexteDonnees.vide().avecFournisseurs(List.of(Donnees.SAHEL))))
        .contains("**Fournisseurs recommandés — matériel roulant** — 1 fournisseur :")
        .contains(
            "1. **Sahel Engins SA** — score 87,5/100, livraisons à l'heure 92 % (matériel roulant)");
  }

  @Test
  void should_number_procedure_steps_and_handle_unknown_topics() {
    ContexteDonnees c =
        ContexteDonnees.vide()
            .avecProcedures(
                List.of(
                    new Procedure(
                        "Signaler une panne",
                        List.of("panne"),
                        List.of("Ouvrir la fiche", "Décrire la panne"))));
    assertThat(repondre("Comment signaler une panne ?", c))
        .isEqualTo("**Signaler une panne**\n1. Ouvrir la fiche\n2. Décrire la panne");
    assertThat(repondre("Comment faire ?", ContexteDonnees.vide()))
        .contains("Je n'ai pas trouvé de procédure");
    assertThat(repondre("Quelle est la capitale du Japon ?", ContexteDonnees.vide()))
        .contains("Je ne réponds pas aux questions hors de ce périmètre")
        .contains("- Où se trouve la pelle hydraulique ?");
  }

  @Test
  void should_add_warnings_for_partial_data_and_truncate_long_lists() {
    ContexteDonnees c =
        ContexteDonnees.builder()
            .avec(x -> x.avecMateriels(java.util.Collections.nCopies(12, Donnees.PELLE)))
            .indisponible(Section.AFFECTATIONS)
            .masquee(Section.CONTRATS)
            .build();
    String r = repondre("Où sont les pelles ?", c);
    assertThat(r)
        .contains("_… et 2 autre(s)._")
        .contains("la réponse peut être incomplète")
        .contains("pas accessibles à votre profil");
    assertThat(c.indisponibles()).isEqualTo(Set.of(Section.AFFECTATIONS));
  }
}
