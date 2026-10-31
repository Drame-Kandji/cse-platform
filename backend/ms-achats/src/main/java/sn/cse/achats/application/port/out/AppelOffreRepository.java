package sn.cse.achats.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Port de persistance des appels d'offres. */
public interface AppelOffreRepository {

  /** Champs triables des listes d'appels d'offres. */
  java.util.Set<String> TRIABLES =
      java.util.Set.of("datePublication", "dateLimite", "reference", "objet", "statut");

  /**
   * Critères de recherche.
   *
   * @param statut statut (facultatif)
   * @param type procédure (facultatif)
   * @param q texte libre sur la référence ou l'objet (facultatif)
   */
  record Filtre(StatutAO statut, TypeAppelOffre type, String q) {}

  /**
   * @param appelOffre agrégat à enregistrer (création ou mise à jour)
   * @return l'agrégat enregistré (version à jour)
   */
  AppelOffre enregistrer(AppelOffre appelOffre);

  /**
   * @param id identifiant
   * @return l'appel d'offres s'il existe
   */
  Optional<AppelOffre> parId(UUID id);

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page d'appels d'offres
   */
  PageResponse<AppelOffre> rechercher(Filtre filtre, PageQuery page);

  /**
   * @param maintenant instant de référence
   * @return identifiants des appels d'offres publiés dont la date limite est atteinte
   */
  List<UUID> idsPubliesEchus(Instant maintenant);

  /**
   * @param fournisseurId fournisseur du portail
   * @param maintenant instant de référence
   * @param page pagination
   * @return les appels d'offres publiés encore ouverts, accessibles à ce fournisseur
   */
  PageResponse<AppelOffre> ouvertsAuFournisseur(
      UUID fournisseurId, Instant maintenant, PageQuery page);

  /**
   * @param fournisseurId fournisseur du portail
   * @param page pagination
   * @return les appels d'offres sur lesquels ce fournisseur a déposé une offre
   */
  PageResponse<AppelOffre> avecOffreDu(UUID fournisseurId, PageQuery page);

  /**
   * @return nombre total d'appels d'offres
   */
  long compter();
}
