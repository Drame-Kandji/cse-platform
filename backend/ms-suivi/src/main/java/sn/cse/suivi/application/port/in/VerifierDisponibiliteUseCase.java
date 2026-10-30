package sn.cse.suivi.application.port.in;

public interface VerifierDisponibiliteUseCase {
    DisponibiliteResult verifier(String categorie, String designation, int quantite);

    record DisponibiliteResult(boolean disponible, int quantiteDisponible) {}
}
