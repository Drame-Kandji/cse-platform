package sn.cse.auth.domain.model;

import java.util.UUID;

public class ChefDeChantier extends Utilisateur {
    private UUID chantierAttribueId;

    public ChefDeChantier(UUID id, String nom, String prenom, String email, UUID chantierAttribueId) {
        super(id, nom, prenom, email);
        this.chantierAttribueId = chantierAttribueId;
    }

    public UUID getChantierAttribueId() { return chantierAttribueId; }
    public void setChantierAttribueId(UUID chantierAttribueId) { this.chantierAttribueId = chantierAttribueId; }
}
