package sn.cse.fournisseurs.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.fournisseurs.application.command.LigneCatalogueCommand;
import sn.cse.fournisseurs.application.port.in.GererCatalogueUseCase;
import sn.cse.fournisseurs.application.port.out.CatalogueRepository;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.DonneesLigneCatalogue;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;
import sn.cse.fournisseurs.domain.model.PrixHistorique;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.domain.service.ComparateurPrix;

/** Catalogue de prix : upsert par référence avec historisation, comparaison multi-fournisseurs. */
@Service
@Transactional
public class CatalogueService implements GererCatalogueUseCase {

  private final CatalogueRepository catalogue;
  private final FournisseurRepository fournisseurs;
  private final ChargeurFournisseur chargeur;
  private final Clock clock;

  /**
   * @param catalogue dépôt du catalogue
   * @param fournisseurs dépôt des fournisseurs
   * @param chargeur chargement des fournisseurs
   * @param clock horloge
   */
  public CatalogueService(
      CatalogueRepository catalogue,
      FournisseurRepository fournisseurs,
      ChargeurFournisseur chargeur,
      Clock clock) {
    this.catalogue = catalogue;
    this.fournisseurs = fournisseurs;
    this.chargeur = chargeur;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LigneCatalogue> catalogue(UUID fournisseurId) {
    chargeur.charger(fournisseurId);
    return catalogue.parFournisseur(fournisseurId);
  }

  @Override
  public LigneCatalogue publierLigne(UUID fournisseurId, LigneCatalogueCommand commande) {
    return publier(chargeur.charger(fournisseurId), commande.donnees(), clock.instant());
  }

  /**
   * Upsert d'une ligne à une date donnée (utilisé aussi par le jeu de démonstration).
   *
   * @param fournisseur fournisseur propriétaire
   * @param donnees données de la ligne
   * @param date instant de publication
   * @return ligne persistée
   */
  public LigneCatalogue publier(
      Fournisseur fournisseur, DonneesLigneCatalogue donnees, Instant date) {
    return catalogue
        .parReference(fournisseur.getId(), donnees.reference())
        .map(
            existante -> {
              boolean prixChange = existante.mettreAJour(fournisseur, donnees, date);
              LigneCatalogue persistee = catalogue.sauvegarder(existante);
              if (prixChange) {
                catalogue.historiser(PrixHistorique.depuis(persistee));
              }
              return persistee;
            })
        .orElseGet(
            () -> {
              LigneCatalogue persistee =
                  catalogue.sauvegarder(LigneCatalogue.creer(fournisseur, donnees, date));
              // Le prix initial ouvre l'historique de la référence.
              catalogue.historiser(PrixHistorique.depuis(persistee));
              return persistee;
            });
  }

  @Override
  @Transactional(readOnly = true)
  public List<PrixHistorique> historique(UUID fournisseurId, String reference) {
    chargeur.charger(fournisseurId);
    String ref =
        reference == null || reference.isBlank()
            ? null
            : reference.trim().toUpperCase(java.util.Locale.ROOT);
    return catalogue.historique(fournisseurId, ref);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ComparateurPrix.ComparaisonPrix> comparer(CategorieMateriel categorie, String q) {
    Map<UUID, Fournisseur> actifs =
        fournisseurs.parStatut(StatutFournisseur.ACTIF).stream()
            .collect(Collectors.toMap(Fournisseur::getId, Function.identity()));
    if (actifs.isEmpty()) {
      return List.of();
    }
    String texte = q == null || q.isBlank() ? null : q.trim();
    List<ComparateurPrix.OffreCatalogue> offres =
        catalogue.rechercher(List.copyOf(actifs.keySet()), categorie, texte).stream()
            .map(
                ligne -> {
                  Fournisseur f = actifs.get(ligne.getFournisseurId());
                  return new ComparateurPrix.OffreCatalogue(
                      ligne, f.getProfil().raisonSociale(), f.getPerformance().scorePerformance());
                })
            .toList();
    return ComparateurPrix.comparer(offres);
  }
}
