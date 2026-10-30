package sn.cse.suivi.application.port.out;

import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaterielRepositoryPort {
    Materiel save(Materiel materiel);
    Optional<Materiel> findById(UUID id);
    Optional<Materiel> findByImmatriculation(String immatriculation);
    List<Materiel> findAll();
    List<Materiel> findByEtat(EtatMateriel etat);
    List<Materiel> findByCategorie(String categorie);
    long countByEtatAndCategorie(EtatMateriel etat, String categorie);
}
