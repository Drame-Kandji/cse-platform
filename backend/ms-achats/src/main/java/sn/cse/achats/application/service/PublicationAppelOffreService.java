package sn.cse.achats.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.CreerAppelOffreCommand;
import sn.cse.achats.application.port.in.PublierAppelOffreUseCase;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.BesoinAAcheterRepository;
import sn.cse.achats.application.port.out.FournisseurAnnuaire;
import sn.cse.achats.application.port.out.ReferenceGenerator;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.model.FournisseurConnu;
import sn.cse.achats.domain.model.NouvelAppelOffre;
import sn.cse.achats.domain.model.PonderationCriteres;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.events.EventTypes;

/** Publication d'un appel d'offres : vérification des invités (R14), rattachement du besoin. */
@Service
@Transactional
public class PublicationAppelOffreService implements PublierAppelOffreUseCase {

  private final AppelOffreRepository appelsOffres;
  private final BesoinAAcheterRepository besoins;
  private final FournisseurAnnuaire annuaire;
  private final ReferenceGenerator references;
  private final AchatsEventPublisher evenements;
  private final Clock clock;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param besoins projection des besoins à acheter
   * @param annuaire vérification des fournisseurs
   * @param references générateur de références
   * @param evenements publication d'événements
   * @param clock horloge
   */
  public PublicationAppelOffreService(
      AppelOffreRepository appelsOffres,
      BesoinAAcheterRepository besoins,
      FournisseurAnnuaire annuaire,
      ReferenceGenerator references,
      AchatsEventPublisher evenements,
      Clock clock) {
    this.appelsOffres = appelsOffres;
    this.besoins = besoins;
    this.annuaire = annuaire;
    this.references = references;
    this.evenements = evenements;
    this.clock = clock;
  }

  @Override
  public AppelOffre publier(CreerAppelOffreCommand commande) {
    Instant maintenant = Instant.now(clock);
    BesoinAAcheter besoin = null;
    if (commande.besoinId() != null) {
      besoin =
          besoins
              .parBesoin(commande.besoinId())
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          CodesErreur.BESOIN_NON_EN_ACHAT,
                          "Le besoin "
                              + commande.besoinId()
                              + " n'a pas été transmis aux achats."));
      // Vérifié avant la création pour échouer sans consommer de référence.
      besoin.rattacherA(UUID.randomUUID());
    }
    List<FournisseurConnu> invites = new ArrayList<>();
    for (UUID id : commande.fournisseurIdsInvites().stream().distinct().toList()) {
      invites.add(
          annuaire
              .verifier(id)
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          CodesErreur.FOURNISSEUR_INCONNU, "Fournisseur inconnu : " + id + ".")));
    }
    NouvelAppelOffre donnees =
        new NouvelAppelOffre(
            commande.objet(),
            commande.description(),
            commande.type(),
            besoin == null ? null : besoin.commeOrigine(),
            commande.categorie(),
            commande.quantite(),
            commande.specifications(),
            commande.budgetEstime(),
            commande.dateLimite(),
            new PonderationCriteres(commande.criteres()),
            invites,
            Acteurs.de(commande.auteur()));
    int annee = maintenant.atZone(ZoneOffset.UTC).getYear();
    AppelOffre appelOffre =
        appelsOffres.enregistrer(
            AppelOffre.publier(
                donnees, () -> references.prochaineReferenceAppelOffre(annee), maintenant));
    if (besoin != null) {
      besoins.enregistrer(besoin.rattacherA(appelOffre.getId()));
    }
    evenements.appelOffre(EventTypes.AO_PUBLIE, appelOffre);
    return appelOffre;
  }
}
