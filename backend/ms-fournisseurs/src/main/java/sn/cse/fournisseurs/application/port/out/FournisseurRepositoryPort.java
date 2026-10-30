package sn.cse.fournisseurs.application.port.out;

import sn.cse.fournisseurs.domain.model.Fournisseur;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FournisseurRepositoryPort {
    List<Fournisseur> findAll();
    Optional<Fournisseur> findById(UUID id);
    Fournisseur save(Fournisseur fournisseur);
}

