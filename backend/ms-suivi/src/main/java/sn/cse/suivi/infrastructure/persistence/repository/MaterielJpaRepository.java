package sn.cse.suivi.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.infrastructure.persistence.entity.MaterielEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaterielJpaRepository extends JpaRepository<MaterielEntity, UUID> {
    Optional<MaterielEntity> findByImmatriculation(String immatriculation);
    List<MaterielEntity> findByEtat(EtatMateriel etat);
    List<MaterielEntity> findByCategorie(String categorie);
    boolean existsByImmatriculation(String immatriculation);
}
