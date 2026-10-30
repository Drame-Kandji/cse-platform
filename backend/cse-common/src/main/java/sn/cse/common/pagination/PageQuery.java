package sn.cse.common.pagination;

/**
 * Demande de pagination indépendante du framework, transmise aux ports de lecture.
 *
 * @param page index de page (base 0)
 * @param size taille (déjà bornée par la configuration)
 * @param sortField champ de tri (déjà validé contre la liste blanche) ou {@code null}
 * @param ascending sens du tri
 */
public record PageQuery(int page, int size, String sortField, boolean ascending) {

  public PageQuery {
    if (page < 0) {
      throw new IllegalArgumentException("page doit être >= 0");
    }
    if (size < 1) {
      throw new IllegalArgumentException("size doit être >= 1");
    }
  }
}
