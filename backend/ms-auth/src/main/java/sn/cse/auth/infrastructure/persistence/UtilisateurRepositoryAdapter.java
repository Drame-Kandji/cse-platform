package sn.cse.auth.infrastructure.persistence;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.application.query.CriteresRecherche;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;

/** Adaptateur JPA du port {@link UtilisateurRepository}. */
@Repository
public class UtilisateurRepositoryAdapter implements UtilisateurRepository {

  private final UtilisateurJpaRepository jpa;
  private final UtilisateurEntityMapper mapper;

  /**
   * @param jpa dépôt Spring Data
   * @param mapper conversion entité / domaine
   */
  public UtilisateurRepositoryAdapter(
      UtilisateurJpaRepository jpa, UtilisateurEntityMapper mapper) {
    this.jpa = jpa;
    this.mapper = mapper;
  }

  @Override
  public Optional<Utilisateur> parId(UUID id) {
    return jpa.findById(id).map(mapper::versDomaine);
  }

  @Override
  public Optional<Utilisateur> parEmail(String email) {
    return jpa.findByEmailIgnoreCase(email).map(mapper::versDomaine);
  }

  @Override
  public Utilisateur enregistrer(Utilisateur utilisateur) {
    Optional<UtilisateurEntity> existante = jpa.findById(utilisateur.id());
    if (existante.isPresent() && !existante.get().getRole().equals(utilisateur.role().code())) {
      jpa.changerRole(utilisateur.id(), utilisateur.role().code());
      existante = jpa.findById(utilisateur.id());
    }
    UtilisateurEntity entite = existante.orElseGet(() -> mapper.nouvelleEntite(utilisateur));
    mapper.copier(utilisateur, entite);
    return mapper.versDomaine(jpa.save(entite));
  }

  @Override
  public PageResponse<Utilisateur> rechercher(CriteresRecherche criteres, PageQuery page) {
    Page<UtilisateurEntity> resultat =
        jpa.findAll(specification(criteres), PageableFactory.toPageable(page));
    return PageResponse.of(
        resultat.getContent().stream().map(mapper::versDomaine).toList(),
        page.page(),
        page.size(),
        resultat.getTotalElements());
  }

  @Override
  public List<Utilisateur> tous() {
    return jpa.findAll().stream().map(mapper::versDomaine).toList();
  }

  @Override
  public Map<Role, Long> compterParRole() {
    Map<Role, Long> effectifs = new EnumMap<>(Role.class);
    for (Object[] ligne : jpa.compterParRole()) {
      Role.depuisCode((String) ligne[0]).ifPresent(r -> effectifs.put(r, (Long) ligne[1]));
    }
    return effectifs;
  }

  private static Specification<UtilisateurEntity> specification(CriteresRecherche c) {
    return (root, query, cb) -> {
      List<jakarta.persistence.criteria.Predicate> filtres = new ArrayList<>();
      if (c.role() != null) {
        filtres.add(cb.equal(root.get("role"), c.role().code()));
      }
      if (c.actif() != null) {
        filtres.add(cb.equal(root.get("actif"), c.actif()));
      }
      if (c.texte() != null) {
        String motif =
            "%"
                + c.texte()
                    .toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
                + "%";
        filtres.add(
            cb.or(
                cb.like(cb.lower(root.get("email")), motif, '\\'),
                cb.like(cb.lower(root.get("prenom")), motif, '\\'),
                cb.like(cb.lower(root.get("nom")), motif, '\\')));
      }
      return cb.and(filtres.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }
}
