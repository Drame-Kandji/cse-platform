package sn.cse.auth.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.cse.auth.application.exception.IdentiteIndisponibleException;
import sn.cse.auth.application.port.out.CompensationTransactionnelle;
import sn.cse.auth.application.port.out.CompteIdentite;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.LienActivation;
import sn.cse.auth.application.port.out.NouveauCompte;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.application.service.ActiverCompteFournisseurService;
import sn.cse.auth.application.service.ParametresPortailFournisseur;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.common.messaging.EventEnvelopeReader;
import sn.cse.common.messaging.IdempotentEventHandler;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.FournisseurPayload;

/**
 * Consommateur {@code FOURNISSEUR_VALIDE} branché sur le vrai cas d'utilisation, Keycloak simulé
 * (port Mockito à état) et garde d'idempotence en mémoire reproduisant {@code processed_event}.
 */
class FournisseursEventsListenerTest {

  private static final UUID FOURNISSEUR_ID =
      UUID.fromString("f0000000-0000-0000-0000-000000000007");
  private static final UUID COMPTE = UUID.fromString("00000000-0000-0000-0000-000000000777");
  private static final String EMAIL = "contact@baobab-logistique.sn";

  private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
  private final Map<UUID, Utilisateur> profils = new LinkedHashMap<>();
  private final AtomicReference<CompteIdentite> compteKeycloak = new AtomicReference<>();
  private IdentityProviderPort keycloak;
  private UtilisateurEventPublisher events;
  private FournisseursEventsListener listener;

  /** Reproduit {@code processed_event} : la marque est annulée si le traitement échoue. */
  private static final class IdempotenceEnMemoire extends IdempotentEventHandler {
    private final Set<String> traites = new HashSet<>();

    IdempotenceEnMemoire() {
      super(null, Clock.systemUTC());
    }

    @Override
    public boolean traiterUneFois(UUID eventId, String consommateur, Runnable traitement) {
      String cle = eventId + "/" + consommateur;
      if (!traites.add(cle)) {
        return false;
      }
      try {
        traitement.run();
      } catch (RuntimeException e) {
        traites.remove(cle);
        throw e;
      }
      return true;
    }
  }

  @BeforeEach
  void initialiser() {
    keycloak = mock(IdentityProviderPort.class);
    events = mock(UtilisateurEventPublisher.class);
    when(keycloak.compteParEmail(EMAIL)).thenAnswer(i -> Optional.ofNullable(compteKeycloak.get()));
    when(keycloak.creerCompte(any()))
        .thenAnswer(
            i -> {
              NouveauCompte nouveau = i.getArgument(0);
              compteKeycloak.set(
                  new CompteIdentite(
                      COMPTE,
                      nouveau.identite(),
                      true,
                      Set.of(Role.FOURNISSEUR.code()),
                      nouveau.attributs(),
                      false));
              return COMPTE;
            });
    when(keycloak.motDePasseDefini(COMPTE)).thenReturn(true);
    UtilisateurRepository repository = mock(UtilisateurRepository.class);
    when(repository.parId(any()))
        .thenAnswer(i -> Optional.ofNullable(profils.get(i.getArgument(0))));
    when(repository.enregistrer(any()))
        .thenAnswer(
            i -> {
              Utilisateur u = i.getArgument(0);
              profils.put(u.id(), u);
              return u;
            });
    ActiverCompteFournisseurService service =
        new ActiverCompteFournisseurService(
            repository,
            keycloak,
            events,
            mock(CompensationTransactionnelle.class),
            new ParametresPortailFournisseur(
                "Contact",
                new LienActivation(
                    "cse-portail-fournisseur", "http://localhost:5174/", Duration.ofHours(72))),
            Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC));
    listener =
        new FournisseursEventsListener(
            new EventEnvelopeReader(mapper), new IdempotenceEnMemoire(), service);
  }

  private String message(UUID eventId, String type, String email) {
    FournisseurPayload payload =
        new FournisseurPayload(
            FOURNISSEUR_ID,
            "Baobab Logistique",
            email,
            List.of("CONSOMMABLE"),
            "ACTIF",
            BigDecimal.valueOf(50),
            null);
    EventEnvelope enveloppe =
        new EventEnvelope(
            eventId,
            type,
            EventEnvelope.CURRENT_VERSION,
            Instant.parse("2026-11-01T09:59:00Z"),
            "ms-fournisseurs",
            "Fournisseur",
            FOURNISSEUR_ID.toString(),
            null,
            null,
            mapper.valueToTree(payload));
    try {
      return mapper.writeValueAsString(enveloppe);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void should_creer_un_seul_compte_when_meme_evenement_recu_deux_fois() {
    String message = message(UUID.randomUUID(), EventTypes.FOURNISSEUR_VALIDE, EMAIL);

    listener.recevoir(message);
    listener.recevoir(message);

    verify(keycloak, times(1)).creerCompte(any());
    verify(keycloak, times(1)).envoyerEmailActivation(any(), any());
    verify(events, times(1)).publierCree(any());
    assertThat(profils).containsOnlyKeys(COMPTE);
  }

  @Test
  void should_creer_un_seul_compte_when_validation_rejouee_avec_un_nouvel_evenement() {
    listener.recevoir(message(UUID.randomUUID(), EventTypes.FOURNISSEUR_VALIDE, EMAIL));
    listener.recevoir(message(UUID.randomUUID(), EventTypes.FOURNISSEUR_VALIDE, EMAIL));

    verify(keycloak, times(1)).creerCompte(any());
    verify(events, times(1)).publierCree(any());
    assertThat(profils).hasSize(1);
  }

  @Test
  void should_ignorer_les_autres_evenements_fournisseurs() {
    listener.recevoir(message(UUID.randomUUID(), EventTypes.FOURNISSEUR_INSCRIT, EMAIL));
    listener.recevoir(message(UUID.randomUUID(), EventTypes.FOURNISSEUR_SUSPENDU, EMAIL));
    verifyNoInteractions(keycloak, events);
  }

  @Test
  void should_propager_l_erreur_keycloak_puis_traiter_au_rejeu() {
    doThrow(new IdentiteIndisponibleException("Keycloak injoignable", null))
        .doNothing()
        .when(keycloak)
        .envoyerEmailActivation(any(), any());
    String message = message(UUID.randomUUID(), EventTypes.FOURNISSEUR_VALIDE, EMAIL);

    assertThatThrownBy(() -> listener.recevoir(message))
        .isInstanceOf(IdentiteIndisponibleException.class);
    // Rejeu du même message (DefaultErrorHandler) : la marque d'idempotence a été annulée.
    when(keycloak.motDePasseDefini(COMPTE)).thenReturn(false);
    compteKeycloak.set(null);
    profils.clear();
    listener.recevoir(message);

    verify(keycloak, times(2)).creerCompte(any());
    verify(keycloak, times(2)).envoyerEmailActivation(any(), any());
    assertThat(profils).containsOnlyKeys(COMPTE);
  }

  @Test
  void should_rejeter_sans_rejeu_when_payload_sans_e_mail() {
    String message = message(UUID.randomUUID(), EventTypes.FOURNISSEUR_VALIDE, null);
    assertThatThrownBy(() -> listener.recevoir(message))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(keycloak);
  }
}
