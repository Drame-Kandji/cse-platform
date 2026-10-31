package sn.cse.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import sn.cse.notification.domain.model.ConditionRegle;
import sn.cse.notification.domain.model.EvenementMetier;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.RegleRoutage;

/** Données de test partagées. */
public final class Fixtures {

  public static final Instant T0 = Instant.parse("2026-10-30T09:15:00Z");

  private Fixtures() {}

  public static EvenementMetier evenement(String type, Map<String, Object> donnees) {
    return new EvenementMetier(UUID.randomUUID(), type, "agg-1", T0, "Ibrahima Fall", donnees);
  }

  public static RegleRoutage regleRole(String evt, String role, String titre) {
    return new RegleRoutage(
        evt,
        Set.of(role),
        List.of(),
        false,
        "chantierId",
        NiveauNotification.INFO,
        titre,
        "Message {reference}",
        "/besoins/{besoinId}",
        "BESOIN",
        "besoinId",
        false,
        null);
  }

  public static RegleRoutage regleUtilisateur(String evt, String champ) {
    return new RegleRoutage(
        evt,
        Set.of(),
        List.of(champ),
        false,
        "chantierId",
        NiveauNotification.SUCCES,
        "Besoin {reference} validé",
        "Validé par {acteurNom}",
        "/besoins/{besoinId}",
        "BESOIN",
        "besoinId",
        true,
        null);
  }

  public static RegleRoutage regleChefs(String evt) {
    return new RegleRoutage(
        evt,
        Set.of(),
        List.of("chefChantierId"),
        true,
        "chantierId",
        NiveauNotification.INFO,
        "Matériel {materielCode} affecté",
        "Au chantier {chantierNom}",
        "/materiels/{materielId}",
        "MATERIEL",
        "materielId",
        true,
        null);
  }

  public static RegleRoutage regleConditionnelle(
      String evt, String role, ConditionRegle condition, NiveauNotification niveau) {
    return new RegleRoutage(
        evt,
        Set.of(role),
        List.of(),
        false,
        "chantierId",
        niveau,
        "Titre",
        "Message",
        null,
        null,
        null,
        false,
        condition);
  }
}
