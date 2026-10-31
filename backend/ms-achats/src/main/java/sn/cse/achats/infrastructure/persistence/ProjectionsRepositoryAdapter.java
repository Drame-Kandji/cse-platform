package sn.cse.achats.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.out.BesoinAAcheterRepository;
import sn.cse.achats.application.port.out.FournisseurProjectionRepository;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.model.FournisseurConnu;
import sn.cse.achats.infrastructure.persistence.entity.BesoinAAcheterEntity;
import sn.cse.achats.infrastructure.persistence.entity.FournisseurProjectionEntity;
import sn.cse.achats.infrastructure.persistence.repository.BesoinAAcheterJpaRepository;
import sn.cse.achats.infrastructure.persistence.repository.FournisseurProjectionJpaRepository;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Adaptateur JPA des projections locales (besoins à acheter, fournisseurs). */
@Repository
@Transactional
public class ProjectionsRepositoryAdapter
    implements BesoinAAcheterRepository, FournisseurProjectionRepository {

  private final BesoinAAcheterJpaRepository besoins;
  private final FournisseurProjectionJpaRepository fournisseurs;

  /**
   * @param besoins dépôt des besoins
   * @param fournisseurs dépôt des fournisseurs
   */
  public ProjectionsRepositoryAdapter(
      BesoinAAcheterJpaRepository besoins, FournisseurProjectionJpaRepository fournisseurs) {
    this.besoins = besoins;
    this.fournisseurs = fournisseurs;
  }

  @Override
  public void enregistrer(BesoinAAcheter b) {
    BesoinAAcheterEntity e = besoins.findById(b.besoinId()).orElseGet(BesoinAAcheterEntity::new);
    e.setBesoinId(b.besoinId());
    e.setReference(b.reference());
    e.setDesignation(b.designation());
    e.setCategorie(b.categorie());
    e.setQuantite(b.quantite());
    e.setSpecifications(b.specifications());
    e.setPriorite(b.priorite());
    e.setChantierId(b.chantierId());
    e.setChantierNom(b.chantierNom());
    e.setDateReception(b.dateReception());
    e.setAppelOffreId(b.appelOffreId());
    besoins.save(e);
  }

  @Override
  public Optional<BesoinAAcheter> parBesoin(UUID besoinId) {
    return besoins.findById(besoinId).map(ProjectionsRepositoryAdapter::besoin);
  }

  @Override
  public PageResponse<BesoinAAcheter> lister(PageQuery page) {
    var pageable =
        PageRequest.of(
            page.page(),
            page.size(),
            Sort.by(Sort.Direction.DESC, "dateReception").and(Sort.by("besoinId")));
    return Pages.reponse(besoins.findAll(pageable), page, ProjectionsRepositoryAdapter::besoin);
  }

  @Override
  public Optional<FournisseurConnu> parId(UUID id) {
    return fournisseurs
        .findById(id)
        .map(f -> new FournisseurConnu(f.getFournisseurId(), f.getRaisonSociale(), f.getStatut()));
  }

  @Override
  public void enregistrerSiPlusRecent(FournisseurConnu fournisseur, Instant observeLe) {
    FournisseurProjectionEntity e =
        fournisseurs.findById(fournisseur.id()).orElseGet(FournisseurProjectionEntity::new);
    if (e.getDerniereMaj() != null && e.getDerniereMaj().isAfter(observeLe)) {
      return;
    }
    e.setFournisseurId(fournisseur.id());
    e.setRaisonSociale(fournisseur.raisonSociale());
    e.setStatut(fournisseur.statut());
    e.setDerniereMaj(observeLe);
    fournisseurs.save(e);
  }

  private static BesoinAAcheter besoin(BesoinAAcheterEntity e) {
    return new BesoinAAcheter(
        e.getBesoinId(),
        e.getReference(),
        e.getDesignation(),
        e.getCategorie(),
        e.getQuantite(),
        e.getSpecifications(),
        e.getPriorite(),
        e.getChantierId(),
        e.getChantierNom(),
        e.getDateReception(),
        e.getAppelOffreId());
  }
}
