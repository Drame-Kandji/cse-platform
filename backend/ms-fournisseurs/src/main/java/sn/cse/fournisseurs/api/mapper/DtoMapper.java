package sn.cse.fournisseurs.api.mapper;

import java.util.List;
import sn.cse.fournisseurs.api.dto.BlacklistageDemandeDto;
import sn.cse.fournisseurs.api.dto.ClassementDto;
import sn.cse.fournisseurs.api.dto.ComparaisonPrixDto;
import sn.cse.fournisseurs.api.dto.CoordonneesBancairesDto;
import sn.cse.fournisseurs.api.dto.DocumentDto;
import sn.cse.fournisseurs.api.dto.EvaluationDto;
import sn.cse.fournisseurs.api.dto.FournisseurDetailDto;
import sn.cse.fournisseurs.api.dto.FournisseurDto;
import sn.cse.fournisseurs.api.dto.FournisseurRequest;
import sn.cse.fournisseurs.api.dto.LigneCatalogueDto;
import sn.cse.fournisseurs.api.dto.LitigeDto;
import sn.cse.fournisseurs.api.dto.ModifierFournisseurRequest;
import sn.cse.fournisseurs.api.dto.PrixHistoriqueDto;
import sn.cse.fournisseurs.api.dto.StatistiquesFournisseursDto;
import sn.cse.fournisseurs.application.command.FournisseurCommand;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.application.query.StatistiquesFournisseurs;
import sn.cse.fournisseurs.domain.model.DocumentAdministratif;
import sn.cse.fournisseurs.domain.model.Evaluation;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.Performance;
import sn.cse.fournisseurs.domain.model.PrixHistorique;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;
import sn.cse.fournisseurs.domain.service.ClassementFournisseurs;
import sn.cse.fournisseurs.domain.service.ComparateurPrix;

/** Conversions DTO ⇄ commandes / objets du domaine (sans logique métier). */
public final class DtoMapper {

  private DtoMapper() {}

  /**
   * @param r requête de création
   * @return commande
   */
  public static FournisseurCommand commande(FournisseurRequest r) {
    return new FournisseurCommand(
        r.raisonSociale(),
        r.ninea(),
        r.rccm(),
        r.adresse(),
        r.ville(),
        r.pays(),
        r.telephone(),
        r.email(),
        r.contactNom(),
        r.contactFonction(),
        r.specialites(),
        r.certifications(),
        r.coordonneesBancaires() == null ? null : r.coordonneesBancaires().banque(),
        r.coordonneesBancaires() == null ? null : r.coordonneesBancaires().iban());
  }

  /**
   * @param r requête de modification
   * @return commande
   */
  public static FournisseurCommand commande(ModifierFournisseurRequest r) {
    return new FournisseurCommand(
        r.raisonSociale(),
        r.ninea(),
        r.rccm(),
        r.adresse(),
        r.ville(),
        r.pays(),
        r.telephone(),
        r.email(),
        r.contactNom(),
        r.contactFonction(),
        r.specialites(),
        r.certifications(),
        r.coordonneesBancaires() == null ? null : r.coordonneesBancaires().banque(),
        r.coordonneesBancaires() == null ? null : r.coordonneesBancaires().iban());
  }

  /**
   * @param vue fournisseur enrichi
   * @return DTO liste
   */
  public static FournisseurDto fournisseur(FournisseurVue vue) {
    Fournisseur f = vue.fournisseur();
    ProfilFournisseur p = f.getProfil();
    Performance perf = f.getPerformance();
    return new FournisseurDto(
        f.getId(),
        p.raisonSociale(),
        p.ninea(),
        p.rccm(),
        p.adresse(),
        p.ville(),
        p.pays(),
        p.telephone(),
        p.email(),
        p.contactNom(),
        p.contactFonction(),
        p.specialites().stream().sorted().toList(),
        p.certifications(),
        f.getStatut(),
        f.getMotifStatut().orElse(null),
        perf.scorePerformance(),
        perf.nombreEvaluations(),
        perf.tauxLivraisonALHeure().orElse(null),
        vue.nombreLitigesOuverts(),
        vue.documentsExpires(),
        f.getDateInscription(),
        demande(f),
        f.getVersion());
  }

  /**
   * @param vue fournisseur enrichi
   * @return DTO détaillé (coordonnées bancaires selon la visibilité calculée par l'application)
   */
  public static FournisseurDetailDto detail(FournisseurVue vue) {
    FournisseurDto b = fournisseur(vue);
    Fournisseur f = vue.fournisseur();
    CoordonneesBancairesDto coordonnees =
        vue.coordonneesBancairesVisibles()
            ? f.getCoordonneesBancaires()
                .map(c -> new CoordonneesBancairesDto(c.banque(), c.iban()))
                .orElse(null)
            : null;
    List<DocumentDto> documents = f.getDocuments().stream().map(d -> document(d, vue)).toList();
    return new FournisseurDetailDto(
        b.id(),
        b.raisonSociale(),
        b.ninea(),
        b.rccm(),
        b.adresse(),
        b.ville(),
        b.pays(),
        b.telephone(),
        b.email(),
        b.contactNom(),
        b.contactFonction(),
        b.specialites(),
        b.certifications(),
        b.statut(),
        b.motifStatut(),
        b.scorePerformance(),
        b.nombreEvaluations(),
        b.tauxLivraisonALHeure(),
        b.nombreLitigesOuverts(),
        b.documentsExpires(),
        b.dateInscription(),
        b.blacklistageDemande(),
        b.version(),
        documents,
        coordonnees);
  }

  private static DocumentDto document(DocumentAdministratif d, FournisseurVue vue) {
    return document(d, vue.dateReference());
  }

  /**
   * @param d document
   * @param aujourdhui date de référence
   * @return DTO
   */
  public static DocumentDto document(DocumentAdministratif d, java.time.LocalDate aujourdhui) {
    return new DocumentDto(
        d.getId(),
        d.getType(),
        d.getLibelle(),
        d.getDateExpiration().orElse(null),
        d.estExpire(aujourdhui));
  }

  private static BlacklistageDemandeDto demande(Fournisseur f) {
    return f.getDemandeBlacklistage()
        .map(d -> new BlacklistageDemandeDto(d.demandeurNom(), d.motif(), d.date()))
        .orElse(null);
  }

  /**
   * @param e évaluation
   * @return DTO
   */
  public static EvaluationDto evaluation(Evaluation e) {
    return new EvaluationDto(
        e.id(),
        e.contratId(),
        e.notes().qualite(),
        e.notes().delais(),
        e.notes().conformite(),
        e.notes().sav(),
        e.notes().prix(),
        e.noteGlobale(),
        e.commentaire(),
        e.evaluateur().nom(),
        e.date());
  }

  /**
   * @param l ligne
   * @return DTO
   */
  public static LigneCatalogueDto ligne(LigneCatalogue l) {
    return new LigneCatalogueDto(
        l.getId(),
        l.getReference(),
        l.getDesignation(),
        l.getCategorie(),
        l.getPrixUnitaire(),
        l.getDonnees().unite(),
        l.getDonnees().delaiLivraisonJours(),
        l.getDateMaj());
  }

  /**
   * @param p entrée d'historique
   * @return DTO
   */
  public static PrixHistoriqueDto prix(PrixHistorique p) {
    return new PrixHistoriqueDto(p.reference(), p.designation(), p.prixUnitaire(), p.date());
  }

  /**
   * @param r rang
   * @return DTO
   */
  public static ClassementDto classement(ClassementFournisseurs.Rang r) {
    Fournisseur f = r.fournisseur();
    return new ClassementDto(
        r.rang(),
        f.getId(),
        f.getProfil().raisonSociale(),
        f.getPerformance().scorePerformance(),
        f.getPerformance().nombreEvaluations(),
        f.getPerformance().tauxLivraisonALHeure().orElse(null),
        r.nombreLitiges(),
        f.getProfil().specialites().stream().sorted().toList());
  }

  /**
   * @param c comparaison
   * @return DTO
   */
  public static ComparaisonPrixDto comparaison(ComparateurPrix.ComparaisonPrix c) {
    return new ComparaisonPrixDto(
        c.designation(),
        c.reference(),
        c.categorie(),
        c.prixMin(),
        c.prixMax(),
        c.prixMoyen(),
        c.offres().stream()
            .map(
                o ->
                    new ComparaisonPrixDto.Offre(
                        o.fournisseurId(),
                        o.raisonSociale(),
                        o.prixUnitaire(),
                        o.delaiLivraisonJours(),
                        o.scorePerformance()))
            .toList());
  }

  /**
   * @param l litige
   * @return DTO
   */
  public static LitigeDto litige(Litige l) {
    return new LitigeDto(
        l.getId(),
        l.getReference(),
        l.getFournisseurId(),
        l.getRaisonSociale(),
        l.getContratId().orElse(null),
        l.getObjet(),
        l.getDescription(),
        l.getGravite(),
        l.getStatut(),
        l.getDateOuverture(),
        l.getDateResolution().orElse(null),
        l.getActions().stream()
            .map(a -> new LitigeDto.Action(a.date(), a.auteurNom(), a.type(), a.commentaire()))
            .toList());
  }

  /**
   * @param s statistiques
   * @return DTO
   */
  public static StatistiquesFournisseursDto statistiques(StatistiquesFournisseurs s) {
    return new StatistiquesFournisseursDto(
        s.total(),
        s.actifs(),
        s.enAttente(),
        s.suspendus(),
        s.blacklistes(),
        s.scoreMoyen(),
        s.litigesOuverts(),
        s.documentsExpires());
  }
}
