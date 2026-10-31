package sn.cse.reporting.application.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import sn.cse.reporting.application.command.DemandeTableau;
import sn.cse.reporting.application.command.FichierExporte;
import sn.cse.reporting.application.port.in.ConsulterTableauUseCase;
import sn.cse.reporting.application.port.in.ExporterTableauUseCase;
import sn.cse.reporting.application.port.out.ExportateurTableau;
import sn.cse.reporting.domain.model.FormatExport;
import sn.cse.reporting.domain.model.Tableau;

/**
 * Export du tableau de bord : mêmes règles d'accès et mêmes chiffres que {@code /kpis}, mis en
 * forme par l'exportateur du format demandé.
 */
@Service
public class ExporterTableauService implements ExporterTableauUseCase {

  /** Préfixe du nom de fichier proposé au téléchargement. */
  static final String PREFIXE_FICHIER = "tableau-de-bord-";

  private final ConsulterTableauUseCase consulter;
  private final Map<FormatExport, ExportateurTableau> exportateurs =
      new EnumMap<>(FormatExport.class);

  /**
   * @param consulter calcul du tableau
   * @param exportateurs exportateurs disponibles (un par format)
   */
  public ExporterTableauService(
      ConsulterTableauUseCase consulter, List<ExportateurTableau> exportateurs) {
    this.consulter = consulter;
    exportateurs.forEach(e -> this.exportateurs.put(e.format(), e));
  }

  @Override
  public FichierExporte exporter(DemandeTableau demande, String format) {
    FormatExport f = FormatExport.depuis(format);
    ExportateurTableau exportateur = exportateurs.get(f);
    if (exportateur == null) {
      throw new IllegalStateException("Aucun exportateur configuré pour le format " + f);
    }
    Tableau tableau = consulter.tableau(demande);
    LocalDate jour = LocalDate.ofInstant(tableau.genereLe(), ZoneOffset.UTC);
    String nom = PREFIXE_FICHIER + tableau.profil().name() + "-" + jour + "." + f.extension();
    return new FichierExporte(nom, f.typeMime(), exportateur.exporter(tableau));
  }
}
