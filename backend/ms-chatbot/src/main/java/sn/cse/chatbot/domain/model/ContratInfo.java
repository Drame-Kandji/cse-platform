package sn.cse.chatbot.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrat (lecture ms-achats).
 *
 * @param id identifiant
 * @param numero numéro
 * @param objet objet
 * @param fournisseurNom fournisseur
 * @param statut statut
 * @param montant montant (FCFA) ou {@code null} si masqué
 * @param dateLivraisonPrevue livraison prévue ou {@code null}
 * @param dateLivraisonReelle livraison réelle ou {@code null}
 * @param chantierNom chantier ou {@code null}
 */
public record ContratInfo(
    String id,
    String numero,
    String objet,
    String fournisseurNom,
    String statut,
    BigDecimal montant,
    LocalDate dateLivraisonPrevue,
    LocalDate dateLivraisonReelle,
    String chantierNom) {

  /**
   * @return une copie sans montant
   */
  public ContratInfo sansMontant() {
    return new ContratInfo(
        id,
        numero,
        objet,
        fournisseurNom,
        statut,
        null,
        dateLivraisonPrevue,
        dateLivraisonReelle,
        chantierNom);
  }
}
