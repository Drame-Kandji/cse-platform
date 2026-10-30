package sn.cse.besoins.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.besoins.application.port.out.BesoinEventPublisher;
import sn.cse.besoins.application.port.out.BesoinRepository;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.EvenementBesoin;
import sn.cse.common.exception.ResourceNotFoundException;

/**
 * Chargement et enregistrement des besoins avec publication des événements de domaine dans la même
 * transaction (outbox).
 */
@Component
class EnregistrementBesoin {

  static final String ENTITE = "Besoin";

  private final BesoinRepository repository;
  private final BesoinEventPublisher publisher;

  EnregistrementBesoin(BesoinRepository repository, BesoinEventPublisher publisher) {
    this.repository = repository;
    this.publisher = publisher;
  }

  BesoinMateriel charger(UUID id) {
    return repository.trouver(id).orElseThrow(() -> new ResourceNotFoundException(ENTITE, id));
  }

  BesoinMateriel enregistrerEtPublier(BesoinMateriel besoin) {
    List<EvenementBesoin> evenements = besoin.extraireEvenements();
    BesoinMateriel enregistre = repository.enregistrer(besoin);
    evenements.forEach(e -> publisher.publier(enregistre, e));
    return enregistre;
  }
}
