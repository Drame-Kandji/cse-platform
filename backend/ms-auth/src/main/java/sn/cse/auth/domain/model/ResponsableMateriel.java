package sn.cse.auth.domain.model;

import java.util.UUID;

public class ResponsableMateriel extends Utilisateur {
    private String perimetreParc;

    public ResponsableMateriel(UUID id, String nom, String prenom, String email, String perimetreParc) {
        super(id, nom, prenom, email);
        this.perimetreParc = perimetreParc;
    }

    public String getPerimetreParc() { return perimetreParc; }
}
