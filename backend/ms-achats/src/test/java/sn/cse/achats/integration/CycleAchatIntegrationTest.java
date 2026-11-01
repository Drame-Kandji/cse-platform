package sn.cse.achats.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import sn.cse.achats.application.port.in.CloturerAppelOffreUseCase;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.infrastructure.messaging.BesoinsEventsListener;
import sn.cse.achats.infrastructure.messaging.FournisseursEventsListener;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.events.Topics;

/**
 * Cycle d'achat complet par l'API : besoin reçu → AO → offres (R10, R14, R02) → clôture automatique
 * → évaluation → attribution RA/RM/DG (R11) → contrat (R03) → signature du CSE → refus puis
 * signature du fournisseur (portail) → engagement DAF (R06) → expédition déclarée par le
 * fournisseur → jalon → livraison → facture déposée sur le portail, avec vérification de l'outbox
 * et du chiffrement au repos.
 */
class CycleAchatIntegrationTest extends IntegrationTestBase {

  private static final UUID F1 = UUID.fromString("f0000000-0000-0000-0000-000000000001");
  private static final UUID F2 = UUID.fromString("f0000000-0000-0000-0000-000000000002");
  private static final UUID F3 = UUID.fromString("f0000000-0000-0000-0000-000000000003");
  private static final UUID F6 = UUID.fromString("f0000000-0000-0000-0000-000000000006");
  private static final UUID RA = UUID.fromString("00000000-0000-0000-0000-000000000301");
  private static final UUID RM = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final UUID DAF = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final UUID DG = UUID.fromString("00000000-0000-0000-0000-000000000501");

  @Autowired BesoinsEventsListener besoins;
  @Autowired FournisseursEventsListener fournisseurs;
  @Autowired CloturerAppelOffreUseCase cloture;
  @Autowired ContratRepository contrats;

  private static RequestPostProcessor role(UUID id, String role) {
    return CseJwt.as(id, role);
  }

  private static MockHttpServletRequestBuilder corps(
      MockHttpServletRequestBuilder requete, String json) {
    return requete.contentType(MediaType.APPLICATION_JSON).content(json.replace('\'', '"'));
  }

  private static String enveloppe(
      UUID eventId, String type, String source, String agregat, String payload) {
    return ("{'eventId':'"
            + eventId
            + "','type':'"
            + type
            + "','version':1,'occurredAt':'2026-10-01T07:00:00Z',"
            + "'source':'"
            + source
            + "','aggregateType':'"
            + agregat
            + "','aggregateId':'x','payload':"
            + payload
            + "}")
        .replace('\'', '"');
  }

  private String nouvelAo(UUID besoinId, List<UUID> invites) {
    StringBuilder ids = new StringBuilder();
    invites.forEach(i -> ids.append(ids.isEmpty() ? "" : ",").append("'").append(i).append("'"));
    return "{"
        + (besoinId == null ? "" : "'besoinId':'" + besoinId + "',")
        + "'objet':'Acquisition de deux pelles hydrauliques 20 t','type':'APPEL_OFFRES','categorie':'MATERIEL_ROULANT',"
        + "'quantite':2,'budgetEstime':320000000,'dateLimite':'2026-10-11T17:00:00Z',"
        + "'criteres':[{'code':'PRIX','poids':40},{'code':'DELAI','poids':20},{'code':'QUALITE_TECHNIQUE','poids':40}],"
        + "'fournisseurIdsInvites':["
        + ids
        + "]}";
  }

  @Test
  void should_derouler_le_cycle_d_achat_complet() throws Exception {
    horloge.regler(Instant.parse("2026-10-01T08:00:00Z"));
    fournisseurDistant(F1, "Sahel Engins SA", "ACTIF");
    fournisseurDistant(F2, "Teranga Équipements BTP", "ACTIF");
    fournisseurIndisponible(F6);
    UUID besoinId = UUID.randomUUID();

    // Projections alimentées par Kafka (consommateurs idempotents).
    String besoin =
        enveloppe(
            UUID.randomUUID(),
            "BESOIN_EN_ACHAT",
            "ms-besoins",
            "BesoinMateriel",
            "{'besoinId':'"
                + besoinId
                + "','reference':'BES-2026-0101','chantierId':'c0000000-0000-0000-0000-000000000001',"
                + "'chantierNom':'Extension autoroute Dakar-AIBD','designation':'Pelle hydraulique 20 t',"
                + "'categorie':'MATERIEL_ROULANT','quantite':2,'priorite':'URGENT','statut':'EN_ACHAT',"
                + "'dateBesoin':'2026-11-15','dureeUtilisationJours':90,'materielIds':[]}");
    besoins.recevoir(besoin);
    besoins.recevoir(besoin);
    fournisseurs.recevoir(
        enveloppe(
            UUID.randomUUID(),
            "FOURNISSEUR_SUSPENDU",
            "ms-fournisseurs",
            "Fournisseur",
            "{'fournisseurId':'"
                + F6
                + "','raisonSociale':'Ndiambour Matériaux','statut':'SUSPENDU','specialites':[]}"));
    fournisseurs.recevoir(
        enveloppe(
            UUID.randomUUID(),
            "FOURNISSEUR_VALIDE",
            "ms-fournisseurs",
            "Fournisseur",
            "{'fournisseurId':'"
                + F3
                + "','raisonSociale':'Dakar Outillage Pro','statut':'ACTIF','specialites':[]}"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM besoin_a_acheter WHERE besoin_id = ?",
                Integer.class,
                besoinId))
        .isEqualTo(1);
    mvc.perform(get("/api/besoins-a-acheter").with(role(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.besoinId=='" + besoinId + "')]").exists());

    // R14 : ms-fournisseurs indisponible → repli sur la projection (SUSPENDU) → refus.
    mvc.perform(
            corps(post("/api/appels-offres"), nouvelAo(null, List.of(F6)))
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("FOURNISSEUR_NON_ELIGIBLE"));

    JsonNode ao =
        json(
            mvc.perform(
                    corps(post("/api/appels-offres"), nouvelAo(besoinId, List.of(F1, F2)))
                        .with(role(RA, Roles.RESPONSABLE_ACHATS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("PUBLIE"))
                .andExpect(jsonPath("$.besoinReference").value("BES-2026-0101"))
                .andExpect(
                    jsonPath("$.fournisseursInvites[1].raisonSociale")
                        .value("Teranga Équipements BTP"))
                .andExpect(jsonPath("$.creeParNom").value("Utilisateur Test")));
    String aoId = ao.get("id").asText();
    assertThat(ao.get("reference").asText()).matches("AO-2026-\\d{3}");
    assertThat(ao.get("budgetEstime").asLong()).isEqualTo(320_000_000L);

    // Offres : saisie RA et dépôt portail, anonymisées tant que PUBLIE (R10).
    mvc.perform(
            corps(
                    post("/api/appels-offres/{id}/offres", aoId),
                    "{'fournisseurId':'"
                        + F1
                        + "','montantPropose':298000000,'delaiLivraisonJours':60,'garantieMois':24}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fournisseurId").doesNotExist());
    mvc.perform(
            corps(
                    post("/api/appels-offres/{id}/offres", aoId),
                    "{'fournisseurId':'"
                        + F2
                        + "','montantPropose':312000000,'delaiLivraisonJours':45}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isCreated());
    RequestPostProcessor portailF3 =
        CseJwt.avecClaims(
            UUID.randomUUID(), Map.of("fournisseur_id", F3.toString()), Roles.FOURNISSEUR);
    mvc.perform(get("/api/portail/appels-offres").with(portailF3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id=='" + aoId + "')].offreDeposee").value(false));
    mvc.perform(
            corps(
                    post("/api/portail/appels-offres/{id}/offres", aoId),
                    "{'montantPropose':305000000,'delaiLivraisonJours':50,'description':'Deux Hyundai HX220'}")
                .with(portailF3))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.appelOffreReference").value(ao.get("reference").asText()));
    mvc.perform(get("/api/portail/offres/mes-offres").with(portailF3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    JsonNode anonymes =
        json(
            mvc.perform(
                    get("/api/appels-offres/{id}/offres", aoId)
                        .with(role(RM, Roles.RESPONSABLE_MATERIEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3)));
    anonymes.forEach(
        o -> {
          assertThat(o.get("fournisseurId").isNull()).isTrue();
          assertThat(o.get("fournisseurNom").isNull()).isTrue();
          assertThat(o.get("codeAnonyme").asText()).startsWith("OFR-");
        });

    // R02 : après la date limite, plus aucune offre ; la clôture automatique lève l'anonymat.
    horloge.regler(Instant.parse("2026-10-12T06:00:00Z"));
    mvc.perform(
            corps(
                    post("/api/portail/appels-offres/{id}/offres", aoId),
                    "{'montantPropose':1,'delaiLivraisonJours':1}")
                .with(
                    CseJwt.avecClaims(
                        UUID.randomUUID(),
                        Map.of("fournisseur_id", F1.toString()),
                        Roles.FOURNISSEUR)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("DATE_LIMITE_DEPASSEE"));
    mvc.perform(
            corps(
                    post("/api/appels-offres/{id}/offres", aoId),
                    "{'fournisseurId':'" + F1 + "','montantPropose':1,'delaiLivraisonJours':1}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("DATE_LIMITE_DEPASSEE"));
    assertThat(cloture.cloturerEchus()).isGreaterThanOrEqualTo(1);
    JsonNode offres =
        json(
            mvc.perform(
                get("/api/appels-offres/{id}/offres", aoId)
                    .with(role(RM, Roles.RESPONSABLE_MATERIEL))));
    Map<String, String> offreParFournisseur = new java.util.HashMap<>();
    offres.forEach(
        o -> offreParFournisseur.put(o.get("fournisseurId").asText(), o.get("id").asText()));
    assertThat(offreParFournisseur).containsKeys(F1.toString(), F2.toString(), F3.toString());
    mvc.perform(
            get("/api/appels-offres/{id}/proces-verbaux/OUVERTURE", aoId)
                .with(role(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));

    // Évaluation (PRIX et DELAI calculés) et comparatif.
    int[] notes = {90, 70, 60};
    UUID[] ordre = {F1, F2, F3};
    for (int i = 0; i < 3; i++) {
      mvc.perform(
              corps(
                      put(
                          "/api/appels-offres/{id}/offres/{o}/evaluation",
                          aoId,
                          offreParFournisseur.get(ordre[i].toString())),
                      "{'QUALITE_TECHNIQUE':" + notes[i] + "}")
                  .with(role(RM, Roles.RESPONSABLE_MATERIEL)))
          .andExpect(status().isOk());
    }
    JsonNode comparatif =
        json(
            mvc.perform(
                    get("/api/appels-offres/{id}/comparatif", aoId)
                        .with(role(RA, Roles.RESPONSABLE_ACHATS)))
                .andExpect(jsonPath("$.offres[0].rang").value(1))
                .andExpect(jsonPath("$.offres[0].scoreGlobal").value(91.0))
                .andExpect(jsonPath("$.offres[0].notes.DELAI").value(75.0)));
    String retenue = comparatif.get("recommandationOffreId").asText();
    assertThat(retenue).isEqualTo(offreParFournisseur.get(F1.toString()));

    // R11 : 298 M FCFA > seuil de délégation → RM puis DG.
    mvc.perform(
            corps(
                    post("/api/appels-offres/{id}/attribuer", aoId),
                    "{'offreId':'" + retenue + "','justification':'Meilleur score global'}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attribution.approbationDg.statut").value("EN_ATTENTE"))
        .andExpect(jsonPath("$.attribution.fournisseurNom").value("Sahel Engins SA"));
    mvc.perform(
            corps(post("/api/appels-offres/{id}/attribution/co-valider", aoId), "{'approuve':true}")
                .with(role(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("CLOTURE"));
    mvc.perform(
            corps(
                    post("/api/appels-offres/{id}/attribution/approuver", aoId),
                    "{'approuve':true,'commentaire':'Comité du 12/10'}")
                .with(role(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("ATTRIBUE"))
        .andExpect(jsonPath("$.montantAttribue").value(298000000));
    mvc.perform(
            get("/api/appels-offres/{id}/proces-verbaux/EVALUATION", aoId)
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk());

    // R03 : contrat BROUILLON généré à l'attribution, unique.
    String contratId =
        contrats.parAppelOffre(UUID.fromString(aoId)).orElseThrow().getId().toString();
    mvc.perform(get("/api/appels-offres/{id}", aoId).with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(jsonPath("$.contratId").value(contratId));
    mvc.perform(
            get("/api/appels-offres?statut=ATTRIBUE&q=pelle")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(jsonPath("$.content[?(@.id=='" + aoId + "')].contratId").value(contratId));
    mvc.perform(get("/api/contrats?appelOffreId={ao}", aoId).with(role(DAF, Roles.DAF)))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(contratId));
    mvc.perform(
            corps(post("/api/contrats"), "{'appelOffreId':'" + aoId + "'}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONTRAT_EXISTANT"));
    mvc.perform(
            corps(
                    put("/api/contrats/{id}/clauses", contratId),
                    "{'garantieMois':24,'conditionsLivraison':'Franco chantier Diamniadio','penaliteRetardPctParJour':0.1,"
                        + "'delaiLivraisonJours':60,'jalons':[{'libelle':'Expédition','datePrevue':'2026-11-30'}]}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("BROUILLON"))
        .andExpect(jsonPath("$.designation").value("Pelle hydraulique 20 t"));
    // Signature en deux temps (§13) : le CSE signe (clauses figées), le fournisseur signe ou
    // refuse.
    RequestPostProcessor portailF1 =
        CseJwt.avecClaims(
            UUID.randomUUID(), Map.of("fournisseur_id", F1.toString()), Roles.FOURNISSEUR);
    RequestPostProcessor portailF2 =
        CseJwt.avecClaims(
            UUID.randomUUID(), Map.of("fournisseur_id", F2.toString()), Roles.FOURNISSEUR);
    mvc.perform(
            post("/api/contrats/{id}/signer", contratId).with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE_SIGNATURE"))
        .andExpect(jsonPath("$.signatureCse.parNom").value("Utilisateur Test"))
        .andExpect(jsonPath("$.engagement").doesNotExist());
    mvc.perform(
            corps(
                    put("/api/contrats/{id}/clauses", contratId),
                    "{'delaiLivraisonJours':30,'jalons':[]}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONTRAT_NON_MODIFIABLE"));
    // Contrat d'un autre fournisseur : 404 (jamais 403) sur toutes les routes du portail.
    mvc.perform(get("/api/portail/contrats/{id}", contratId).with(portailF2))
        .andExpect(status().isNotFound());
    mvc.perform(post("/api/portail/contrats/{id}/signer", contratId).with(portailF2))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/portail/contrats").with(portailF2))
        .andExpect(jsonPath("$.content[?(@.id=='" + contratId + "')]").isEmpty());
    mvc.perform(get("/api/portail/contrats?statut=EN_ATTENTE_SIGNATURE").with(portailF1))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[?(@.id=='" + contratId + "')].statut")
                .value("EN_ATTENTE_SIGNATURE"));
    mvc.perform(corps(post("/api/portail/contrats/{id}/refuser", contratId), "{}").with(portailF1))
        .andExpect(status().isBadRequest());
    mvc.perform(
            corps(
                    post("/api/portail/contrats/{id}/refuser", contratId),
                    "{'motif':'Pénalité journalière trop élevée'}")
                .with(portailF1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("BROUILLON"))
        .andExpect(jsonPath("$.signatureCse").doesNotExist())
        .andExpect(jsonPath("$.motifRefusSignature").value("Pénalité journalière trop élevée"));
    mvc.perform(post("/api/portail/contrats/{id}/signer", contratId).with(portailF1))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TRANSITION_INTERDITE"));
    mvc.perform(
            post("/api/contrats/{id}/signer", contratId).with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE_SIGNATURE"))
        .andExpect(jsonPath("$.motifRefusSignature").value("Pénalité journalière trop élevée"));
    JsonNode signe =
        json(
            mvc.perform(post("/api/portail/contrats/{id}/signer", contratId).with(portailF1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SIGNE"))
                .andExpect(jsonPath("$.signatureCse.parNom").value("Utilisateur Test"))
                .andExpect(jsonPath("$.signatureFournisseur.parNom").isNotEmpty())
                .andExpect(jsonPath("$.motifRefusSignature").doesNotExist())
                .andExpect(jsonPath("$.engagement.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.dateLivraisonPrevue").value("2026-12-11")));
    mvc.perform(get("/api/engagements?statut=EN_ATTENTE").with(role(DAF, Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[?(@.contratId=='" + contratId + "')].montant").value(298000000));

    // R06 : ni expédition (portail) ni démarrage hors portail avant la validation DAF.
    mvc.perform(
            corps(
                    post("/api/portail/contrats/{id}/expedier", contratId),
                    "{'dateExpedition':'2026-10-12'}")
                .with(portailF1))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ENGAGEMENT_NON_VALIDE"));
    mvc.perform(
            post("/api/contrats/{id}/livraison/demarrer", contratId)
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ENGAGEMENT_NON_VALIDE"));
    mvc.perform(
            corps(post("/api/engagements/{id}/valider", contratId), "{'approuve':true}")
                .with(role(DAF, Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("ENGAGEMENT_VALIDE"));
    String factureFournisseur =
        "{'contratId':'"
            + contratId
            + "','numero':'FA-SE-2026-901','montantHt':120000000,'tva':21600000,"
            + "'dateEmission':'2026-12-05','dateEcheance':'2027-01-04'}";
    mvc.perform(corps(post("/api/portail/factures"), factureFournisseur).with(portailF1))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CONTRAT_NON_LIVRE"));
    mvc.perform(
            corps(
                    post("/api/portail/contrats/{id}/expedier", contratId),
                    "{'dateExpedition':'2026-10-12','commentaire':'Convoi exceptionnel depuis le port de Dakar'}")
                .with(portailF1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("EN_LIVRAISON"))
        .andExpect(jsonPath("$.dateExpedition").value("2026-10-12"))
        .andExpect(
            jsonPath("$.commentaireExpedition")
                .value("Convoi exceptionnel depuis le port de Dakar"));
    String jalonId = signe.get("jalons").get(0).get("id").asText();
    horloge.regler(Instant.parse("2026-12-05T09:00:00Z"));
    mvc.perform(
            corps(
                    patch("/api/portail/contrats/{id}/jalons/{j}", contratId, jalonId),
                    "{'dateReelle':'2026-12-01'}")
                .with(portailF2))
        .andExpect(status().isNotFound());
    mvc.perform(
            corps(
                    patch("/api/portail/contrats/{id}/jalons/{j}", contratId, jalonId),
                    "{'dateReelle':'2026-12-01'}")
                .with(portailF1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jalons[0].statut").value("ATTEINT"));

    // Livraison confirmée par le RA, puis facture déposée par le fournisseur sur le portail.
    mvc.perform(
            corps(
                    post("/api/contrats/{id}/livraison/confirmer", contratId),
                    "{'dateLivraisonReelle':'2026-12-04','numeroBonLivraison':'BL-SE-2026-0901'}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("LIVRE"))
        .andExpect(jsonPath("$.penalitesRetard").value(0));
    JsonNode facture =
        json(
            mvc.perform(corps(post("/api/portail/factures"), factureFournisseur).with(portailF1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montantTtc").value(141600000))
                .andExpect(jsonPath("$.enRetard").value(false)));
    mvc.perform(
            corps(
                    post("/api/factures/{id}/mandater", facture.get("id").asText()),
                    "{'referenceMandat':'MDT-2026-0901'}")
                .with(role(DAF, Roles.DAF)))
        .andExpect(jsonPath("$.statut").value("MANDATEE"));
    mvc.perform(
            post("/api/factures/{id}/payer", facture.get("id").asText()).with(role(DAF, Roles.DAF)))
        .andExpect(jsonPath("$.statut").value("PAYEE"));
    mvc.perform(get("/api/portail/factures").with(portailF1))
        .andExpect(
            jsonPath("$.content[?(@.id=='" + facture.get("id").asText() + "')].statut")
                .value("PAYEE"));
    mvc.perform(get("/api/finance/synthese").with(role(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.montantPaye").isNumber());
    mvc.perform(
            post("/api/contrats/{id}/cloturer", contratId).with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(jsonPath("$.statut").value("CLOTURE"));

    // Outbox : événements de domaine + audit, contenu de LIVRAISON_EFFECTUEE pour ms-suivi.
    List<String> types = new ArrayList<>();
    jdbc.query(
        "SELECT event_type FROM outbox_event WHERE topic = ? AND message_key IN (?, ?) ORDER BY created_at",
        rs -> {
          types.add(rs.getString(1));
        },
        Topics.ACHATS,
        aoId,
        contratId);
    // Plusieurs événements partagent le même instant (horloge figée) : présence vérifiée sans
    // ordre.
    assertThat(types)
        .contains(
            "AO_PUBLIE",
            "OFFRE_DEPOSEE",
            "AO_CLOTURE",
            "ATTRIBUTION_PROPOSEE",
            "MARCHE_ATTRIBUE",
            "CONTRAT_GENERE",
            "CONTRAT_ENVOYE_SIGNATURE",
            "CONTRAT_SIGNATURE_REFUSEE",
            "CONTRAT_SIGNE",
            "ENGAGEMENT_DEMANDE",
            "ENGAGEMENT_VALIDE",
            "LIVRAISON_DEMARREE",
            "LIVRAISON_EFFECTUEE",
            "CONTRAT_CLOTURE");
    JsonNode livraison =
        mapper.readTree(
            jdbc.queryForObject(
                "SELECT payload FROM outbox_event WHERE event_type = 'LIVRAISON_EFFECTUEE' AND message_key = ?",
                String.class,
                contratId));
    assertThat(livraison.at("/payload/besoinId").asText()).isEqualTo(besoinId.toString());
    // Événements du portail : instantané enrichi du motif de refus et de la date d'expédition.
    JsonNode refus =
        mapper.readTree(
            jdbc.queryForObject(
                "SELECT payload FROM outbox_event WHERE event_type = 'CONTRAT_SIGNATURE_REFUSEE'"
                    + " AND message_key = ?",
                String.class,
                contratId));
    assertThat(refus.at("/payload/motif").asText()).isEqualTo("Pénalité journalière trop élevée");
    assertThat(refus.at("/payload/statut").asText()).isEqualTo("BROUILLON");
    JsonNode demarrage =
        mapper.readTree(
            jdbc.queryForObject(
                "SELECT payload FROM outbox_event WHERE event_type = 'LIVRAISON_DEMARREE'"
                    + " AND message_key = ?",
                String.class,
                contratId));
    assertThat(demarrage.at("/payload/dateExpedition").asText()).isEqualTo("2026-10-12");
    assertThat(
            jdbc.queryForObject(
                "SELECT signature_cse_par_nom || '|' || signature_fournisseur_date::date"
                    + " FROM contrat_achat WHERE id = ?::uuid",
                String.class,
                contratId))
        .isEqualTo("Utilisateur Test|2026-10-12");
    assertThat(livraison.at("/payload/chantierId").asText())
        .isEqualTo("c0000000-0000-0000-0000-000000000001");
    assertThat(livraison.at("/payload/designation").asText()).isEqualTo("Pelle hydraulique 20 t");
    // FACTURE_PAYEE porte les dates métier (reporting date paiements et mandats sur celles-ci).
    JsonNode paiement =
        mapper.readTree(
            jdbc.queryForObject(
                "SELECT payload FROM outbox_event WHERE event_type = 'FACTURE_PAYEE' AND message_key = ?",
                String.class,
                facture.get("id").asText()));
    assertThat(paiement.at("/payload/dateEmission").asText())
        .isEqualTo(facture.get("dateEmission").asText());
    assertThat(paiement.at("/payload/dateMandatement").asText()).isNotBlank();
    assertThat(paiement.at("/payload/datePaiement").asText()).isNotBlank();
    assertThat(livraison.at("/payload/quantite").asInt()).isEqualTo(2);
    assertThat(livraison.at("/payload/tarifJournalierEstime").asLong()).isEqualTo(373_000L);
    assertThat(livraison.at("/payload/dateLivraisonReelle").asText()).isEqualTo("2026-12-04");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE topic = ? AND message_key = ?",
                Integer.class,
                Topics.AUDIT,
                contratId))
        .isGreaterThanOrEqualTo(6);

    // Chiffrement AES-GCM au repos : le montant n'apparaît pas en clair.
    String montantStocke =
        jdbc.queryForObject(
            "SELECT montant FROM contrat_achat WHERE id = ?::uuid", String.class, contratId);
    assertThat(montantStocke).doesNotContain("298000000").hasSizeGreaterThan(20);
  }

  @Test
  void should_supprimer_proposition_refusee_et_garder_ao_cloture_R11() throws Exception {
    horloge.regler(Instant.parse("2026-10-01T08:00:00Z"));
    fournisseurDistant(F1, "Sahel Engins SA", "ACTIF");
    String aoId =
        json(mvc.perform(
                    corps(post("/api/appels-offres"), nouvelAo(null, List.of(F1)))
                        .with(role(RA, Roles.RESPONSABLE_ACHATS)))
                .andExpect(status().isCreated()))
            .get("id")
            .asText();
    String offreId =
        json(mvc.perform(
                corps(
                        post("/api/appels-offres/{id}/offres", aoId),
                        "{'fournisseurId':'"
                            + F1
                            + "','montantPropose':90000000,'delaiLivraisonJours':30}")
                    .with(role(RA, Roles.RESPONSABLE_ACHATS))))
            .get("id")
            .asText();
    mvc.perform(
            post("/api/appels-offres/{id}/cloturer", aoId).with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(jsonPath("$.statut").value("CLOTURE"));
    mvc.perform(
            corps(
                    post("/api/appels-offres/{id}/attribuer", aoId),
                    "{'offreId':'" + offreId + "','justification':'Seule offre'}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(jsonPath("$.attribution.approbationDg").doesNotExist());
    mvc.perform(
            corps(post("/api/appels-offres/{id}/attribution/approuver", aoId), "{'approuve':true}")
                .with(role(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("APPROBATION_DG_NON_REQUISE"));

    mvc.perform(
            corps(
                    post("/api/appels-offres/{id}/attribution/co-valider", aoId),
                    "{'approuve':false,'commentaire':'Hors besoin'}")
                .with(role(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("CLOTURE"))
        .andExpect(jsonPath("$.attribution").doesNotExist());
    mvc.perform(
            corps(post("/api/contrats"), "{'appelOffreId':'" + aoId + "'}")
                .with(role(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("AO_NON_ATTRIBUE"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE event_type = 'ATTRIBUTION_REFUSEE' AND message_key = ?",
                Integer.class,
                aoId))
        .isEqualTo(1);
  }
}
