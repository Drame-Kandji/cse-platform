package sn.cse.besoins.application.port.in;

import sn.cse.besoins.domain.model.Priorite;

import java.util.UUID;

public interface SoumettreBesoinUseCase {
    SoumissionResult soumettre(SoumettreBesoinCommand command);

    record SoumettreBesoinCommand(String designation, int quantite, String specifications, Priorite priorite,
                                  UUID chantierId, UUID demandeurId, Integer dureeUtilisationPrevue,
                                  String justification) {}
    record SoumissionResult(UUID id, String reference, String statut) {}
}
