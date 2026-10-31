package sn.cse.achats.application.service;

import java.util.UUID;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Facture;
import sn.cse.common.exception.ResourceNotFoundException;

/** Chargement des agrégats avec 404 explicite. */
final class Chargements {

  private Chargements() {}

  static AppelOffre appelOffre(AppelOffreRepository repository, UUID id) {
    return repository.parId(id).orElseThrow(() -> new ResourceNotFoundException("AppelOffre", id));
  }

  static ContratAchat contrat(ContratRepository repository, UUID id) {
    return repository.parId(id).orElseThrow(() -> new ResourceNotFoundException("Contrat", id));
  }

  static Facture facture(FactureRepository repository, UUID id) {
    return repository.parId(id).orElseThrow(() -> new ResourceNotFoundException("Facture", id));
  }
}
