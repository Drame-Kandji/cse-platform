package sn.cse.reporting.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;
import sn.cse.reporting.api.controller.ReportingController;
import sn.cse.reporting.application.port.out.LectureReportingRepository;
import sn.cse.reporting.application.service.ConsulterSerieService;
import sn.cse.reporting.application.service.ConsulterTableauService;
import sn.cse.reporting.application.service.ExporterTableauService;
import sn.cse.reporting.domain.JeuDeDonnees;
import sn.cse.reporting.domain.model.HistoriqueParc;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.infrastructure.export.ExcelExporter;
import sn.cse.reporting.infrastructure.export.PdfExporter;

/**
 * Sécurité et contrat HTTP : vrais cas d'utilisation et exportateurs, seule la lecture des vues est
 * simulée (jeu {@link JeuDeDonnees}).
 */
@WebMvcTest(ReportingController.class)
@ActiveProfiles("test")
@Import({
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class,
  ConsulterTableauService.class,
  ConsulterSerieService.class,
  ExporterTableauService.class,
  PdfExporter.class,
  ExcelExporter.class,
  ReportingControllerTest.Config.class
})
class ReportingControllerTest {

  private static final UUID ID = UUID.randomUUID();

  @Autowired private MockMvc mvc;

  @MockitoBean private LectureReportingRepository lecture;

  @TestConfiguration(proxyBeanMethods = false)
  static class Config {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    ParametresReporting parametresReporting() {
      return new ParametresReporting(90, 1830, 400, 5, 365);
    }
  }

  @BeforeEach
  void donnees() {
    when(lecture.charger(any(), any())).thenReturn(JeuDeDonnees.donnees());
    when(lecture.instantanes(any())).thenReturn(List.of());
    when(lecture.historiqueParc()).thenReturn(HistoriqueParc.vide());
  }

  @Test
  void should_retourner_401_when_sans_jeton() throws Exception {
    mvc.perform(get("/api/reporting/kpis"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void should_refuser_403_when_role_sans_tableau_de_bord() throws Exception {
    mvc.perform(get("/api/reporting/kpis").with(CseJwt.as(ID, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    mvc.perform(
            get("/api/reporting/export")
                .param("format", "pdf")
                .with(CseJwt.as(ID, Roles.FOURNISSEUR)))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_refuser_403_problem_detail_when_profil_non_autorise() throws Exception {
    mvc.perform(
            get("/api/reporting/kpis")
                .param("profil", "DG")
                .with(CseJwt.as(ID, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("PROFIL_NON_AUTORISE"))
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.traceId").exists());
    mvc.perform(
            get("/api/reporting/export")
                .param("format", "xlsx")
                .param("profil", "DAF")
                .with(CseJwt.as(ID, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PROFIL_NON_AUTORISE"));
  }

  @Test
  void should_retourner_profil_principal_et_ses_seules_sections() throws Exception {
    mvc.perform(get("/api/reporting/kpis").with(CseJwt.as(ID, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profil").value("RESPONSABLE_MATERIEL"))
        .andExpect(jsonPath("$.du").value("2026-08-02"))
        .andExpect(jsonPath("$.au").value("2026-10-30"))
        .andExpect(jsonPath("$.parc.total").value(4))
        .andExpect(jsonPath("$.parc.parCategorie.length()").value(4))
        .andExpect(jsonPath("$.besoins.parPriorite.URGENT").exists())
        .andExpect(jsonPath("$.chantiers").isArray())
        .andExpect(jsonPath("$.achats").doesNotExist())
        .andExpect(jsonPath("$.finances").doesNotExist())
        .andExpect(jsonPath("$.fournisseurs").doesNotExist());
  }

  @Test
  void should_ouvrir_tout_profil_au_DG() throws Exception {
    mvc.perform(
            get("/api/reporting/kpis")
                .param("profil", "RESPONSABLE_ACHATS")
                .param("du", "2026-10-01")
                .param("au", "2026-10-30")
                .with(CseJwt.as(ID, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profil").value("RESPONSABLE_ACHATS"))
        .andExpect(jsonPath("$.achats.delaiMoyenApprovisionnementJours").value(56.5))
        .andExpect(jsonPath("$.fournisseurs.top[0].raisonSociale").value("Sahel Engins"))
        .andExpect(jsonPath("$.parc").doesNotExist());
    mvc.perform(
            get("/api/reporting/kpis")
                .param("du", "2026-10-01")
                .param("au", "2026-10-30")
                .with(CseJwt.as(ID, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profil").value("DG"))
        .andExpect(jsonPath("$.finances.montantEngage").value(5000000))
        .andExpect(jsonPath("$.parc.operationnels").value(2))
        .andExpect(jsonPath("$.parc.tauxDisponibilite").value(66.7));
  }

  @Test
  void should_restreindre_le_chef_et_refuser_un_autre_chantier() throws Exception {
    mvc.perform(
            get("/api/reporting/kpis")
                .param("du", "2026-10-01")
                .with(CseJwt.chef(ID, JeuDeDonnees.C1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profil").value("CHEF_CHANTIER"))
        .andExpect(jsonPath("$.parc.total").value(2))
        .andExpect(jsonPath("$.chantiers.length()").value(1))
        .andExpect(jsonPath("$.chantiers[0].chantierId").value(JeuDeDonnees.C1.toString()))
        .andExpect(jsonPath("$.achats").doesNotExist());
    mvc.perform(
            get("/api/reporting/kpis")
                .param("chantierId", JeuDeDonnees.C2.toString())
                .with(CseJwt.chef(ID, JeuDeDonnees.C1)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CHANTIER_NON_AUTORISE"));
  }

  @Test
  void should_retourner_400_when_parametre_invalide() throws Exception {
    mvc.perform(get("/api/reporting/kpis").param("profil", "ADMIN").with(CseJwt.as(ID, Roles.DAF)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("PROFIL_INCONNU"));
    mvc.perform(
            get("/api/reporting/kpis")
                .param("du", "2026-10-30")
                .param("au", "2026-10-01")
                .with(CseJwt.as(ID, Roles.DAF)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PERIODE_INVALIDE"));
    mvc.perform(get("/api/reporting/series").with(CseJwt.as(ID, Roles.DAF)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/reporting/kpis")
                .param("chantierId", "pas-un-uuid")
                .with(CseJwt.as(ID, Roles.DAF)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_servir_series_selon_profils_autorises() throws Exception {
    mvc.perform(
            get("/api/reporting/series")
                .param("indicateur", "MONTANT_ENGAGE")
                .param("granularite", "MOIS")
                .param("du", "2026-09-01")
                .param("au", "2026-10-30")
                .with(CseJwt.as(ID, Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].periode").value("2026-09"))
        .andExpect(jsonPath("$[1].valeur").value(5000000));
    mvc.perform(
            get("/api/reporting/series")
                .param("indicateur", "TAUX_DISPONIBILITE")
                .with(CseJwt.as(ID, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INDICATEUR_NON_AUTORISE"));
    mvc.perform(
            get("/api/reporting/series")
                .param("indicateur", "BESOINS_SOUMIS")
                .param("granularite", "SEMAINE")
                .param("du", "2026-10-01")
                .param("au", "2026-10-30")
                .with(CseJwt.chef(ID, JeuDeDonnees.C1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].periode").value("2026-W40"));
  }

  @Test
  void should_exporter_pdf_avec_nom_francais_en_piece_jointe() throws Exception {
    byte[] pdf =
        mvc.perform(
                get("/api/reporting/export")
                    .param("format", "pdf")
                    .with(CseJwt.as(ID, Roles.DIRECTION_GENERALE)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        "attachment; filename=\"tableau-de-bord-DG-2026-10-30.pdf\""))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void should_exporter_excel_une_feuille_par_section() throws Exception {
    byte[] xlsx =
        mvc.perform(
                get("/api/reporting/export")
                    .param("format", "XLSX")
                    .param("profil", "DAF")
                    .with(CseJwt.as(ID, Roles.DAF)))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        "attachment; filename=\"tableau-de-bord-DAF-2026-10-30.xlsx\""))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      assertThat(classeur.getNumberOfSheets()).isEqualTo(3);
      assertThat(classeur.getSheetName(0)).isEqualTo("Achats");
      assertThat(classeur.getSheetName(1)).isEqualTo("Finances");
      assertThat(classeur.getSheetName(2)).isEqualTo("Chantiers");
      assertThat(
              classeur
                  .getSheet("Finances")
                  .getRow(5)
                  .getCell(1)
                  .getCellStyle()
                  .getDataFormatString())
          .contains("FCFA");
    }
    mvc.perform(get("/api/reporting/export").param("format", "csv").with(CseJwt.as(ID, Roles.DAF)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("FORMAT_INCONNU"));
  }
}
