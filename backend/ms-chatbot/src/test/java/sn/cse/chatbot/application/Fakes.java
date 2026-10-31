package sn.cse.chatbot.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import sn.cse.chatbot.application.exception.LlmIndisponibleException;
import sn.cse.chatbot.application.exception.SourceIndisponibleException;
import sn.cse.chatbot.application.port.out.AchatsPort;
import sn.cse.chatbot.application.port.out.BesoinsPort;
import sn.cse.chatbot.application.port.out.ConversationRepository;
import sn.cse.chatbot.application.port.out.CritereMateriel;
import sn.cse.chatbot.application.port.out.FournisseursPort;
import sn.cse.chatbot.application.port.out.LlmPort;
import sn.cse.chatbot.application.port.out.RequeteLlm;
import sn.cse.chatbot.application.port.out.SuiviPort;
import sn.cse.chatbot.domain.Donnees;
import sn.cse.chatbot.domain.model.AffectationInfo;
import sn.cse.chatbot.domain.model.AppelOffreInfo;
import sn.cse.chatbot.domain.model.BesoinInfo;
import sn.cse.chatbot.domain.model.ChantierInfo;
import sn.cse.chatbot.domain.model.ContratInfo;
import sn.cse.chatbot.domain.model.Conversation;
import sn.cse.chatbot.domain.model.ConversationResume;
import sn.cse.chatbot.domain.model.FournisseurInfo;
import sn.cse.chatbot.domain.model.MaintenanceInfo;
import sn.cse.chatbot.domain.model.MaterielInfo;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Ports simulés en mémoire, avec trace des appels. */
final class Fakes {

  private Fakes() {}

  static final class Suivi implements SuiviPort {
    final List<String> appels = new ArrayList<>();
    boolean enPanne;

    private void tracer(String appel) {
      appels.add(appel);
      if (enPanne) {
        throw new SourceIndisponibleException("suivi", new IllegalStateException("panne"));
      }
    }

    @Override
    public List<MaterielInfo> rechercherMateriels(CritereMateriel c, int limite) {
      tracer(
          "materiels q="
              + c.q()
              + " categorie="
              + c.categorie()
              + " etat="
              + c.etat()
              + " chantier="
              + c.chantierId());
      return List.of(Donnees.NIVELEUSE, Donnees.PELLE).stream()
          .filter(m -> c.etat() == null || c.etat().equals(m.etat()))
          .filter(
              m ->
                  c.q() == null
                      || (m.code() + " " + m.designation())
                          .toLowerCase()
                          .contains(c.q().toLowerCase()))
          .toList();
    }

    @Override
    public List<MaintenanceInfo> maintenances(String materielId, int limite) {
      tracer("maintenances " + materielId);
      return List.of(Donnees.VIDANGE);
    }

    @Override
    public List<AffectationInfo> planning(
        String chantierId, LocalDate du, LocalDate au, int limite) {
      tracer("planning " + chantierId + " " + du + " " + au);
      return List.of(Donnees.AFFECTATION);
    }

    @Override
    public List<AffectationInfo> affectationsDuMateriel(String materielId, int limite) {
      tracer("affectations " + materielId);
      return List.of(Donnees.AFFECTATION);
    }

    @Override
    public List<ChantierInfo> rechercherChantiers(String q, int limite) {
      tracer("chantiers " + q);
      return q.toLowerCase().contains("thi") ? List.of(Donnees.THIES) : List.of();
    }
  }

  static final class Besoins implements BesoinsPort {
    final List<String> appels = new ArrayList<>();

    @Override
    public List<BesoinInfo> rechercher(String q, String statut, int limite) {
      appels.add("besoins q=" + q + " statut=" + statut);
      return List.of(Donnees.BESOIN_REJETE);
    }
  }

  static final class Achats implements AchatsPort {
    final List<String> appels = new ArrayList<>();

    @Override
    public List<ContratInfo> rechercherContrats(String q, int limite) {
      appels.add("contrats " + q);
      return List.of(Donnees.CONTRAT);
    }

    @Override
    public List<AppelOffreInfo> rechercherAppelsOffres(String q, int limite) {
      appels.add("ao " + q);
      return List.of(Donnees.AO);
    }
  }

  static final class Fournisseurs implements FournisseursPort {
    final List<String> appels = new ArrayList<>();

    @Override
    public List<FournisseurInfo> classement(String categorie, int limite) {
      appels.add("classement " + categorie);
      return List.of(Donnees.SAHEL);
    }
  }

  static final class Llm implements LlmPort {
    boolean disponible;
    boolean enEchec;
    String reponse = "Réponse du LLM : la niveleuse NIV-002 est à Thiès.";
    RequeteLlm derniere;

    @Override
    public boolean estDisponible() {
      return disponible;
    }

    @Override
    public String generer(RequeteLlm requete) {
      derniere = requete;
      if (enEchec) {
        throw new LlmIndisponibleException("délai dépassé", null);
      }
      return reponse;
    }
  }

  static final class Conversations implements ConversationRepository {
    final Map<UUID, Conversation> stock = new HashMap<>();

    @Override
    public void sauvegarder(Conversation c) {
      stock.put(c.id(), c);
    }

    @Override
    public Optional<Conversation> parId(UUID id) {
      return Optional.ofNullable(stock.get(id));
    }

    @Override
    public PageResponse<ConversationResume> resumes(UUID utilisateurId, PageQuery page) {
      List<ConversationResume> liste =
          stock.values().stream()
              .filter(c -> c.appartientA(utilisateurId))
              .map(c -> c.resume(50))
              .sorted(Comparator.comparing(ConversationResume::updatedAt).reversed())
              .toList();
      return PageResponse.of(liste, page.page(), page.size(), liste.size());
    }

    @Override
    public void supprimer(UUID id) {
      stock.remove(id);
    }
  }
}
