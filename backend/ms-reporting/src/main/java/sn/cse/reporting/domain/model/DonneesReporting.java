package sn.cse.reporting.domain.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.AppelOffreVue;
import sn.cse.reporting.domain.model.vue.BesoinVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.ContratVue;
import sn.cse.reporting.domain.model.vue.FactureVue;
import sn.cse.reporting.domain.model.vue.FournisseurVue;
import sn.cse.reporting.domain.model.vue.LitigeVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;
import sn.cse.reporting.domain.model.vue.PanneVue;

/**
 * Jeu de vues sur lequel les indicateurs sont calculés. Les listes peuvent contenir plus que la
 * période demandée : chaque calcul applique ses propres règles d'appartenance à la période.
 *
 * @param chantiers chantiers
 * @param materiels matériels
 * @param affectations affectations
 * @param maintenances maintenances
 * @param pannes pannes
 * @param besoins besoins
 * @param appelsOffres appels d'offres
 * @param contrats contrats
 * @param factures factures
 * @param fournisseurs fournisseurs
 * @param litiges litiges
 */
public record DonneesReporting(
    List<ChantierVue> chantiers,
    List<MaterielVue> materiels,
    List<AffectationVue> affectations,
    List<MaintenanceVue> maintenances,
    List<PanneVue> pannes,
    List<BesoinVue> besoins,
    List<AppelOffreVue> appelsOffres,
    List<ContratVue> contrats,
    List<FactureVue> factures,
    List<FournisseurVue> fournisseurs,
    List<LitigeVue> litiges) {

  public DonneesReporting {
    chantiers = List.copyOf(chantiers);
    materiels = List.copyOf(materiels);
    affectations = List.copyOf(affectations);
    maintenances = List.copyOf(maintenances);
    pannes = List.copyOf(pannes);
    besoins = List.copyOf(besoins);
    appelsOffres = List.copyOf(appelsOffres);
    contrats = List.copyOf(contrats);
    factures = List.copyOf(factures);
    fournisseurs = List.copyOf(fournisseurs);
    litiges = List.copyOf(litiges);
  }

  /**
   * @return un jeu vide
   */
  public static DonneesReporting vide() {
    return new DonneesReporting(
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
        List.of(), List.of(), List.of());
  }

  /**
   * Restreint les données rattachées à un chantier au périmètre. Les matériels sont retenus selon
   * leur chantier courant, les factures selon le chantier de leur contrat. Fournisseurs et litiges,
   * transverses, ne sont pas filtrés.
   *
   * @param perimetre périmètre visible
   * @return le jeu restreint
   */
  public DonneesReporting restreindre(Perimetre perimetre) {
    if (!perimetre.restreint()) {
      return this;
    }
    List<ContratVue> contratsVisibles =
        contrats.stream().filter(c -> perimetre.inclut(c.chantierId())).toList();
    Set<UUID> idsContrats =
        contratsVisibles.stream().map(ContratVue::id).collect(Collectors.toSet());
    return new DonneesReporting(
        chantiers.stream().filter(c -> perimetre.inclut(c.id())).toList(),
        materiels.stream().filter(m -> perimetre.inclut(m.chantierId())).toList(),
        affectations.stream().filter(a -> perimetre.inclut(a.chantierId())).toList(),
        maintenances.stream().filter(m -> perimetre.inclut(m.chantierId())).toList(),
        pannes.stream().filter(p -> perimetre.inclut(p.chantierId())).toList(),
        besoins.stream().filter(b -> perimetre.inclut(b.chantierId())).toList(),
        appelsOffres.stream().filter(a -> perimetre.inclut(a.chantierId())).toList(),
        contratsVisibles,
        factures.stream().filter(f -> idsContrats.contains(f.contratId())).toList(),
        fournisseurs,
        litiges);
  }
}
