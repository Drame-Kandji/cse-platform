package sn.cse.fournisseurs.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.fournisseurs.application.port.out.CatalogueRepository;
import sn.cse.fournisseurs.application.port.out.EvaluationRepository;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.port.out.GenerateurReference;
import sn.cse.fournisseurs.application.port.out.LitigeRepository;
import sn.cse.fournisseurs.application.query.CompteurLitiges;
import sn.cse.fournisseurs.application.query.CriteresFournisseurs;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.Evaluation;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.PrixHistorique;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.domain.model.StatutLitige;

/** Adaptateurs en mémoire des ports sortants, pour les tests de la couche application. */
public final class Memoire {

  private Memoire() {}

  /** Fournisseurs en mémoire ; la sauvegarde incrémente la version comme JPA. */
  public static class Fournisseurs implements FournisseurRepository {
    public final Map<UUID, Fournisseur> donnees = new LinkedHashMap<>();

    @Override
    public Optional<Fournisseur> parId(UUID id) {
      return Optional.ofNullable(donnees.get(id)).map(Fournisseurs::copie);
    }

    @Override
    public Fournisseur sauvegarder(Fournisseur f) {
      long version = donnees.containsKey(f.getId()) ? f.getVersion() + 1 : 0;
      Fournisseur persiste = avecVersion(f, version);
      donnees.put(f.getId(), persiste);
      return copie(persiste);
    }

    @Override
    public boolean nineaUtilise(String ninea, UUID exclu) {
      return donnees.values().stream()
          .anyMatch(f -> f.getProfil().ninea().equals(ninea) && !f.getId().equals(exclu));
    }

    @Override
    public boolean emailUtilise(String email) {
      return donnees.values().stream().anyMatch(f -> f.getProfil().email().equalsIgnoreCase(email));
    }

    @Override
    public PageResponse<Fournisseur> rechercher(CriteresFournisseurs c, PageQuery page) {
      List<Fournisseur> tous =
          donnees.values().stream()
              .filter(f -> c.statut() == null || f.getStatut() == c.statut())
              .filter(f -> c.specialite() == null || f.getProfil().couvre(c.specialite()))
              .filter(
                  f ->
                      c.q() == null
                          || f.getProfil()
                              .raisonSociale()
                              .toLowerCase(Locale.ROOT)
                              .contains(c.q().toLowerCase(Locale.ROOT)))
              .toList();
      int debut = Math.min(page.page() * page.size(), tous.size());
      int fin = Math.min(debut + page.size(), tous.size());
      return PageResponse.of(tous.subList(debut, fin), page.page(), page.size(), tous.size());
    }

    @Override
    public List<Fournisseur> parStatut(StatutFournisseur statut) {
      return donnees.values().stream()
          .filter(f -> f.getStatut() == statut)
          .map(Fournisseurs::copie)
          .toList();
    }

    @Override
    public List<Fournisseur> parIds(Collection<UUID> ids) {
      return ids.stream().map(donnees::get).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public List<Fournisseur> avecDocumentsExpiresNonSignales(LocalDate aujourdhui) {
      return donnees.values().stream()
          .filter(f -> f.getDocuments().stream().anyMatch(d -> d.expirationANotifier(aujourdhui)))
          .map(Fournisseurs::copie)
          .toList();
    }

    @Override
    public Map<StatutFournisseur, Long> compterParStatut() {
      Map<StatutFournisseur, Long> m = new EnumMap<>(StatutFournisseur.class);
      donnees.values().forEach(f -> m.merge(f.getStatut(), 1L, Long::sum));
      return m;
    }

    @Override
    public long compterDocumentsExpires(LocalDate aujourdhui) {
      return donnees.values().stream().mapToLong(f -> f.nombreDocumentsExpires(aujourdhui)).sum();
    }

    private static Fournisseur copie(Fournisseur f) {
      return avecVersion(f, f.getVersion());
    }

    private static Fournisseur avecVersion(Fournisseur f, long version) {
      return Fournisseur.reconstituer(
          new Fournisseur.EtatFournisseur(
              f.getId(),
              f.getProfil(),
              f.getCoordonneesBancaires().orElse(null),
              f.getStatut(),
              f.getMotifStatut().orElse(null),
              f.getDemandeBlacklistage().orElse(null),
              f.getPerformance(),
              f.getDocuments().stream()
                  .map(
                      d ->
                          sn.cse.fournisseurs.domain.model.DocumentAdministratif.reconstituer(
                              d.getId(),
                              d.getType(),
                              d.getLibelle(),
                              d.getDateExpiration().orElse(null),
                              d.isExpirationNotifiee()))
                  .toList(),
              f.getDateInscription(),
              version));
    }
  }

  /** Évaluations en mémoire. */
  public static class Evaluations implements EvaluationRepository {
    public final List<Evaluation> donnees = new ArrayList<>();

    @Override
    public Evaluation enregistrer(Evaluation evaluation) {
      donnees.add(evaluation);
      return evaluation;
    }

    @Override
    public List<Evaluation> parFournisseur(UUID fournisseurId) {
      return donnees.stream()
          .filter(e -> e.fournisseurId().equals(fournisseurId))
          .sorted(Comparator.comparing(Evaluation::date).reversed())
          .toList();
    }
  }

  /** Litiges en mémoire. */
  public static class Litiges implements LitigeRepository {
    public final Map<UUID, Litige> donnees = new LinkedHashMap<>();

    @Override
    public Litige sauvegarder(Litige litige) {
      donnees.put(litige.getId(), litige);
      return litige;
    }

    @Override
    public Optional<Litige> parId(UUID id) {
      return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public PageResponse<Litige> rechercher(
        StatutLitige statut, UUID fournisseurId, PageQuery page) {
      List<Litige> tous =
          donnees.values().stream()
              .filter(l -> statut == null || l.getStatut() == statut)
              .filter(l -> fournisseurId == null || l.getFournisseurId().equals(fournisseurId))
              .toList();
      return PageResponse.of(tous, page.page(), page.size(), tous.size());
    }

    @Override
    public Map<UUID, CompteurLitiges> compterParFournisseur(Collection<UUID> ids) {
      Map<UUID, CompteurLitiges> m = new HashMap<>();
      for (Litige l : donnees.values()) {
        if (!ids.contains(l.getFournisseurId())) {
          continue;
        }
        CompteurLitiges c = m.getOrDefault(l.getFournisseurId(), CompteurLitiges.AUCUN);
        m.put(
            l.getFournisseurId(),
            switch (l.getStatut()) {
              case OUVERT, EN_COURS ->
                  new CompteurLitiges(c.ouverts() + 1, c.resolus(), c.classes());
              case RESOLU -> new CompteurLitiges(c.ouverts(), c.resolus() + 1, c.classes());
              case CLASSE -> new CompteurLitiges(c.ouverts(), c.resolus(), c.classes() + 1);
            });
      }
      return m;
    }

    @Override
    public long compterOuverts() {
      return donnees.values().stream().filter(l -> l.getStatut().estOuvert()).count();
    }
  }

  /** Catalogue en mémoire. */
  public static class Catalogue implements CatalogueRepository {
    public final Map<UUID, LigneCatalogue> lignes = new LinkedHashMap<>();
    public final List<PrixHistorique> historique = new ArrayList<>();

    @Override
    public List<LigneCatalogue> parFournisseur(UUID fournisseurId) {
      return lignes.values().stream()
          .filter(l -> l.getFournisseurId().equals(fournisseurId))
          .toList();
    }

    @Override
    public Optional<LigneCatalogue> parReference(UUID fournisseurId, String reference) {
      return parFournisseur(fournisseurId).stream()
          .filter(l -> l.getReference().equals(reference))
          .findFirst();
    }

    @Override
    public LigneCatalogue sauvegarder(LigneCatalogue ligne) {
      lignes.put(ligne.getId(), ligne);
      return ligne;
    }

    @Override
    public void historiser(PrixHistorique prix) {
      historique.add(prix);
    }

    @Override
    public List<PrixHistorique> historique(UUID fournisseurId, String reference) {
      return historique.stream()
          .filter(h -> h.fournisseurId().equals(fournisseurId))
          .filter(h -> reference == null || h.reference().equals(reference))
          .sorted(Comparator.comparing(PrixHistorique::date).reversed())
          .toList();
    }

    @Override
    public List<LigneCatalogue> rechercher(List<UUID> ids, CategorieMateriel categorie, String q) {
      return lignes.values().stream()
          .filter(l -> ids.contains(l.getFournisseurId()))
          .filter(l -> categorie == null || l.getCategorie() == categorie)
          .filter(
              l ->
                  q == null
                      || l.getDesignation()
                          .toLowerCase(Locale.ROOT)
                          .contains(q.toLowerCase(Locale.ROOT)))
          .toList();
    }
  }

  /** Événement publié capturé. */
  public record Evenement(String type, UUID aggregateId, String statut, String motif) {}

  /** Publication capturée. */
  public static class Publications implements FournisseurEventPublisher {
    public final List<Evenement> evenements = new ArrayList<>();

    @Override
    public void publierFournisseur(String type, Fournisseur f, String motif) {
      evenements.add(new Evenement(type, f.getId(), f.getStatut().name(), motif));
    }

    @Override
    public void publierLitige(String type, Litige l) {
      evenements.add(new Evenement(type, l.getId(), l.getStatut().name(), l.getReference()));
    }

    public List<String> types() {
      return evenements.stream().map(Evenement::type).toList();
    }
  }

  /** Générateur de références séquentiel. */
  public static class References implements GenerateurReference {
    private final AtomicInteger compteur = new AtomicInteger();

    @Override
    public String prochaineReferenceLitige(int annee) {
      return String.format("LIT-%d-%03d", annee, compteur.incrementAndGet());
    }
  }
}
