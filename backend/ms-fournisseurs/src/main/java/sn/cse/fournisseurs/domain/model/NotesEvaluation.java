package sn.cse.fournisseurs.domain.model;

import sn.cse.common.exception.BusinessRuleException;
import sn.cse.fournisseurs.domain.exception.CodesErreur;

/**
 * Notes d'une évaluation fournisseur, chacune comprise entre {@link #NOTE_MIN} et {@link
 * #NOTE_MAX}.
 *
 * @param qualite qualité du matériel livré
 * @param delais respect des délais
 * @param conformite conformité à la commande
 * @param sav qualité du service après-vente
 * @param prix niveau de prix
 */
public record NotesEvaluation(int qualite, int delais, int conformite, int sav, int prix) {

  /** Note minimale d'un critère. */
  public static final int NOTE_MIN = 0;

  /** Note maximale d'un critère. */
  public static final int NOTE_MAX = 10;

  /** Vérifie les bornes de chaque critère. */
  public NotesEvaluation {
    verifier("qualite", qualite);
    verifier("delais", delais);
    verifier("conformite", conformite);
    verifier("sav", sav);
    verifier("prix", prix);
  }

  private static void verifier(String critere, int note) {
    if (note < NOTE_MIN || note > NOTE_MAX) {
      throw new BusinessRuleException(
          CodesErreur.NOTE_HORS_BORNES,
          "La note « "
              + critere
              + " » doit être comprise entre "
              + NOTE_MIN
              + " et "
              + NOTE_MAX
              + ".");
    }
  }
}
