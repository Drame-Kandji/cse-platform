package sn.cse.achats.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.BesoinAAcheterRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.application.port.out.FournisseurAnnuaire;
import sn.cse.achats.application.port.out.FournisseurProjectionRepository;
import sn.cse.achats.application.port.out.ReferenceGenerator;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.FournisseurConnu;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Implémentations en mémoire des ports sortants, pour tester les cas d'utilisation. */
public final class FauxPorts {

  /** Horloge déplaçable. */
  public static final class Horloge extends Clock {
    private Instant instant;

    public Horloge(Instant instant) {
      this.instant = instant;
    }

    public void regler(Instant nouvelInstant) {
      this.instant = nouvelInstant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }

  public final Map<UUID, AppelOffre> appelsOffres = new LinkedHashMap<>();
  public final Map<UUID, ContratAchat> contrats = new LinkedHashMap<>();
  public final Map<UUID, Facture> factures = new LinkedHashMap<>();
  public final Map<UUID, BesoinAAcheter> besoins = new LinkedHashMap<>();
  public final Map<UUID, FournisseurConnu> projection = new HashMap<>();
  public final Map<UUID, Instant> projectionMaj = new HashMap<>();
  public final Map<UUID, FournisseurConnu> distants = new HashMap<>();
  public final List<String> evenements = new ArrayList<>();
  private int sequenceAo;
  private int sequenceContrat;

  private static <T> PageResponse<T> page(List<T> tous, PageQuery q) {
    int debut = Math.min(q.page() * q.size(), tous.size());
    int fin = Math.min(debut + q.size(), tous.size());
    return PageResponse.of(tous.subList(debut, fin), q.page(), q.size(), tous.size());
  }

  public AppelOffreRepository appelOffreRepository() {
    return new AppelOffreRepository() {
      @Override
      public AppelOffre enregistrer(AppelOffre appelOffre) {
        appelsOffres.put(appelOffre.getId(), appelOffre);
        return appelOffre;
      }

      @Override
      public Optional<AppelOffre> parId(UUID id) {
        return Optional.ofNullable(appelsOffres.get(id));
      }

      @Override
      public PageResponse<AppelOffre> rechercher(Filtre filtre, PageQuery q) {
        Predicate<AppelOffre> p =
            a ->
                (filtre.statut() == null || a.getStatut() == filtre.statut())
                    && (filtre.type() == null || a.getType() == filtre.type())
                    && (filtre.q() == null || a.getObjet().contains(filtre.q()));
        return page(appelsOffres.values().stream().filter(p).toList(), q);
      }

      @Override
      public List<UUID> idsPubliesEchus(Instant maintenant) {
        return appelsOffres.values().stream()
            .filter(a -> a.estEchu(maintenant))
            .map(AppelOffre::getId)
            .toList();
      }

      @Override
      public PageResponse<AppelOffre> ouvertsAuFournisseur(
          UUID f, Instant maintenant, PageQuery q) {
        return page(
            appelsOffres.values().stream()
                .filter(a -> a.estAnonyme() && a.getDateLimite().isAfter(maintenant))
                .filter(a -> a.estOuvertAuFournisseur(f))
                .toList(),
            q);
      }

      @Override
      public PageResponse<AppelOffre> avecOffreDu(UUID f, PageQuery q) {
        return page(
            appelsOffres.values().stream().filter(a -> a.offreDu(f).isPresent()).toList(), q);
      }

      @Override
      public long compter() {
        return appelsOffres.size();
      }
    };
  }

  public ContratRepository contratRepository() {
    return new ContratRepository() {
      @Override
      public ContratAchat enregistrer(ContratAchat contrat) {
        contrats.put(contrat.getId(), contrat);
        return contrat;
      }

      @Override
      public Optional<ContratAchat> parId(UUID id) {
        return Optional.ofNullable(contrats.get(id));
      }

      @Override
      public Optional<ContratAchat> parAppelOffre(UUID appelOffreId) {
        return contrats.values().stream()
            .filter(c -> c.getAppelOffreId().equals(appelOffreId))
            .findFirst();
      }

      @Override
      public PageResponse<ContratAchat> rechercher(Filtre filtre, PageQuery q) {
        return page(
            contrats.values().stream()
                .filter(c -> filtre.statut() == null || c.getStatut() == filtre.statut())
                .filter(
                    c ->
                        filtre.fournisseurId() == null
                            || c.getFournisseurId().equals(filtre.fournisseurId()))
                .toList(),
            q);
      }

      @Override
      public PageResponse<ContratAchat> avecEngagement(StatutApprobation statut, PageQuery q) {
        return page(
            contrats.values().stream()
                .filter(c -> c.getEngagement().isPresent())
                .filter(c -> statut == null || c.getEngagement().get().statut() == statut)
                .toList(),
            q);
      }

      @Override
      public List<ContratAchat> enExecutionEcheanceDepassee(LocalDate aujourdhui) {
        return contrats.values().stream().filter(c -> c.estEnRetard(aujourdhui)).toList();
      }

      @Override
      public List<ContratAchat> tous() {
        return List.copyOf(contrats.values());
      }
    };
  }

  public FactureRepository factureRepository() {
    return new FactureRepository() {
      @Override
      public Facture enregistrer(Facture facture) {
        factures.put(facture.getId(), facture);
        return facture;
      }

      @Override
      public Optional<Facture> parId(UUID id) {
        return Optional.ofNullable(factures.get(id));
      }

      @Override
      public PageResponse<Facture> rechercher(Filtre filtre, LocalDate aujourdhui, PageQuery q) {
        return page(
            factures.values().stream()
                .filter(f -> filtre.statut() == null || f.getStatut() == filtre.statut())
                .filter(
                    f ->
                        filtre.fournisseurId() == null
                            || f.getFournisseurId().equals(filtre.fournisseurId()))
                .filter(
                    f ->
                        filtre.enRetard() == null || f.estEnRetard(aujourdhui) == filtre.enRetard())
                .toList(),
            q);
      }

      @Override
      public List<Facture> parContrat(UUID contratId) {
        return factures.values().stream().filter(f -> f.getContratId().equals(contratId)).toList();
      }

      @Override
      public boolean existe(UUID fournisseurId, String numero) {
        return factures.values().stream()
            .anyMatch(
                f -> f.getFournisseurId().equals(fournisseurId) && f.getNumero().equals(numero));
      }

      @Override
      public List<Facture> toutes() {
        return List.copyOf(factures.values());
      }
    };
  }

  public BesoinAAcheterRepository besoinRepository() {
    return new BesoinAAcheterRepository() {
      @Override
      public void enregistrer(BesoinAAcheter besoin) {
        besoins.put(besoin.besoinId(), besoin);
      }

      @Override
      public Optional<BesoinAAcheter> parBesoin(UUID besoinId) {
        return Optional.ofNullable(besoins.get(besoinId));
      }

      @Override
      public PageResponse<BesoinAAcheter> lister(PageQuery q) {
        return page(
            besoins.values().stream()
                .sorted(Comparator.comparing(BesoinAAcheter::dateReception).reversed())
                .toList(),
            q);
      }
    };
  }

  public FournisseurProjectionRepository projectionRepository() {
    return new FournisseurProjectionRepository() {
      @Override
      public Optional<FournisseurConnu> parId(UUID id) {
        return Optional.ofNullable(projection.get(id));
      }

      @Override
      public void enregistrerSiPlusRecent(FournisseurConnu f, Instant observeLe) {
        Instant connu = projectionMaj.get(f.id());
        if (connu == null || !connu.isAfter(observeLe)) {
          projection.put(f.id(), f);
          projectionMaj.put(f.id(), observeLe);
        }
      }
    };
  }

  /** Annuaire distant simulé : {@link #distants}. */
  public FournisseurAnnuaire annuaire() {
    return id -> Optional.ofNullable(distants.get(id));
  }

  public ReferenceGenerator references() {
    return new ReferenceGenerator() {
      @Override
      public String prochaineReferenceAppelOffre(int annee) {
        return String.format("AO-%d-%03d", annee, ++sequenceAo);
      }

      @Override
      public String prochainNumeroContrat(int annee) {
        return String.format("CTR-%d-%03d", annee, ++sequenceContrat);
      }
    };
  }

  public AchatsEventPublisher publisher() {
    return new AchatsEventPublisher() {
      @Override
      public void appelOffre(String type, AppelOffre appelOffre) {
        evenements.add(type);
      }

      @Override
      public void contrat(String type, ContratAchat contrat) {
        evenements.add(type);
      }

      @Override
      public void facture(String type, Facture facture) {
        evenements.add(type);
      }
    };
  }
}
