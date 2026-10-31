package sn.cse.achats.infrastructure.persistence.mapper;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import sn.cse.achats.domain.model.Acteur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.Approbation;
import sn.cse.achats.domain.model.Attribution;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.FournisseurInvite;
import sn.cse.achats.domain.model.Offre;
import sn.cse.achats.domain.model.OrigineBesoin;
import sn.cse.achats.domain.model.PonderationCriteres;
import sn.cse.achats.infrastructure.persistence.entity.AppelOffreEntity;
import sn.cse.achats.infrastructure.persistence.entity.CritereEmbeddable;
import sn.cse.achats.infrastructure.persistence.entity.InviteEmbeddable;
import sn.cse.achats.infrastructure.persistence.entity.NoteEmbeddable;
import sn.cse.achats.infrastructure.persistence.entity.OffreEntity;

/** Correspondance agrégat AppelOffre ↔ entités JPA. */
public final class AppelOffreEntityMapper {

  private AppelOffreEntityMapper() {}

  /**
   * @param e entité chargée
   * @return l'agrégat reconstitué
   */
  public static AppelOffre versDomaine(AppelOffreEntity e) {
    OrigineBesoin origine =
        e.getBesoinId() == null
            ? null
            : new OrigineBesoin(
                e.getBesoinId(),
                e.getBesoinReference(),
                e.getChantierId(),
                e.getChantierNom(),
                e.getBesoinDesignation());
    List<CriterePondere> criteres =
        e.getCriteres().stream()
            .map(c -> new CriterePondere(c.getCritere(), c.getPoids()))
            .toList();
    List<FournisseurInvite> invites =
        e.getInvites().stream()
            .map(i -> new FournisseurInvite(i.getFournisseurId(), i.getRaisonSociale()))
            .toList();
    List<Offre> offres = e.getOffres().stream().map(AppelOffreEntityMapper::offre).toList();
    return new AppelOffre(
        e.getId(),
        e.getReference(),
        e.getObjet(),
        e.getDescription(),
        e.getType(),
        origine,
        e.getCategorie(),
        e.getQuantite(),
        e.getSpecifications(),
        e.getBudgetEstime(),
        e.getDatePublication(),
        e.getDateLimite(),
        e.getDateCloture(),
        e.getStatut(),
        new PonderationCriteres(criteres),
        invites,
        offres,
        attribution(e),
        e.getOffreRetenueId(),
        e.getMontantAttribue(),
        new Acteur(e.getCreeParId(), e.getCreeParNom()),
        e.getVersion() == null ? 0 : e.getVersion());
  }

  /**
   * Recopie l'état de l'agrégat dans l'entité (nouvelle ou gérée par le contexte de persistance).
   *
   * @param a agrégat
   * @param e entité cible
   */
  public static void copier(AppelOffre a, AppelOffreEntity e) {
    boolean nouvelle = e.getId() == null;
    e.setId(a.getId());
    e.setReference(a.getReference());
    e.setObjet(a.getObjet());
    e.setDescription(a.getDescription());
    e.setType(a.getType());
    a.getOrigine()
        .ifPresent(
            o -> {
              e.setBesoinId(o.besoinId());
              e.setBesoinReference(o.reference());
              e.setBesoinDesignation(o.designation());
              e.setChantierId(o.chantierId());
              e.setChantierNom(o.chantierNom());
            });
    e.setCategorie(a.getCategorie());
    e.setQuantite(a.getQuantite());
    e.setSpecifications(a.getSpecifications());
    e.setBudgetEstime(a.getBudgetEstime());
    e.setDatePublication(a.getDatePublication());
    e.setDateLimite(a.getDateLimite());
    e.setDateCloture(a.getDateCloture());
    e.setStatut(a.getStatut());
    e.setOffreRetenueId(a.getOffreRetenueId());
    e.setMontantAttribue(a.getMontantAttribue());
    e.setCreeParId(a.getCreePar().id());
    e.setCreeParNom(a.getCreePar().nom());
    if (nouvelle) {
      // Critères et invités sont figés à la publication.
      a.getPonderation()
          .criteres()
          .forEach(c -> e.getCriteres().add(new CritereEmbeddable(c.code(), c.poids())));
      a.getInvites()
          .forEach(i -> e.getInvites().add(new InviteEmbeddable(i.id(), i.raisonSociale())));
    }
    copierAttribution(a, e);
    copierOffres(a, e);
  }

  private static void copierOffres(AppelOffre a, AppelOffreEntity e) {
    Map<UUID, OffreEntity> existantes =
        e.getOffres().stream().collect(Collectors.toMap(OffreEntity::getId, Function.identity()));
    for (Offre o : a.getOffres()) {
      OffreEntity cible = existantes.get(o.getId());
      if (cible == null) {
        cible = new OffreEntity();
        cible.setId(o.getId());
        cible.setAppelOffre(e);
        cible.setCodeAnonyme(o.getCodeAnonyme());
        cible.setFournisseurId(o.getFournisseurId());
        cible.setMontantPropose(o.getMontantPropose());
        cible.setDelaiLivraisonJours(o.getDelaiLivraisonJours());
        cible.setGarantieMois(o.getGarantieMois());
        cible.setDescription(o.getDescription());
        cible.setDateDepot(o.getDateDepot());
        e.getOffres().add(cible);
      }
      cible.setFournisseurNom(o.getFournisseurNom());
      cible.setRetenue(o.isRetenue());
      var notes =
          o.getNotesSaisies().entrySet().stream()
              .map(n -> new NoteEmbeddable(n.getKey(), n.getValue()))
              .collect(Collectors.toSet());
      if (!cible.getNotes().equals(notes)) {
        cible.getNotes().clear();
        cible.getNotes().addAll(notes);
      }
    }
  }

  private static void copierAttribution(AppelOffre a, AppelOffreEntity e) {
    Attribution attr = a.getAttribution().orElse(null);
    e.setAttributionOffreId(attr == null ? null : attr.offreId());
    e.setAttributionMontant(attr == null ? null : attr.montant());
    e.setAttributionJustification(attr == null ? null : attr.justification());
    e.setAttributionParId(attr == null ? null : attr.proposeePar().id());
    e.setAttributionParNom(attr == null ? null : attr.proposeePar().nom());
    e.setAttributionDate(attr == null ? null : attr.dateProposition());
    Approbation co = attr == null ? null : attr.coValidationMateriel();
    e.setCovalidationStatut(co == null ? null : co.statut());
    e.setCovalidationParId(co == null ? null : co.parId());
    e.setCovalidationParNom(co == null ? null : co.parNom());
    e.setCovalidationDate(co == null ? null : co.date());
    e.setCovalidationCommentaire(co == null ? null : co.commentaire());
    Approbation dg = attr == null ? null : attr.approbationDg();
    e.setApprobationDgStatut(dg == null ? null : dg.statut());
    e.setApprobationDgParId(dg == null ? null : dg.parId());
    e.setApprobationDgParNom(dg == null ? null : dg.parNom());
    e.setApprobationDgDate(dg == null ? null : dg.date());
    e.setApprobationDgCommentaire(dg == null ? null : dg.commentaire());
  }

  private static Attribution attribution(AppelOffreEntity e) {
    if (e.getAttributionOffreId() == null) {
      return null;
    }
    Approbation co =
        new Approbation(
            e.getCovalidationStatut(),
            e.getCovalidationParId(),
            e.getCovalidationParNom(),
            e.getCovalidationDate(),
            e.getCovalidationCommentaire());
    Approbation dg =
        e.getApprobationDgStatut() == null
            ? null
            : new Approbation(
                e.getApprobationDgStatut(),
                e.getApprobationDgParId(),
                e.getApprobationDgParNom(),
                e.getApprobationDgDate(),
                e.getApprobationDgCommentaire());
    return new Attribution(
        e.getAttributionOffreId(),
        e.getAttributionMontant(),
        e.getAttributionJustification(),
        new Acteur(e.getAttributionParId(), e.getAttributionParNom()),
        e.getAttributionDate(),
        co,
        dg);
  }

  private static Offre offre(OffreEntity o) {
    Map<CritereEvaluation, Integer> notes = new EnumMap<>(CritereEvaluation.class);
    o.getNotes().forEach(n -> notes.put(n.getCritere(), n.getNote()));
    return new Offre(
        o.getId(),
        o.getCodeAnonyme(),
        o.getFournisseurId(),
        o.getFournisseurNom(),
        o.getMontantPropose(),
        o.getDelaiLivraisonJours(),
        o.getGarantieMois(),
        o.getDescription(),
        o.getDateDepot(),
        new HashMap<>(notes),
        o.isRetenue());
  }
}
