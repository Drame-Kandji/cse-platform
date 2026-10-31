package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.EtatMateriel;

/**
 * Vue d'un matériel du parc : état courant, chantier courant, tarif journalier et alerte de
 * maintenance préventive en attente.
 *
 * @param id identifiant
 * @param code code
 * @param designation désignation
 * @param categorie catégorie (peut être inconnue pour une référence)
 * @param etat état courant (peut être inconnu pour une référence)
 * @param chantierId chantier courant
 * @param tarifJournalier tarif journalier interne (FCFA)
 * @param valeurAcquisition valeur d'acquisition (FCFA)
 * @param alerteMaintenanceLe instant de la dernière alerte de maintenance non suivie d'effet
 * @param etatMajLe instant du dernier événement ayant fixé l'état
 */
public record MaterielVue(
    UUID id,
    String code,
    String designation,
    CategorieMateriel categorie,
    EtatMateriel etat,
    UUID chantierId,
    BigDecimal tarifJournalier,
    BigDecimal valeurAcquisition,
    Instant alerteMaintenanceLe,
    Instant etatMajLe) {

  public MaterielVue {
    Objects.requireNonNull(id, "id");
    tarifJournalier = tarifJournalier == null ? BigDecimal.ZERO : tarifJournalier;
  }

  /**
   * Référence créée quand un événement d'affectation ou de maintenance précède l'enregistrement du
   * matériel.
   *
   * @param id identifiant
   * @param code code connu
   * @param designation désignation connue
   * @param categorie catégorie connue
   * @param tarifJournalier tarif connu
   * @return la référence (état inconnu)
   */
  public static MaterielVue reference(
      UUID id,
      String code,
      String designation,
      CategorieMateriel categorie,
      BigDecimal tarifJournalier) {
    return new MaterielVue(
        id, code, designation, categorie, null, null, tarifJournalier, null, null, null);
  }

  /**
   * Applique un instantané {@code MaterielPayload} : l'état n'est remplacé que si l'instantané est
   * plus récent ; les attributs descriptifs inconnus sont toujours complétés.
   *
   * @param recu instantané reçu
   * @return la vue fusionnée
   */
  public MaterielVue fusionnerAvec(MaterielVue recu) {
    Instant alerte = Horodatage.max(alerteMaintenanceLe, recu.alerteMaintenanceLe);
    if (etat != EtatMateriel.REFORME && Horodatage.plusRecent(recu.etatMajLe, etatMajLe)) {
      return new MaterielVue(
          id,
          Horodatage.sinon(recu.code, code),
          Horodatage.sinon(recu.designation, designation),
          Horodatage.sinon(recu.categorie, categorie),
          Horodatage.sinon(recu.etat, etat),
          recu.chantierId,
          recu.tarifJournalier.signum() > 0 ? recu.tarifJournalier : tarifJournalier,
          Horodatage.sinon(recu.valeurAcquisition, valeurAcquisition),
          alerte,
          recu.etatMajLe);
    }
    return new MaterielVue(
        id,
        Horodatage.sinon(code, recu.code),
        Horodatage.sinon(designation, recu.designation),
        Horodatage.sinon(categorie, recu.categorie),
        etat,
        chantierId,
        tarifJournalier.signum() > 0 ? tarifJournalier : recu.tarifJournalier,
        Horodatage.sinon(valeurAcquisition, recu.valeurAcquisition),
        alerte,
        etatMajLe);
  }

  /**
   * Change l'état déduit d'un événement d'affectation ou de maintenance. Sans effet si l'état connu
   * est plus récent ou si le matériel est réformé (état terminal).
   *
   * @param nouvelEtat état déduit
   * @param nouveauChantier chantier courant déduit
   * @param quand instant de l'événement
   * @return la vue mise à jour
   */
  public MaterielVue avecEtat(EtatMateriel nouvelEtat, UUID nouveauChantier, Instant quand) {
    if (etat == EtatMateriel.REFORME || !Horodatage.plusRecent(quand, etatMajLe)) {
      return this;
    }
    return new MaterielVue(
        id,
        code,
        designation,
        categorie,
        nouvelEtat,
        nouveauChantier,
        tarifJournalier,
        valeurAcquisition,
        alerteMaintenanceLe,
        quand);
  }

  /**
   * @param quand instant d'une alerte {@code ALERTE_MAINTENANCE}
   * @return la vue avec l'alerte en attente
   */
  public MaterielVue avecAlerte(Instant quand) {
    return new MaterielVue(
        id,
        code,
        designation,
        categorie,
        etat,
        chantierId,
        tarifJournalier,
        valeurAcquisition,
        Horodatage.max(alerteMaintenanceLe, quand),
        etatMajLe);
  }

  /**
   * Une maintenance démarrée ou clôturée après l'alerte la traite.
   *
   * @param quand instant de la maintenance
   * @return la vue sans alerte si celle-ci est antérieure
   */
  public MaterielVue alerteTraiteeLe(Instant quand) {
    if (alerteMaintenanceLe == null || alerteMaintenanceLe.isAfter(quand)) {
      return this;
    }
    return new MaterielVue(
        id,
        code,
        designation,
        categorie,
        etat,
        chantierId,
        tarifJournalier,
        valeurAcquisition,
        null,
        etatMajLe);
  }

  /**
   * @return vrai si une alerte de maintenance attend d'être traitée (hors matériel réformé)
   */
  public boolean alerteEnAttente() {
    return alerteMaintenanceLe != null && etat != EtatMateriel.REFORME;
  }

  /**
   * @return vrai si le matériel est réformé
   */
  public boolean reforme() {
    return etat == EtatMateriel.REFORME;
  }
}
