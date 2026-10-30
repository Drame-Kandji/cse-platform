package sn.cse.fournisseurs.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.fournisseurs.application.port.out.CatalogueRepository;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.DonneesLigneCatalogue;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;
import sn.cse.fournisseurs.domain.model.PrixHistorique;
import sn.cse.fournisseurs.infrastructure.persistence.entity.LigneCatalogueEntity;
import sn.cse.fournisseurs.infrastructure.persistence.entity.PrixHistoriqueEntity;
import sn.cse.fournisseurs.infrastructure.persistence.repository.LigneCatalogueJpaRepository;
import sn.cse.fournisseurs.infrastructure.persistence.repository.PrixHistoriqueJpaRepository;

/** Adaptateur JPA du port {@link CatalogueRepository}. */
@Repository
@Transactional
public class CatalogueRepositoryAdapter implements CatalogueRepository {

  private final LigneCatalogueJpaRepository lignes;
  private final PrixHistoriqueJpaRepository historiques;

  /**
   * @param lignes dépôt des lignes
   * @param historiques dépôt de l'historique
   */
  public CatalogueRepositoryAdapter(
      LigneCatalogueJpaRepository lignes, PrixHistoriqueJpaRepository historiques) {
    this.lignes = lignes;
    this.historiques = historiques;
  }

  @Override
  public List<LigneCatalogue> parFournisseur(UUID fournisseurId) {
    return lignes.findByFournisseurIdOrderByReferenceAsc(fournisseurId).stream()
        .map(CatalogueRepositoryAdapter::versDomaine)
        .toList();
  }

  @Override
  public Optional<LigneCatalogue> parReference(UUID fournisseurId, String reference) {
    return lignes
        .findByFournisseurIdAndReference(fournisseurId, reference)
        .map(CatalogueRepositoryAdapter::versDomaine);
  }

  @Override
  public LigneCatalogue sauvegarder(LigneCatalogue ligne) {
    LigneCatalogueEntity e =
        lignes
            .findById(ligne.getId())
            .orElseGet(
                () -> {
                  LigneCatalogueEntity nouvelle = new LigneCatalogueEntity();
                  nouvelle.setId(ligne.getId());
                  nouvelle.setFournisseurId(ligne.getFournisseurId());
                  return nouvelle;
                });
    DonneesLigneCatalogue d = ligne.getDonnees();
    e.setReference(d.reference());
    e.setDesignation(d.designation());
    e.setCategorie(d.categorie());
    e.setPrixUnitaire(d.prixUnitaire());
    e.setUnite(d.unite());
    e.setDelaiLivraisonJours(d.delaiLivraisonJours());
    e.setDateMaj(ligne.getDateMaj());
    return versDomaine(lignes.saveAndFlush(e));
  }

  @Override
  public void historiser(PrixHistorique prix) {
    historiques.save(
        new PrixHistoriqueEntity(
            prix.id(),
            prix.fournisseurId(),
            prix.reference(),
            prix.designation(),
            prix.prixUnitaire(),
            prix.date()));
  }

  @Override
  public List<PrixHistorique> historique(UUID fournisseurId, String reference) {
    List<PrixHistoriqueEntity> entites =
        reference == null
            ? historiques.findByFournisseurIdOrderByDatePrixDesc(fournisseurId)
            : historiques.findByFournisseurIdAndReferenceOrderByDatePrixDesc(
                fournisseurId, reference);
    return entites.stream()
        .map(
            h ->
                new PrixHistorique(
                    h.getId(),
                    h.getFournisseurId(),
                    h.getReference(),
                    h.getDesignation(),
                    h.getPrixUnitaire(),
                    h.getDatePrix()))
        .toList();
  }

  @Override
  public List<LigneCatalogue> rechercher(
      List<UUID> fournisseurIds, CategorieMateriel categorie, String q) {
    if (fournisseurIds.isEmpty()) {
      return List.of();
    }
    Specification<LigneCatalogueEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicats = new ArrayList<>();
          predicats.add(root.get("fournisseurId").in(fournisseurIds));
          if (categorie != null) {
            predicats.add(cb.equal(root.get("categorie"), categorie));
          }
          if (q != null) {
            String motif = "%" + q.toLowerCase(Locale.ROOT) + "%";
            predicats.add(
                cb.or(
                    cb.like(cb.lower(root.get("designation")), motif),
                    cb.like(cb.lower(root.get("reference")), motif)));
          }
          return cb.and(predicats.toArray(Predicate[]::new));
        };
    return lignes.findAll(spec, Sort.by("designation")).stream()
        .map(CatalogueRepositoryAdapter::versDomaine)
        .toList();
  }

  private static LigneCatalogue versDomaine(LigneCatalogueEntity e) {
    return LigneCatalogue.reconstituer(
        e.getId(),
        e.getFournisseurId(),
        new DonneesLigneCatalogue(
            e.getReference(),
            e.getDesignation(),
            e.getCategorie(),
            e.getPrixUnitaire(),
            e.getUnite(),
            e.getDelaiLivraisonJours()),
        e.getDateMaj(),
        e.getVersion() == null ? 0L : e.getVersion());
  }
}
