package sn.cse.suivi.api.mapper;

import java.util.ArrayList;
import java.util.List;
import sn.cse.suivi.api.dto.AffectationDto;
import sn.cse.suivi.api.dto.CarteDto;
import sn.cse.suivi.api.dto.ChantierDto;
import sn.cse.suivi.api.dto.ChantierRequest;
import sn.cse.suivi.api.dto.DisponibiliteDto;
import sn.cse.suivi.api.dto.MaintenanceDto;
import sn.cse.suivi.api.dto.MaterielDetailDto;
import sn.cse.suivi.api.dto.MaterielDto;
import sn.cse.suivi.api.dto.MaterielRequest;
import sn.cse.suivi.api.dto.MouvementDto;
import sn.cse.suivi.api.dto.PanneDto;
import sn.cse.suivi.api.dto.TypeEnginDto;
import sn.cse.suivi.api.dto.TypeEnginRequest;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.application.query.CarteVue;
import sn.cse.suivi.application.query.ChantierVue;
import sn.cse.suivi.application.query.DisponibiliteVue;
import sn.cse.suivi.application.query.MaintenanceVue;
import sn.cse.suivi.application.query.MaterielDetailVue;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.application.query.PanneVue;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Coordonnees;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesChantier;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.domain.model.TypeEngin;

/** Conversions manuelles entre DTO de l'API et objets de l'application / du domaine. */
public final class ApiMapper {

  /** Valeur {@code kind} d'un point chantier sur la carte. */
  public static final String KIND_CHANTIER = "CHANTIER";

  /** Valeur {@code kind} d'un point matériel sur la carte. */
  public static final String KIND_MATERIEL = "MATERIEL";

  private ApiMapper() {}

  /**
   * @param r requête
   * @return les données du domaine
   */
  public static DonneesChantier donnees(ChantierRequest r) {
    return new DonneesChantier(
        r.code(),
        r.nom(),
        r.localisation(),
        r.region(),
        new Coordonnees(r.latitude(), r.longitude()),
        r.budget(),
        r.montantMarche(),
        r.dateDebut(),
        r.dateFinPrevue(),
        r.chefChantierId(),
        r.chefChantierNom());
  }

  /**
   * @param r requête
   * @return la description du domaine
   */
  public static DescriptionMateriel description(MaterielRequest r) {
    return new DescriptionMateriel(
        r.code(),
        r.immatriculation(),
        r.designation(),
        r.categorie(),
        r.typeEnginId(),
        r.marque(),
        r.modele(),
        r.tarifJournalier(),
        r.valeurAcquisition(),
        r.dateAcquisition());
  }

  /**
   * @param r requête
   * @return les données du domaine
   */
  public static DonneesTypeEngin donnees(TypeEnginRequest r) {
    return new DonneesTypeEngin(
        r.code(),
        r.libelle(),
        r.categorie(),
        r.seuilMaintenanceHeures(),
        r.seuilMaintenanceJours(),
        Boolean.TRUE.equals(r.actif()));
  }

  /**
   * @param v vue
   * @return le DTO
   */
  public static ChantierDto chantier(ChantierVue v) {
    Chantier c = v.chantier();
    return new ChantierDto(
        c.getId(),
        c.getCode(),
        c.getNom(),
        c.getLocalisation(),
        c.getRegion(),
        c.getCoordonnees().latitude(),
        c.getCoordonnees().longitude(),
        c.getStatut(),
        c.getBudget(),
        c.getMontantMarche(),
        c.getDateDebut(),
        c.getDateFinPrevue(),
        c.getChefChantierId(),
        c.getChefChantierNom(),
        v.nombreMateriels(),
        c.getVersion());
  }

  /**
   * @param v vue
   * @return le DTO
   */
  public static MaterielDto materiel(MaterielVue v) {
    Materiel m = v.materiel();
    DescriptionMateriel d = m.getDescription();
    Coordonnees p = m.getPosition();
    return new MaterielDto(
        m.getId(),
        d.code(),
        d.immatriculation(),
        d.designation(),
        d.categorie(),
        d.typeEnginId(),
        v.typeEnginLibelle(),
        d.marque(),
        d.modele(),
        m.getEtat(),
        m.getHeuresFonctionnement(),
        m.heuresDepuisMaintenance(),
        m.getSeuilMaintenanceHeures(),
        m.prochaineMaintenance(),
        v.maintenanceDue(),
        d.tarifJournalier(),
        d.valeurAcquisition(),
        d.dateAcquisition(),
        m.getChantierActuelId(),
        v.chantierActuelNom(),
        p == null ? null : p.latitude(),
        p == null ? null : p.longitude(),
        m.getFournisseurId(),
        m.getContratId(),
        m.isReformeDemandee(),
        m.getMotifReforme(),
        m.getVersion());
  }

  /**
   * @param v vue
   * @return le DTO
   */
  public static MaterielDetailDto detail(MaterielDetailVue v) {
    return new MaterielDetailDto(
        materiel(v.materiel()),
        v.affectations().stream().map(ApiMapper::affectation).toList(),
        v.maintenances().stream().map(ApiMapper::maintenance).toList(),
        v.pannes().stream().map(ApiMapper::panne).toList(),
        v.mouvements().stream().map(ApiMapper::mouvement).toList());
  }

  /**
   * @param v vue
   * @return le DTO
   */
  public static AffectationDto affectation(AffectationVue v) {
    Affectation a = v.affectation();
    Affectation.Snapshot s = a.snapshot();
    return new AffectationDto(
        a.getId(),
        a.getMaterielId(),
        v.materielCode(),
        v.materielDesignation(),
        v.categorie(),
        a.getChantierId(),
        v.chantierNom(),
        a.getBesoinId(),
        a.getDateDebut(),
        a.getDateFinPrevue(),
        a.getDateFinReelle(),
        a.getStatut(),
        a.getDateReception(),
        s.observationsReception(),
        a.getFournisseurId(),
        a.getContratId());
  }

  /**
   * @param m mouvement
   * @return le DTO
   */
  public static MouvementDto mouvement(Mouvement m) {
    return new MouvementDto(
        m.id(),
        m.materielId(),
        m.type(),
        m.chantierOrigineNom(),
        m.chantierDestinationNom(),
        m.date(),
        m.auteurNom(),
        m.commentaire());
  }

  /**
   * @param v vue
   * @return le DTO
   */
  public static MaintenanceDto maintenance(MaintenanceVue v) {
    Maintenance.Snapshot s = v.maintenance().snapshot();
    return new MaintenanceDto(
        s.id(),
        s.numeroOt(),
        s.materielId(),
        v.materielCode(),
        v.materielDesignation(),
        s.chantierId(),
        v.chantierNom(),
        s.type(),
        s.statut(),
        s.description(),
        s.datePlanifiee(),
        s.dateDebut(),
        s.dateFin(),
        s.technicien(),
        s.cout(),
        s.heuresCompteur(),
        s.observations(),
        s.panneId());
  }

  /**
   * @param v vue
   * @return le DTO
   */
  public static PanneDto panne(PanneVue v) {
    Panne.Snapshot s = v.panne().snapshot();
    return new PanneDto(
        s.id(),
        s.materielId(),
        v.materielCode(),
        v.materielDesignation(),
        s.chantierId(),
        v.chantierNom(),
        s.gravite(),
        s.description(),
        s.statut(),
        s.signaleParId(),
        s.signaleParNom(),
        s.dateSignalement(),
        s.maintenanceId());
  }

  /**
   * @param t type d'engin
   * @return le DTO
   */
  public static TypeEnginDto typeEngin(TypeEngin t) {
    DonneesTypeEngin d = t.getDonnees();
    return new TypeEnginDto(
        t.getId(),
        d.code(),
        d.libelle(),
        d.categorie(),
        d.seuilMaintenanceHeures(),
        d.seuilMaintenanceJours(),
        d.actif());
  }

  /**
   * @param v vue
   * @return le DTO
   */
  public static DisponibiliteDto disponibilite(DisponibiliteVue v) {
    return new DisponibiliteDto(
        v.disponible(),
        v.quantiteDemandee(),
        v.quantiteDisponible(),
        v.materiels().stream().map(ApiMapper::materiel).toList());
  }

  /**
   * @param v vue
   * @return la FeatureCollection GeoJSON (coordonnées {@code [longitude, latitude]})
   */
  public static CarteDto carte(CarteVue v) {
    List<CarteDto.Feature> points = new ArrayList<>();
    for (ChantierVue cv : v.chantiers()) {
      Chantier c = cv.chantier();
      points.add(
          CarteDto.Feature.point(
              c.getCoordonnees().longitude(),
              c.getCoordonnees().latitude(),
              new CarteDto.Proprietes(
                  KIND_CHANTIER,
                  c.getId(),
                  c.getCode(),
                  c.getNom(),
                  null,
                  null,
                  c.getStatut(),
                  null,
                  null,
                  cv.nombreMateriels())));
    }
    for (MaterielVue mv : v.materiels()) {
      Materiel m = mv.materiel();
      points.add(
          CarteDto.Feature.point(
              m.getPosition().longitude(),
              m.getPosition().latitude(),
              new CarteDto.Proprietes(
                  KIND_MATERIEL,
                  m.getId(),
                  m.getCode(),
                  m.getDesignation(),
                  m.getCategorie(),
                  m.getEtat(),
                  null,
                  m.getChantierActuelId(),
                  mv.chantierActuelNom(),
                  null)));
    }
    return CarteDto.de(points);
  }
}
