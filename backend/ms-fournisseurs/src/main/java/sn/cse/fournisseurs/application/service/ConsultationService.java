package sn.cse.fournisseurs.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;
import sn.cse.fournisseurs.application.port.in.ConsulterFournisseursUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.port.out.LitigeRepository;
import sn.cse.fournisseurs.application.query.CompteurLitiges;
import sn.cse.fournisseurs.application.query.CriteresFournisseurs;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.application.query.StatistiquesFournisseurs;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.domain.service.ClassementFournisseurs;

/** Lecture du référentiel : recherche, détail, classement et statistiques. */
@Service
@Transactional(readOnly = true)
public class ConsultationService implements ConsulterFournisseursUseCase {

  /** Rôles habilités à voir les coordonnées bancaires (donnée sensible). */
  static final String[] ROLES_COORDONNEES_BANCAIRES = {Roles.RESPONSABLE_ACHATS, Roles.DAF};

  private final FournisseurRepository fournisseurs;
  private final LitigeRepository litiges;
  private final ChargeurFournisseur chargeur;

  /**
   * @param fournisseurs dépôt des fournisseurs
   * @param litiges dépôt des litiges
   * @param chargeur chargement et vues
   */
  public ConsultationService(
      FournisseurRepository fournisseurs, LitigeRepository litiges, ChargeurFournisseur chargeur) {
    this.fournisseurs = fournisseurs;
    this.litiges = litiges;
    this.chargeur = chargeur;
  }

  @Override
  public PageResponse<FournisseurVue> rechercher(CriteresFournisseurs criteres, PageQuery page) {
    PageResponse<Fournisseur> resultat = fournisseurs.rechercher(criteres, page);
    List<FournisseurVue> vues = chargeur.vues(resultat.content());
    return new PageResponse<>(
        vues,
        resultat.page(),
        resultat.size(),
        resultat.totalElements(),
        resultat.totalPages(),
        resultat.first(),
        resultat.last());
  }

  @Override
  public FournisseurVue detail(UUID id, CurrentUser utilisateur) {
    boolean visibles = utilisateur != null && utilisateur.hasAnyRole(ROLES_COORDONNEES_BANCAIRES);
    return chargeur.vue(chargeur.charger(id), visibles);
  }

  @Override
  public List<ClassementFournisseurs.Rang> classement(CategorieMateriel categorie) {
    List<Fournisseur> actifs = fournisseurs.parStatut(StatutFournisseur.ACTIF);
    Map<UUID, CompteurLitiges> compteurs =
        litiges.compterParFournisseur(actifs.stream().map(Fournisseur::getId).toList());
    List<ClassementFournisseurs.Candidat> candidats =
        actifs.stream()
            .map(
                f ->
                    new ClassementFournisseurs.Candidat(
                        f, compteurs.getOrDefault(f.getId(), CompteurLitiges.AUCUN).total()))
            .toList();
    return ClassementFournisseurs.classer(candidats, categorie);
  }

  @Override
  public StatistiquesFournisseurs statistiques() {
    Map<StatutFournisseur, Long> parStatut = fournisseurs.compterParStatut();
    List<Fournisseur> actifs = fournisseurs.parStatut(StatutFournisseur.ACTIF);
    BigDecimal scoreMoyen =
        actifs.isEmpty()
            ? BigDecimal.ZERO.setScale(1)
            : actifs.stream()
                .map(f -> f.getPerformance().scorePerformance())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(actifs.size()), 1, RoundingMode.HALF_UP);
    return new StatistiquesFournisseurs(
        parStatut.values().stream().mapToLong(Long::longValue).sum(),
        parStatut.getOrDefault(StatutFournisseur.ACTIF, 0L),
        parStatut.getOrDefault(StatutFournisseur.EN_ATTENTE_VALIDATION, 0L),
        parStatut.getOrDefault(StatutFournisseur.SUSPENDU, 0L),
        parStatut.getOrDefault(StatutFournisseur.BLACKLISTE, 0L),
        scoreMoyen,
        litiges.compterOuverts(),
        fournisseurs.compterDocumentsExpires(chargeur.aujourdhui()));
  }
}
