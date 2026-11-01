package sn.cse.achats.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import sn.cse.achats.domain.model.ClausesContrat;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Engagement;
import sn.cse.achats.domain.model.Jalon;
import sn.cse.achats.domain.model.Signature;
import sn.cse.achats.infrastructure.persistence.entity.ContratAchatEntity;
import sn.cse.achats.infrastructure.persistence.entity.JalonEntity;

/** Correspondance agrégat ContratAchat ↔ entités JPA. */
public final class ContratEntityMapper {

  private ContratEntityMapper() {}

  /**
   * @param e entité chargée
   * @return l'agrégat reconstitué
   */
  public static ContratAchat versDomaine(ContratAchatEntity e) {
    Engagement engagement =
        e.getEngagementStatut() == null
            ? null
            : new Engagement(
                e.getEngagementStatut(),
                e.getEngagementMontant(),
                e.getEngagementParId(),
                e.getEngagementParNom(),
                e.getEngagementDateDemande(),
                e.getEngagementDateDecision(),
                e.getEngagementCommentaire());
    List<Jalon> jalons =
        e.getJalons().stream()
            .map(j -> new Jalon(j.getId(), j.getLibelle(), j.getDatePrevue(), j.getDateReelle()))
            .toList();
    return new ContratAchat(
        e.getId(),
        e.getNumero(),
        e.getAppelOffreId(),
        e.getAppelOffreReference(),
        e.getOffreId(),
        e.getFournisseurId(),
        e.getFournisseurNom(),
        e.getBesoinId(),
        e.getChantierId(),
        e.getChantierNom(),
        e.getObjet(),
        e.getDesignation(),
        e.getCategorie(),
        e.getQuantite(),
        e.getMontant(),
        e.getStatut(),
        e.getDateSignature(),
        e.getDelaiLivraisonJours(),
        e.getDateLivraisonPrevue(),
        e.getDateLivraisonReelle(),
        e.getNumeroBonLivraison(),
        e.getObservationsLivraison(),
        new ClausesContrat(
            e.getGarantieMois(),
            e.getConditionsMaintenance(),
            e.getConditionsLivraison(),
            e.getPenaliteRetardPctParJour()),
        engagement,
        jalons,
        e.getPenalitesRetard(),
        e.getDateDerniereAlerteRetard(),
        signature(e.getSignatureCseParId(), e.getSignatureCseParNom(), e.getSignatureCseDate()),
        signature(
            e.getSignatureFournisseurParId(),
            e.getSignatureFournisseurParNom(),
            e.getSignatureFournisseurDate()),
        e.getMotifRefusSignature(),
        e.getDateExpedition(),
        e.getCommentaireExpedition(),
        e.getVersion() == null ? 0 : e.getVersion());
  }

  private static Signature signature(UUID parId, String parNom, Instant date) {
    return parNom == null || date == null ? null : new Signature(parId, parNom, date);
  }

  /**
   * @param c agrégat
   * @param e entité cible
   */
  public static void copier(ContratAchat c, ContratAchatEntity e) {
    e.setId(c.getId());
    e.setNumero(c.getNumero());
    e.setAppelOffreId(c.getAppelOffreId());
    e.setAppelOffreReference(c.getAppelOffreReference());
    e.setOffreId(c.getOffreId());
    e.setFournisseurId(c.getFournisseurId());
    e.setFournisseurNom(c.getFournisseurNom());
    e.setBesoinId(c.getBesoinId());
    e.setChantierId(c.getChantierId());
    e.setChantierNom(c.getChantierNom());
    e.setObjet(c.getObjet());
    e.setDesignation(c.getDesignation());
    e.setCategorie(c.getCategorie());
    e.setQuantite(c.getQuantite());
    e.setMontant(c.getMontant());
    e.setStatut(c.getStatut());
    e.setDateSignature(c.getDateSignature());
    e.setDelaiLivraisonJours(c.getDelaiLivraisonJours());
    e.setDateLivraisonPrevue(c.getDateLivraisonPrevue());
    e.setDateLivraisonReelle(c.getDateLivraisonReelle());
    e.setNumeroBonLivraison(c.getNumeroBonLivraison());
    e.setObservationsLivraison(c.getObservationsLivraison());
    ClausesContrat cl = c.getClauses();
    e.setGarantieMois(cl.garantieMois());
    e.setConditionsMaintenance(cl.conditionsMaintenance());
    e.setConditionsLivraison(cl.conditionsLivraison());
    e.setPenaliteRetardPctParJour(cl.penaliteRetardPctParJour());
    Engagement en = c.getEngagement().orElse(null);
    e.setEngagementStatut(en == null ? null : en.statut());
    e.setEngagementMontant(en == null ? null : en.montant());
    e.setEngagementParId(en == null ? null : en.parId());
    e.setEngagementParNom(en == null ? null : en.parNom());
    e.setEngagementDateDemande(en == null ? null : en.dateDemande());
    e.setEngagementDateDecision(en == null ? null : en.dateDecision());
    e.setEngagementCommentaire(en == null ? null : en.commentaire());
    e.setPenalitesRetard(c.getPenalitesRetard());
    e.setDateDerniereAlerteRetard(c.getDateDerniereAlerteRetard());
    Signature cse = c.getSignatureCse().orElse(null);
    e.setSignatureCseParId(cse == null ? null : cse.parId());
    e.setSignatureCseParNom(cse == null ? null : cse.parNom());
    e.setSignatureCseDate(cse == null ? null : cse.date());
    Signature fournisseur = c.getSignatureFournisseur().orElse(null);
    e.setSignatureFournisseurParId(fournisseur == null ? null : fournisseur.parId());
    e.setSignatureFournisseurParNom(fournisseur == null ? null : fournisseur.parNom());
    e.setSignatureFournisseurDate(fournisseur == null ? null : fournisseur.date());
    e.setMotifRefusSignature(c.getMotifRefusSignature());
    e.setDateExpedition(c.getDateExpedition());
    e.setCommentaireExpedition(c.getCommentaireExpedition());
    copierJalons(c, e);
  }

  private static void copierJalons(ContratAchat c, ContratAchatEntity e) {
    Map<UUID, Jalon> voulus =
        c.getJalons().stream().collect(Collectors.toMap(Jalon::id, Function.identity()));
    for (Iterator<JalonEntity> it = e.getJalons().iterator(); it.hasNext(); ) {
      if (!voulus.containsKey(it.next().getId())) {
        it.remove();
      }
    }
    Map<UUID, JalonEntity> existants =
        e.getJalons().stream().collect(Collectors.toMap(JalonEntity::getId, Function.identity()));
    List<JalonEntity> ordonnes = new ArrayList<>();
    int ordre = 0;
    for (Jalon j : c.getJalons()) {
      JalonEntity cible = existants.get(j.id());
      if (cible == null) {
        cible = new JalonEntity();
        cible.setId(j.id());
        cible.setContrat(e);
      }
      cible.setOrdre(ordre++);
      cible.setLibelle(j.libelle());
      cible.setDatePrevue(j.datePrevue());
      cible.setDateReelle(j.dateReelle());
      ordonnes.add(cible);
    }
    e.getJalons().clear();
    e.getJalons().addAll(ordonnes);
  }
}
