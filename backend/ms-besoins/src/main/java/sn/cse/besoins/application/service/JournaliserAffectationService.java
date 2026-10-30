package sn.cse.besoins.application.service;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.besoins.application.command.JournaliserAffectationCommand;
import sn.cse.besoins.application.port.in.JournaliserAffectationUseCase;
import sn.cse.besoins.application.port.out.BesoinRepository;
import sn.cse.besoins.domain.model.Acteur;

/** Historise dans le besoin concerné les affectations notifiées par ms-suivi. */
@Service
public class JournaliserAffectationService implements JournaliserAffectationUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(JournaliserAffectationService.class);

  private final BesoinRepository repository;

  JournaliserAffectationService(BesoinRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public boolean journaliser(JournaliserAffectationCommand commande) {
    return repository
        .trouver(commande.besoinId())
        .map(
            besoin -> {
              besoin.journaliserAffectationMateriel(
                  commande.libelle(),
                  new Acteur(commande.auteurId(), commande.auteurNom(), Set.of()),
                  commande.date());
              repository.enregistrer(besoin);
              return true;
            })
        .orElseGet(
            () -> {
              LOG.debug("Affectation ignorée : besoin {} inconnu", commande.besoinId());
              return false;
            });
  }
}
