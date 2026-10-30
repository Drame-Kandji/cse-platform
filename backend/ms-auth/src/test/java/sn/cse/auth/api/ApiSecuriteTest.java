package sn.cse.auth.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static sn.cse.auth.domain.Fixtures.CHANTIER_1;
import static sn.cse.auth.domain.Fixtures.ID;
import static sn.cse.auth.domain.Fixtures.utilisateur;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.auth.api.controller.AdminRoleController;
import sn.cse.auth.api.controller.AdminUtilisateurController;
import sn.cse.auth.api.controller.MonProfilController;
import sn.cse.auth.api.mapper.UtilisateurApiMapper;
import sn.cse.auth.application.command.ChangerStatutCommand;
import sn.cse.auth.application.command.ReinitialiserMotDePasseCommand;
import sn.cse.auth.application.exception.IdentiteIndisponibleException;
import sn.cse.auth.application.port.in.ChangerStatutUtilisateurUseCase;
import sn.cse.auth.application.port.in.ConsulterMonProfilUseCase;
import sn.cse.auth.application.port.in.CreerUtilisateurUseCase;
import sn.cse.auth.application.port.in.ListerRolesUseCase;
import sn.cse.auth.application.port.in.ModifierUtilisateurUseCase;
import sn.cse.auth.application.port.in.RechercherUtilisateursUseCase;
import sn.cse.auth.application.port.in.ReinitialiserMotDePasseUseCase;
import sn.cse.auth.application.query.RoleResume;
import sn.cse.auth.domain.exception.EmailDejaUtiliseException;
import sn.cse.auth.domain.model.Role;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;

@WebMvcTest(
    controllers = {
      MonProfilController.class,
      AdminUtilisateurController.class,
      AdminRoleController.class
    })
@ImportAutoConfiguration({CseSecurityAutoConfiguration.class, CseWebAutoConfiguration.class})
@Import(UtilisateurApiMapper.class)
class ApiSecuriteTest {

  private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000601");
  private static final String CORPS_CREATION =
      """
      {"email":"awa.diallo@cse.sn","prenom":"Awa","nom":"Diallo","role":"CHEF_CHANTIER",
       "motDePasseTemporaire":"Temp@2026!","chantierIds":["c0000000-0000-0000-0000-000000000001"]}
      """;

  @Autowired private MockMvc mvc;

  @MockitoBean private ConsulterMonProfilUseCase consulter;
  @MockitoBean private RechercherUtilisateursUseCase rechercher;
  @MockitoBean private CreerUtilisateurUseCase creer;
  @MockitoBean private ModifierUtilisateurUseCase modifier;
  @MockitoBean private ChangerStatutUtilisateurUseCase changerStatut;
  @MockitoBean private ReinitialiserMotDePasseUseCase reinitialiser;
  @MockitoBean private ListerRolesUseCase roles;

  @Test
  void should_repondre_401_problem_detail_when_sans_jeton() throws Exception {
    mvc.perform(get("/api/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    mvc.perform(get("/api/admin/utilisateurs")).andExpect(status().isUnauthorized());
  }

  @Test
  void should_retourner_mon_profil_when_utilisateur_authentifie() throws Exception {
    when(consulter.consulter(any())).thenReturn(utilisateur(Role.CHEF_CHANTIER));
    mvc.perform(
            get("/api/me")
                .with(
                    CseJwt.avecClaims(
                        ID,
                        Map.of(
                            "given_name", "Mamadou",
                            "family_name", "Diop",
                            "chantier_ids", List.of(CHANTIER_1.toString())),
                        Roles.CHEF_CHANTIER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID.toString()))
        .andExpect(jsonPath("$.role").value("CHEF_CHANTIER"))
        .andExpect(jsonPath("$.chantierIds.length()").value(2))
        .andExpect(jsonPath("$.otpConfigure").value(false))
        .andExpect(jsonPath("$.seuilDelegation").doesNotExist());
  }

  @Test
  void should_repondre_403_when_role_autre_que_admin() throws Exception {
    mvc.perform(get("/api/admin/utilisateurs").with(CseJwt.as(ID, Roles.DAF)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    mvc.perform(get("/api/admin/roles").with(CseJwt.as(ID, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/utilisateurs")
                .with(CseJwt.as(ID, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_CREATION))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_lister_les_utilisateurs_when_admin() throws Exception {
    when(rechercher.rechercher(any(), any()))
        .thenReturn(PageResponse.of(List.of(utilisateur(Role.DAF)), 0, 20, 1));
    mvc.perform(
            get("/api/admin/utilisateurs?role=DAF&actif=true&q=ba&size=500")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].role").value("DAF"))
        .andExpect(jsonPath("$.content[0].budgetGere").value(5000000000L))
        .andExpect(jsonPath("$.content[0].chantierIds").doesNotExist())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void should_refuser_un_tri_non_autorise_when_liste() throws Exception {
    mvc.perform(
            get("/api/admin/utilisateurs?sort=telephone,asc")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TRI_NON_AUTORISE"));
  }

  @Test
  void should_creer_et_retourner_201_when_requete_valide() throws Exception {
    when(creer.creer(any())).thenReturn(utilisateur(Role.CHEF_CHANTIER));
    mvc.perform(
            post("/api/admin/utilisateurs")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_CREATION))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/admin/utilisateurs/" + ID)))
        .andExpect(jsonPath("$.email").value("chef.chantier@cse.sn"));
  }

  @Test
  void should_repondre_400_when_corps_invalide() throws Exception {
    mvc.perform(
            post("/api/admin/utilisateurs")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"pas-un-email\",\"prenom\":\"\",\"role\":\"DAF\",\"motDePasseTemporaire\":\"court\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION"))
        .andExpect(jsonPath("$.errors.length()").value(4));
    mvc.perform(
            post("/api/admin/utilisateurs")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_CREATION.replace("CHEF_CHANTIER", "INCONNU")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_repondre_409_when_email_deja_utilise() throws Exception {
    when(creer.creer(any())).thenThrow(new EmailDejaUtiliseException("awa.diallo@cse.sn"));
    mvc.perform(
            post("/api/admin/utilisateurs")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_CREATION))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_DEJA_UTILISE"));
  }

  @Test
  void should_repondre_503_when_keycloak_indisponible() throws Exception {
    when(rechercher.parId(ID)).thenThrow(new IdentiteIndisponibleException("injoignable", null));
    mvc.perform(get("/api/admin/utilisateurs/" + ID).with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("IDENTITE_INDISPONIBLE"))
        .andExpect(jsonPath("$.detail").value(containsString("indisponible")));
  }

  @Test
  void should_modifier_statut_et_mot_de_passe_when_admin() throws Exception {
    when(modifier.modifier(any())).thenReturn(utilisateur(Role.RESPONSABLE_MATERIEL));
    mvc.perform(
            put("/api/admin/utilisateurs/" + ID)
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"a@cse.sn\",\"prenom\":\"A\",\"nom\":\"B\",\"role\":\"RESPONSABLE_MATERIEL\",\"perimetreParc\":\"NATIONAL\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perimetreParc").value("NATIONAL"));

    when(changerStatut.changerStatut(any())).thenReturn(utilisateur(Role.DAF));
    mvc.perform(
            patch("/api/admin/utilisateurs/" + ID + "/statut")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actif\":false}"))
        .andExpect(status().isOk());
    verify(changerStatut).changerStatut(new ChangerStatutCommand(ID, false, ADMIN));

    mvc.perform(
            post("/api/admin/utilisateurs/" + ID + "/reinitialiser-mot-de-passe")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motDePasseTemporaire\":\"Nouveau@2026\"}"))
        .andExpect(status().isNoContent());
    verify(reinitialiser).reinitialiser(new ReinitialiserMotDePasseCommand(ID, "Nouveau@2026"));
  }

  @Test
  void should_lister_les_roles_avec_libelles_when_admin() throws Exception {
    when(roles.lister()).thenReturn(List.of(new RoleResume(Role.DAF, 1, true)));
    mvc.perform(get("/api/admin/roles").with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("DAF"))
        .andExpect(jsonPath("$[0].libelle").value("Direction Administrative et Financière"))
        .andExpect(jsonPath("$[0].nombreUtilisateurs").value(1))
        .andExpect(jsonPath("$[0].otpObligatoire").value(true));
  }
}
