package sn.cse.besoins.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import sn.cse.besoins.application.port.out.BesoinEventPublisher;
import sn.cse.besoins.application.port.out.BesoinRepository;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.EtatBesoin;
import sn.cse.besoins.domain.model.EvenementBesoin;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.model.TypeEvenementBesoin;

/** Doublures en mémoire des ports sortants. */
final class Fakes {

  private Fakes() {}

  /** Dépôt en mémoire qui incrémente la version à chaque enregistrement. */
  static final class Depot implements BesoinRepository {

    final Map<UUID, EtatBesoin> etats = new HashMap<>();

    @Override
    public BesoinMateriel enregistrer(BesoinMateriel besoin) {
      EtatBesoin e = besoin.etat();
      long version = e.version() == null ? 0 : e.version() + 1;
      EtatBesoin stocke =
          new EtatBesoin(
              e.id(),
              e.reference(),
              e.chantierId(),
              e.chantierNom(),
              e.demandeurId(),
              e.demandeurNom(),
              e.description(),
              e.statut(),
              e.motifRejet(),
              e.validateurId(),
              e.validateurNom(),
              e.dateValidation(),
              e.materielIdsAffectes(),
              e.orientationEnAttente(),
              e.historique(),
              version,
              e.creeLe(),
              e.modifieLe());
      etats.put(e.id(), stocke);
      return BesoinMateriel.reconstituer(stocke);
    }

    @Override
    public Optional<BesoinMateriel> trouver(UUID id) {
      return Optional.ofNullable(etats.get(id)).map(BesoinMateriel::reconstituer);
    }

    @Override
    public boolean existe(UUID id) {
      return etats.containsKey(id);
    }

    BesoinMateriel ajouter(BesoinMateriel besoin) {
      besoin.extraireEvenements();
      return enregistrer(besoin);
    }

    StatutBesoin statut(UUID id) {
      return etats.get(id).statut();
    }
  }

  /** Publication enregistrée en mémoire. */
  static final class Publication implements BesoinEventPublisher {

    final List<TypeEvenementBesoin> types = new ArrayList<>();
    final List<StatutBesoin> statuts = new ArrayList<>();

    @Override
    public void publier(BesoinMateriel besoin, EvenementBesoin evenement) {
      types.add(evenement.type());
      statuts.add(besoin.statut());
    }
  }
}
