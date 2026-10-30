package sn.cse.fournisseurs.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.port.out.LitigeRepository;
import sn.cse.fournisseurs.application.query.CompteurLitiges;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.domain.model.Fournisseur;

/** Chargement des fournisseurs et construction des vues enrichies (litiges, documents). */
@Component
public class ChargeurFournisseur {

  private final FournisseurRepository fournisseurs;
  private final LitigeRepository litiges;
  private final Clock clock;

  /**
   * @param fournisseurs dépôt des fournisseurs
   * @param litiges dépôt des litiges
   * @param clock horloge
   */
  public ChargeurFournisseur(
      FournisseurRepository fournisseurs, LitigeRepository litiges, Clock clock) {
    this.fournisseurs = fournisseurs;
    this.litiges = litiges;
    this.clock = clock;
  }

  /**
   * @param id identifiant
   * @return fournisseur
   * @throws ResourceNotFoundException si absent (404 {@code FOURNISSEUR_INTROUVABLE})
   */
  public Fournisseur charger(UUID id) {
    return fournisseurs
        .parId(id)
        .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", id));
  }

  /**
   * @param fournisseur fournisseur
   * @param coordonneesVisibles exposer les coordonnées bancaires
   * @return vue enrichie
   */
  public FournisseurVue vue(Fournisseur fournisseur, boolean coordonneesVisibles) {
    CompteurLitiges compteur =
        litiges
            .compterParFournisseur(List.of(fournisseur.getId()))
            .getOrDefault(fournisseur.getId(), CompteurLitiges.AUCUN);
    return new FournisseurVue(fournisseur, compteur.ouverts(), aujourdhui(), coordonneesVisibles);
  }

  /**
   * @param liste fournisseurs
   * @return vues enrichies (sans coordonnées bancaires), comptage des litiges groupé
   */
  public List<FournisseurVue> vues(List<Fournisseur> liste) {
    Map<UUID, CompteurLitiges> compteurs =
        litiges.compterParFournisseur(liste.stream().map(Fournisseur::getId).toList());
    LocalDate jour = aujourdhui();
    return liste.stream()
        .map(
            f ->
                new FournisseurVue(
                    f,
                    compteurs.getOrDefault(f.getId(), CompteurLitiges.AUCUN).ouverts(),
                    jour,
                    false))
        .toList();
  }

  /**
   * @return date du jour selon l'horloge
   */
  public LocalDate aujourdhui() {
    return LocalDate.now(clock);
  }
}
