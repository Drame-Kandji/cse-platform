package sn.cse.fournisseurs.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.query.CriteresFournisseurs;
import sn.cse.fournisseurs.domain.model.CoordonneesBancaires;
import sn.cse.fournisseurs.domain.model.DemandeBlacklistage;
import sn.cse.fournisseurs.domain.model.DocumentAdministratif;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.Performance;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.infrastructure.persistence.entity.DocumentEntity;
import sn.cse.fournisseurs.infrastructure.persistence.entity.FournisseurEntity;
import sn.cse.fournisseurs.infrastructure.persistence.repository.FournisseurJpaRepository;

/** Adaptateur JPA du port {@link FournisseurRepository} (mapping domaine ⇄ entité manuel). */
@Repository
@Transactional
public class FournisseurRepositoryAdapter implements FournisseurRepository {

  private final FournisseurJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public FournisseurRepositoryAdapter(FournisseurJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Fournisseur> parId(UUID id) {
    return jpa.findById(id).map(FournisseurRepositoryAdapter::versDomaine);
  }

  @Override
  public Fournisseur sauvegarder(Fournisseur fournisseur) {
    FournisseurEntity entite =
        jpa.findById(fournisseur.getId())
            .orElseGet(
                () -> {
                  FournisseurEntity nouvelle = new FournisseurEntity();
                  nouvelle.setId(fournisseur.getId());
                  return nouvelle;
                });
    copier(fournisseur, entite);
    return versDomaine(jpa.saveAndFlush(entite));
  }

  @Override
  public boolean nineaUtilise(String ninea, UUID exclu) {
    return exclu == null ? jpa.existsByNinea(ninea) : jpa.existsByNineaAndIdNot(ninea, exclu);
  }

  @Override
  public PageResponse<Fournisseur> rechercher(CriteresFournisseurs criteres, PageQuery page) {
    return Pages.versReponse(
        jpa.findAll(specification(criteres), PageableFactory.toPageable(page)),
        FournisseurRepositoryAdapter::versDomaine);
  }

  @Override
  public List<Fournisseur> parStatut(StatutFournisseur statut) {
    return jpa.findByStatut(statut).stream()
        .map(FournisseurRepositoryAdapter::versDomaine)
        .toList();
  }

  @Override
  public List<Fournisseur> parIds(Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return jpa.findAllById(ids).stream().map(FournisseurRepositoryAdapter::versDomaine).toList();
  }

  @Override
  public List<Fournisseur> avecDocumentsExpiresNonSignales(LocalDate aujourdhui) {
    return jpa.avecDocumentsExpiresNonSignales(aujourdhui).stream()
        .map(FournisseurRepositoryAdapter::versDomaine)
        .toList();
  }

  @Override
  public Map<StatutFournisseur, Long> compterParStatut() {
    Map<StatutFournisseur, Long> resultat = new EnumMap<>(StatutFournisseur.class);
    for (Object[] ligne : jpa.compterParStatut()) {
      resultat.put((StatutFournisseur) ligne[0], ((Number) ligne[1]).longValue());
    }
    return resultat;
  }

  @Override
  public long compterDocumentsExpires(LocalDate aujourdhui) {
    return jpa.compterDocumentsExpires(aujourdhui);
  }

  private static Specification<FournisseurEntity> specification(CriteresFournisseurs criteres) {
    return (root, query, cb) -> {
      List<Predicate> predicats = new ArrayList<>();
      if (criteres.statut() != null) {
        predicats.add(cb.equal(root.get("statut"), criteres.statut()));
      }
      if (criteres.specialite() != null) {
        predicats.add(cb.isMember(criteres.specialite(), root.get("specialites")));
      }
      if (criteres.q() != null) {
        String motif = "%" + criteres.q().toLowerCase(Locale.ROOT) + "%";
        predicats.add(
            cb.or(
                cb.like(cb.lower(root.get("raisonSociale")), motif),
                cb.like(cb.lower(root.get("ninea")), motif),
                cb.like(cb.lower(root.get("ville")), motif),
                cb.like(cb.lower(root.get("email")), motif)));
      }
      return cb.and(predicats.toArray(Predicate[]::new));
    };
  }

  private static void copier(Fournisseur f, FournisseurEntity e) {
    ProfilFournisseur p = f.getProfil();
    e.setRaisonSociale(p.raisonSociale());
    e.setNinea(p.ninea());
    e.setRccm(p.rccm());
    e.setAdresse(p.adresse());
    e.setVille(p.ville());
    e.setPays(p.pays());
    e.setTelephone(p.telephone());
    e.setEmail(p.email());
    e.setContactNom(p.contactNom());
    e.setContactFonction(p.contactFonction());
    e.getSpecialites().retainAll(p.specialites());
    e.getSpecialites().addAll(p.specialites());
    if (!e.getCertifications().equals(p.certifications())) {
      e.getCertifications().clear();
      e.getCertifications().addAll(p.certifications());
    }
    e.setStatut(f.getStatut());
    e.setMotifStatut(f.getMotifStatut().orElse(null));
    e.setBanque(f.getCoordonneesBancaires().map(CoordonneesBancaires::banque).orElse(null));
    e.setIban(f.getCoordonneesBancaires().map(CoordonneesBancaires::iban).orElse(null));
    Optional<DemandeBlacklistage> demande = f.getDemandeBlacklistage();
    e.setBlacklistageDemandeurId(demande.map(DemandeBlacklistage::demandeurId).orElse(null));
    e.setBlacklistageDemandeurNom(demande.map(DemandeBlacklistage::demandeurNom).orElse(null));
    e.setBlacklistageMotif(demande.map(DemandeBlacklistage::motif).orElse(null));
    e.setBlacklistageDate(demande.map(DemandeBlacklistage::date).orElse(null));
    Performance perf = f.getPerformance();
    e.setScorePerformance(perf.scorePerformance());
    e.setNombreEvaluations(perf.nombreEvaluations());
    e.setLivraisonsTotal(perf.livraisonsTotal());
    e.setLivraisonsALHeure(perf.livraisonsALHeure());
    e.setDateInscription(f.getDateInscription());
    synchroniserDocuments(f.getDocuments(), e);
  }

  private static void synchroniserDocuments(
      List<DocumentAdministratif> documents, FournisseurEntity e) {
    Map<UUID, DocumentEntity> existants =
        e.getDocuments().stream()
            .collect(Collectors.toMap(DocumentEntity::getId, Function.identity()));
    e.getDocuments()
        .removeIf(d -> documents.stream().noneMatch(doc -> doc.getId().equals(d.getId())));
    for (DocumentAdministratif doc : documents) {
      DocumentEntity entite = existants.get(doc.getId());
      if (entite == null) {
        entite = new DocumentEntity();
        entite.setId(doc.getId());
        entite.setFournisseur(e);
        e.getDocuments().add(entite);
      }
      entite.setType(doc.getType());
      entite.setLibelle(doc.getLibelle());
      entite.setDateExpiration(doc.getDateExpiration().orElse(null));
      entite.setExpirationNotifiee(doc.isExpirationNotifiee());
    }
  }

  static Fournisseur versDomaine(FournisseurEntity e) {
    ProfilFournisseur profil =
        new ProfilFournisseur(
            e.getRaisonSociale(),
            e.getNinea(),
            e.getRccm(),
            e.getAdresse(),
            e.getVille(),
            e.getPays(),
            e.getTelephone(),
            e.getEmail(),
            e.getContactNom(),
            e.getContactFonction(),
            e.getSpecialites(),
            List.copyOf(e.getCertifications()));
    CoordonneesBancaires coordonnees =
        e.getBanque() == null || e.getIban() == null
            ? null
            : new CoordonneesBancaires(e.getBanque(), e.getIban());
    DemandeBlacklistage demande =
        e.getBlacklistageMotif() == null
            ? null
            : new DemandeBlacklistage(
                e.getBlacklistageDemandeurId(),
                e.getBlacklistageDemandeurNom(),
                e.getBlacklistageMotif(),
                e.getBlacklistageDate());
    List<DocumentAdministratif> documents =
        e.getDocuments().stream()
            .map(
                d ->
                    DocumentAdministratif.reconstituer(
                        d.getId(),
                        d.getType(),
                        d.getLibelle(),
                        d.getDateExpiration(),
                        d.isExpirationNotifiee()))
            .toList();
    return Fournisseur.reconstituer(
        new Fournisseur.EtatFournisseur(
            e.getId(),
            profil,
            coordonnees,
            e.getStatut(),
            e.getMotifStatut(),
            demande,
            new Performance(
                e.getScorePerformance(),
                e.getNombreEvaluations(),
                e.getLivraisonsTotal(),
                e.getLivraisonsALHeure()),
            documents,
            e.getDateInscription(),
            e.getVersion() == null ? 0L : e.getVersion()));
  }
}
