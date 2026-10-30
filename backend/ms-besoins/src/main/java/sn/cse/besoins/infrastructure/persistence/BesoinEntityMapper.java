package sn.cse.besoins.infrastructure.persistence;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;
import sn.cse.besoins.domain.model.EtatBesoin;
import sn.cse.besoins.domain.model.HistoriqueBesoin;

/** Conversions entre l'agrégat, l'entité JPA et la vue de lecture. */
final class BesoinEntityMapper {

  private static final String SYSTEME = "systeme";

  private BesoinEntityMapper() {}

  /** Recopie l'état de l'agrégat dans l'entité (l'historique n'est complété que par ajout). */
  static void copier(EtatBesoin e, BesoinMaterielEntity cible) {
    DescriptionBesoin d = e.description();
    cible.id = e.id();
    cible.reference = e.reference();
    cible.chantierId = e.chantierId();
    cible.chantierNom = e.chantierNom();
    cible.demandeurId = e.demandeurId();
    cible.demandeurNom = e.demandeurNom();
    cible.designation = d.designation();
    cible.categorie = d.categorie();
    cible.typeEnginId = d.typeEnginId();
    cible.quantite = d.quantite();
    cible.specifications = d.specifications();
    cible.priorite = d.priorite();
    cible.dureeUtilisationJours = d.dureeUtilisationJours();
    cible.dateBesoin = d.dateBesoin();
    cible.justification = d.justification();
    cible.statut = e.statut();
    cible.motifRejet = e.motifRejet();
    cible.validateurId = e.validateurId();
    cible.validateurNom = e.validateurNom();
    cible.dateValidation = e.dateValidation();
    cible.orientationEnAttente = e.orientationEnAttente();
    if (!cible.materielIdsAffectes.equals(e.materielIdsAffectes())) {
      cible.materielIdsAffectes.clear();
      cible.materielIdsAffectes.addAll(e.materielIdsAffectes());
    }
    Set<UUID> connus =
        cible.historique.stream().map(h -> h.id).collect(Collectors.toUnmodifiableSet());
    e.historique().stream()
        .filter(h -> !connus.contains(h.id()))
        .map(BesoinEntityMapper::versEntite)
        .forEach(cible.historique::add);
    if (cible.createdAt == null) {
      cible.createdAt = e.creeLe();
      cible.createdBy = auteur(e.demandeurId(), e.demandeurNom());
    }
    cible.updatedAt = e.modifieLe();
    cible.updatedBy =
        e.historique().isEmpty()
            ? cible.createdBy
            : derniere(e.historique())
                .map(h -> auteur(h.auteurId(), h.auteurNom()))
                .orElse(SYSTEME);
  }

  static BesoinMateriel versDomaine(BesoinMaterielEntity s) {
    return BesoinMateriel.reconstituer(
        new EtatBesoin(
            s.id,
            s.reference,
            s.chantierId,
            s.chantierNom,
            s.demandeurId,
            s.demandeurNom,
            new DescriptionBesoin(
                s.designation,
                s.categorie,
                s.typeEnginId,
                s.quantite,
                s.specifications,
                s.priorite,
                s.dureeUtilisationJours,
                s.dateBesoin,
                s.justification),
            s.statut,
            s.motifRejet,
            s.validateurId,
            s.validateurNom,
            s.dateValidation,
            List.copyOf(s.materielIdsAffectes),
            s.orientationEnAttente,
            s.historique.stream().map(BesoinEntityMapper::versDomaine).toList(),
            s.version,
            s.createdAt,
            s.updatedAt));
  }

  static BesoinVue versVue(BesoinMaterielEntity s) {
    return new BesoinVue(
        s.id,
        s.reference,
        s.designation,
        s.categorie,
        s.typeEnginId,
        s.quantite,
        s.specifications,
        s.priorite,
        s.statut,
        s.chantierId,
        s.chantierNom,
        s.demandeurId,
        s.demandeurNom,
        s.dureeUtilisationJours,
        s.dateBesoin,
        s.justification,
        s.motifRejet,
        s.validateurId,
        s.validateurNom,
        s.dateValidation,
        List.copyOf(s.materielIdsAffectes),
        s.orientationEnAttente,
        s.createdAt,
        s.updatedAt,
        s.version == null ? 0L : s.version);
  }

  private static HistoriqueBesoin versDomaine(HistoriqueBesoinEntity h) {
    return new HistoriqueBesoin(
        h.id,
        h.statut,
        h.action,
        h.etapeValidation,
        h.auteurId,
        h.auteurNom,
        h.commentaire,
        h.date);
  }

  private static HistoriqueBesoinEntity versEntite(HistoriqueBesoin h) {
    HistoriqueBesoinEntity e = new HistoriqueBesoinEntity();
    e.id = h.id();
    e.statut = h.statut();
    e.action = h.action();
    e.etapeValidation = h.etapeValidation();
    e.auteurId = h.auteurId();
    e.auteurNom = h.auteurNom();
    e.commentaire = h.commentaire();
    e.date = h.date();
    return e;
  }

  private static java.util.Optional<HistoriqueBesoin> derniere(List<HistoriqueBesoin> historique) {
    return historique.stream().reduce((a, b) -> b);
  }

  private static String auteur(UUID id, String nom) {
    if (id != null) {
      return id.toString();
    }
    return nom == null ? SYSTEME : nom;
  }
}
