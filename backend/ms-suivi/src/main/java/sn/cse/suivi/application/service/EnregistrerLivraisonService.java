package sn.cse.suivi.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.command.LivraisonRecue;
import sn.cse.suivi.application.config.SuiviParametres;
import sn.cse.suivi.application.port.in.EnregistrerLivraisonUseCase;
import sn.cse.suivi.application.port.out.AffectationRepository;
import sn.cse.suivi.application.port.out.GenerateurSequence;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.MouvementRepository;
import sn.cse.suivi.application.port.out.SuiviEventPublisher;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Mouvement;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.TypeMouvement;
import sn.cse.suivi.domain.service.PolitiqueAffectation;

/**
 * Entrée en parc d'une livraison : un matériel par unité livrée (code généré par catégorie et par
 * année), puis affectation {@code EN_ATTENTE_RECEPTION} au chantier du besoin s'il est actif.
 */
@Service
@Transactional
public class EnregistrerLivraisonService implements EnregistrerLivraisonUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(EnregistrerLivraisonService.class);

  /** Série de numérotation des codes matériel générés. */
  static final String SERIE_MATERIEL = "materiel";

  private final MaterielRepository materiels;
  private final AffectationRepository affectations;
  private final MouvementRepository mouvements;
  private final GenerateurSequence sequences;
  private final ChargeurAgregats chargeur;
  private final SuiviEventPublisher evenements;
  private final SuiviParametres parametres;
  private final Clock clock;

  /**
   * @param materiels stockage des matériels
   * @param affectations stockage des affectations
   * @param mouvements journal des mouvements
   * @param sequences numérotation
   * @param chargeur chargement des agrégats
   * @param evenements publication des événements
   * @param parametres paramètres métier
   * @param clock horloge
   */
  public EnregistrerLivraisonService(
      MaterielRepository materiels,
      AffectationRepository affectations,
      MouvementRepository mouvements,
      GenerateurSequence sequences,
      ChargeurAgregats chargeur,
      SuiviEventPublisher evenements,
      SuiviParametres parametres,
      Clock clock) {
    this.materiels = materiels;
    this.affectations = affectations;
    this.mouvements = mouvements;
    this.sequences = sequences;
    this.chargeur = chargeur;
    this.evenements = evenements;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public List<UUID> enregistrer(LivraisonRecue livraison) {
    if (livraison.quantite() < 1) {
      throw new IllegalArgumentException(
          "Quantité livrée invalide pour le contrat " + livraison.contratId());
    }
    LocalDate aujourdhui = LocalDate.now(clock);
    LocalDate debut = livraison.dateLivraison() == null ? aujourdhui : livraison.dateLivraison();
    Chantier chantier = chantierDestinataire(livraison);
    BigDecimal valeurUnitaire =
        livraison.montant() == null
            ? null
            : livraison
                .montant()
                .divide(BigDecimal.valueOf(livraison.quantite()), 0, RoundingMode.HALF_UP);
    BigDecimal tarif = tarifJournalier(livraison, valeurUnitaire);
    Materiel.Origine origine =
        new Materiel.Origine(livraison.fournisseurId(), livraison.contratId());
    Mouvement.Auteur systeme = Auteurs.de(Auteurs.SYSTEME);
    String reference =
        livraison.numeroContrat() == null
            ? String.valueOf(livraison.contratId())
            : livraison.numeroContrat();

    List<UUID> crees = new ArrayList<>();
    for (int i = 0; i < livraison.quantite(); i++) {
      DescriptionMateriel description =
          new DescriptionMateriel(
              codeGenere(livraison, debut.getYear()),
              null,
              livraison.designation(),
              livraison.categorie(),
              null,
              null,
              null,
              tarif,
              valeurUnitaire,
              debut);
      Materiel materiel =
          materiels.save(Materiel.enregistrer(description, null, 0L, origine, aujourdhui));
      mouvements.ajouter(
          Mouvement.de(
              materiel.getId(),
              TypeMouvement.ENREGISTREMENT,
              null,
              null,
              Instant.now(clock),
              systeme,
              "Livraison du contrat " + reference));
      evenements.materiel(EventTypes.MATERIEL_ENREGISTRE, materiel, null, null);
      if (chantier != null) {
        affecter(materiel, chantier, livraison.besoinId(), debut, systeme);
      }
      crees.add(materiel.getId());
    }
    LOG.info(
        "Livraison {} : {} matériel(s) enregistré(s){}",
        reference,
        crees.size(),
        chantier == null ? " sans affectation" : " et affecté(s) à " + chantier.getCode());
    return crees;
  }

  private void affecter(
      Materiel materiel,
      Chantier chantier,
      UUID besoinId,
      LocalDate debut,
      Mouvement.Auteur auteur) {
    Periode periode =
        new Periode(debut, debut.plusDays(parametres.livraison().dureeAffectationJours() - 1L));
    Affectation affectation =
        affectations.save(
            PolitiqueAffectation.affecter(materiel, chantier, besoinId, periode, List.of()));
    Materiel affecte = materiels.save(materiel);
    mouvements.ajouter(
        Mouvement.de(
            affecte.getId(),
            TypeMouvement.AFFECTATION,
            null,
            chantier,
            Instant.now(clock),
            auteur,
            "Affectation automatique à la livraison"));
    evenements.affectation(EventTypes.MATERIEL_AFFECTE, affectation, affecte, chantier);
  }

  private Chantier chantierDestinataire(LivraisonRecue livraison) {
    Chantier chantier = chargeur.chantierOuNull(livraison.chantierId());
    if (livraison.chantierId() != null && (chantier == null || !chantier.estEnCours())) {
      LOG.warn(
          "Livraison du contrat {} : chantier {} inconnu ou terminé, matériels laissés au parc",
          livraison.contratId(),
          livraison.chantierId());
      return null;
    }
    return chantier;
  }

  private BigDecimal tarifJournalier(LivraisonRecue livraison, BigDecimal valeurUnitaire) {
    if (livraison.tarifJournalier() != null) {
      return livraison.tarifJournalier();
    }
    return valeurUnitaire == null
        ? BigDecimal.ZERO
        : valeurUnitaire
            .multiply(parametres.livraison().ratioTarifJournalier())
            .setScale(0, RoundingMode.HALF_UP);
  }

  private String codeGenere(LivraisonRecue livraison, int annee) {
    String prefixe = parametres.numerotation().prefixesCodeMateriel().get(livraison.categorie());
    if (prefixe == null) {
      throw new IllegalStateException(
          "Préfixe de code non configuré pour la catégorie " + livraison.categorie());
    }
    return String.format(
        "%s-%d-%04d", prefixe, annee, sequences.prochaineValeur(SERIE_MATERIEL, annee));
  }
}
