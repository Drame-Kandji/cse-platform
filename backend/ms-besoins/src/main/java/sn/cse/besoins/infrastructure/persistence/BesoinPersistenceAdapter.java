package sn.cse.besoins.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import sn.cse.besoins.application.port.out.BesoinRepository;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.EtatBesoin;

/** Adaptateur JPA du port {@link BesoinRepository}. */
@Repository
class BesoinPersistenceAdapter implements BesoinRepository {

  private final BesoinMaterielJpaRepository jpa;

  BesoinPersistenceAdapter(BesoinMaterielJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public BesoinMateriel enregistrer(BesoinMateriel besoin) {
    EtatBesoin etat = besoin.etat();
    BesoinMaterielEntity entite;
    if (etat.version() == null) {
      entite = new BesoinMaterielEntity();
    } else {
      entite =
          jpa.findById(etat.id())
              .filter(e -> Objects.equals(e.version, etat.version()))
              // L'agrégat a été lu dans une version antérieure : conflit de concurrence (409).
              .orElseThrow(
                  () ->
                      new ObjectOptimisticLockingFailureException(
                          BesoinMaterielEntity.class, etat.id()));
    }
    BesoinEntityMapper.copier(etat, entite);
    return BesoinEntityMapper.versDomaine(jpa.saveAndFlush(entite));
  }

  @Override
  public Optional<BesoinMateriel> trouver(UUID id) {
    return jpa.findById(id).map(BesoinEntityMapper::versDomaine);
  }

  @Override
  public boolean existe(UUID id) {
    return jpa.existsById(id);
  }
}
