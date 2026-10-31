package sn.cse.achats.api.mapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.achats.api.dto.AppelOffreDto;
import sn.cse.achats.api.dto.AppelOffrePortailDto;
import sn.cse.achats.api.dto.ApprobationDto;
import sn.cse.achats.api.dto.AttributionDto;
import sn.cse.achats.api.dto.BesoinAAcheterDto;
import sn.cse.achats.api.dto.ClausesDto;
import sn.cse.achats.api.dto.ComparatifDto;
import sn.cse.achats.api.dto.ContratDto;
import sn.cse.achats.api.dto.CriterePondereDto;
import sn.cse.achats.api.dto.EngagementContratDto;
import sn.cse.achats.api.dto.EngagementDto;
import sn.cse.achats.api.dto.FactureDto;
import sn.cse.achats.api.dto.FournisseurInviteDto;
import sn.cse.achats.api.dto.JalonDto;
import sn.cse.achats.api.dto.MarchePortailDto;
import sn.cse.achats.api.dto.OffreDto;
import sn.cse.achats.api.dto.OffrePortailDto;
import sn.cse.achats.api.dto.SyntheseFinanceDto;
import sn.cse.achats.application.port.in.OffreFournisseur;
import sn.cse.achats.application.port.in.OffreVue;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.Approbation;
import sn.cse.achats.domain.model.Attribution;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.Engagement;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.Offre;
import sn.cse.achats.domain.service.Comparatif;
import sn.cse.achats.domain.service.SyntheseFinance;

/**
 * Conversion des objets du domaine en DTO de l'API. Les statuts dépendant de la date (jalons,
 * factures en retard) sont évalués par le domaine à la date de l'horloge injectée.
 */
@Component
public class AchatsApiMapper {

  private final Clock clock;

  /**
   * @param clock horloge
   */
  public AchatsApiMapper(Clock clock) {
    this.clock = clock;
  }

  /**
   * @param a appel d'offres
   * @param contratId contrat généré depuis l'appel d'offres (null s'il n'existe pas)
   * @return le DTO
   */
  public AppelOffreDto appelOffre(AppelOffre a, UUID contratId) {
    return new AppelOffreDto(
        a.getId(),
        a.getReference(),
        a.getObjet(),
        a.getDescription(),
        a.getType(),
        a.getOrigine().map(o -> o.besoinId()).orElse(null),
        a.getOrigine().map(o -> o.reference()).orElse(null),
        a.getOrigine().map(o -> o.chantierId()).orElse(null),
        a.getOrigine().map(o -> o.chantierNom()).orElse(null),
        a.getCategorie(),
        a.getQuantite(),
        a.getSpecifications(),
        a.getBudgetEstime(),
        a.getDatePublication(),
        a.getDateLimite(),
        a.getStatut(),
        criteres(a.getPonderation().criteres()),
        a.getInvites().stream()
            .map(i -> new FournisseurInviteDto(i.id(), i.raisonSociale()))
            .toList(),
        a.getOffres().size(),
        a.getOffreRetenueId(),
        a.getMontantAttribue(),
        a.getAttribution().map(at -> attribution(a, at)).orElse(null),
        contratId,
        a.getCreePar().nom(),
        a.getVersion());
  }

  /**
   * @param v offre et contexte d'anonymat
   * @return le DTO (identité masquée si anonyme, R10)
   */
  public OffreDto offre(OffreVue v) {
    Offre o = v.evaluation().offre();
    return new OffreDto(
        o.getId(),
        v.appelOffreId(),
        o.getCodeAnonyme(),
        v.anonyme() ? null : o.getFournisseurId(),
        v.anonyme() ? null : o.getFournisseurNom(),
        o.getMontantPropose(),
        o.getDelaiLivraisonJours(),
        o.getGarantieMois(),
        o.getDescription(),
        o.getDateDepot(),
        v.evaluation().notes(),
        v.evaluation().scoreGlobal(),
        v.evaluation().rang(),
        o.isRetenue());
  }

  /**
   * @param c comparatif (appel d'offres clôturé : anonymat levé)
   * @return le DTO
   */
  public ComparatifDto comparatif(Comparatif c) {
    return new ComparatifDto(
        c.appelOffreId(),
        criteres(c.criteres()),
        c.offres().stream().map(e -> offre(new OffreVue(c.appelOffreId(), false, e))).toList(),
        c.recommandationOffreId());
  }

  /**
   * @param c contrat
   * @return le DTO
   */
  public ContratDto contrat(ContratAchat c) {
    LocalDate aujourdhui = LocalDate.now(clock);
    return new ContratDto(
        c.getId(),
        c.getNumero(),
        c.getAppelOffreId(),
        c.getAppelOffreReference(),
        c.getOffreId(),
        c.getFournisseurId(),
        c.getFournisseurNom(),
        c.getBesoinId(),
        c.getChantierId(),
        c.getChantierNom(),
        c.getObjet(),
        c.getDesignation(),
        c.getCategorie(),
        c.getQuantite(),
        c.getMontant(),
        c.getStatut(),
        c.getDateSignature(),
        c.getDelaiLivraisonJours(),
        c.getDateLivraisonPrevue(),
        c.getDateLivraisonReelle(),
        c.getNumeroBonLivraison(),
        new ClausesDto(
            c.getClauses().garantieMois(),
            c.getClauses().conditionsMaintenance(),
            c.getClauses().conditionsLivraison(),
            c.getClauses().penaliteRetardPctParJour()),
        c.getEngagement().map(AchatsApiMapper::engagementContrat).orElse(null),
        c.getJalons().stream()
            .map(
                j ->
                    new JalonDto(
                        j.id(),
                        j.libelle(),
                        j.datePrevue(),
                        j.dateReelle(),
                        j.statutAu(aujourdhui)))
            .toList(),
        c.getPenalitesRetard(),
        c.getVersion());
  }

  /**
   * @param c contrat portant un engagement
   * @return la vue engagement
   */
  public EngagementDto engagement(ContratAchat c) {
    Engagement e = c.getEngagement().orElseThrow();
    return new EngagementDto(
        c.getId(),
        c.getNumero(),
        c.getFournisseurNom(),
        c.getChantierNom(),
        c.getObjet(),
        e.montant(),
        e.statut(),
        e.dateDemande(),
        e.dateDecision(),
        e.parNom());
  }

  /**
   * @param f facture
   * @return le DTO
   */
  public FactureDto facture(Facture f) {
    return new FactureDto(
        f.getId(),
        f.getNumero(),
        f.getContratId(),
        f.getContratNumero(),
        f.getFournisseurId(),
        f.getFournisseurNom(),
        f.getMontantHt(),
        f.getTva(),
        f.getMontantTtc(),
        f.getDateEmission(),
        f.getDateEcheance(),
        f.getStatut(),
        f.getReferenceMandat(),
        f.getDateMandatement(),
        f.getDatePaiement(),
        f.getMotifRejet(),
        f.estEnRetard(LocalDate.now(clock)));
  }

  /**
   * @param b besoin
   * @return le DTO
   */
  public BesoinAAcheterDto besoin(BesoinAAcheter b) {
    return new BesoinAAcheterDto(
        b.besoinId(),
        b.reference(),
        b.designation(),
        b.categorie(),
        b.quantite(),
        b.specifications(),
        b.priorite(),
        b.chantierId(),
        b.chantierNom(),
        b.dateReception(),
        b.appelOffreId());
  }

  /**
   * @param s synthèse
   * @return le DTO
   */
  public SyntheseFinanceDto synthese(SyntheseFinance s) {
    return new SyntheseFinanceDto(
        s.montantEngage(),
        s.montantEngagementsEnAttente(),
        s.montantFacture(),
        s.montantMandate(),
        s.montantPaye(),
        s.facturesEnRetard(),
        s.engagementsEnAttente());
  }

  /**
   * @param a appel d'offres
   * @param fournisseurId fournisseur connecté
   * @return la vue portail
   */
  public AppelOffrePortailDto appelOffrePortail(AppelOffre a, java.util.UUID fournisseurId) {
    return new AppelOffrePortailDto(
        a.getId(),
        a.getReference(),
        a.getObjet(),
        a.getDescription(),
        a.getType(),
        a.getCategorie(),
        a.getQuantite(),
        a.getSpecifications(),
        a.getDatePublication(),
        a.getDateLimite(),
        a.getStatut(),
        criteres(a.getPonderation().criteres()),
        a.offreDu(fournisseurId).isPresent());
  }

  /**
   * @param of offre du fournisseur connecté
   * @return la vue portail
   */
  public OffrePortailDto offrePortail(OffreFournisseur of) {
    Offre o = of.offre();
    AppelOffre a = of.appelOffre();
    return new OffrePortailDto(
        o.getId(),
        a.getId(),
        a.getReference(),
        a.getObjet(),
        a.getStatut(),
        a.getDateLimite(),
        o.getCodeAnonyme(),
        o.getMontantPropose(),
        o.getDelaiLivraisonJours(),
        o.getGarantieMois(),
        o.getDescription(),
        o.getDateDepot(),
        o.isRetenue());
  }

  /**
   * @param c contrat du fournisseur connecté
   * @return la vue portail
   */
  public MarchePortailDto marchePortail(ContratAchat c) {
    return new MarchePortailDto(
        c.getId(),
        c.getNumero(),
        c.getAppelOffreReference(),
        c.getObjet(),
        c.getMontant(),
        c.getStatut(),
        c.getDateSignature(),
        c.getDateLivraisonPrevue(),
        c.getDateLivraisonReelle());
  }

  private static AttributionDto attribution(AppelOffre a, Attribution at) {
    Offre o = a.offre(at.offreId());
    return new AttributionDto(
        at.offreId(),
        o.getCodeAnonyme(),
        o.getFournisseurNom(),
        at.montant(),
        at.justification(),
        at.proposeePar().nom(),
        at.dateProposition(),
        approbation(at.coValidationMateriel()),
        at.approbationDg() == null ? null : approbation(at.approbationDg()));
  }

  private static ApprobationDto approbation(Approbation a) {
    return new ApprobationDto(a.statut(), a.parNom(), a.date(), a.commentaire());
  }

  private static EngagementContratDto engagementContrat(Engagement e) {
    return new EngagementContratDto(
        e.statut(), e.montant(), e.parNom(), e.dateDemande(), e.dateDecision(), e.commentaire());
  }

  private static List<CriterePondereDto> criteres(List<CriterePondere> criteres) {
    return criteres.stream().map(c -> new CriterePondereDto(c.code(), c.poids())).toList();
  }
}
