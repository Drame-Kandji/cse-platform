package sn.cse.achats.infrastructure.persistence.mapper;

import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.infrastructure.persistence.entity.FactureEntity;

/** Correspondance agrégat Facture ↔ entité JPA. */
public final class FactureEntityMapper {

  private FactureEntityMapper() {}

  /**
   * @param e entité chargée
   * @return l'agrégat reconstitué
   */
  public static Facture versDomaine(FactureEntity e) {
    return new Facture(
        e.getId(),
        e.getNumero(),
        e.getContratId(),
        e.getContratNumero(),
        e.getFournisseurId(),
        e.getFournisseurNom(),
        e.getMontantHt(),
        e.getTva(),
        e.getDateEmission(),
        e.getDateEcheance(),
        e.getStatut(),
        e.getReferenceMandat(),
        e.getDateMandatement(),
        e.getDatePaiement(),
        e.getMotifRejet(),
        e.getVersion() == null ? 0 : e.getVersion());
  }

  /**
   * @param f agrégat
   * @param e entité cible
   */
  public static void copier(Facture f, FactureEntity e) {
    e.setId(f.getId());
    e.setNumero(f.getNumero());
    e.setContratId(f.getContratId());
    e.setContratNumero(f.getContratNumero());
    e.setFournisseurId(f.getFournisseurId());
    e.setFournisseurNom(f.getFournisseurNom());
    e.setMontantHt(f.getMontantHt());
    e.setTva(f.getTva());
    e.setDateEmission(f.getDateEmission());
    e.setDateEcheance(f.getDateEcheance());
    e.setStatut(f.getStatut());
    e.setReferenceMandat(f.getReferenceMandat());
    e.setDateMandatement(f.getDateMandatement());
    e.setDatePaiement(f.getDatePaiement());
    e.setMotifRejet(f.getMotifRejet());
  }
}
