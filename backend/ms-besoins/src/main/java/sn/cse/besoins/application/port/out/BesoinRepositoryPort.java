package sn.cse.besoins.application.port.out;

import sn.cse.besoins.domain.model.BesoinMateriel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BesoinRepositoryPort {
    BesoinMateriel save(BesoinMateriel besoin);
    Optional<BesoinMateriel> findById(UUID id);
    List<BesoinMateriel> findAll();
}
