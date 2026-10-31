package sn.cse.reporting.infrastructure.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import sn.cse.reporting.application.port.out.ExportateurTableau;
import sn.cse.reporting.domain.model.FormatExport;
import sn.cse.reporting.domain.model.Tableau;
import sn.cse.reporting.infrastructure.export.ContenuRapport.Bloc;
import sn.cse.reporting.infrastructure.export.ContenuRapport.Ligne;
import sn.cse.reporting.infrastructure.export.ContenuRapport.Nature;
import sn.cse.reporting.infrastructure.export.ContenuRapport.Table;
import sn.cse.reporting.infrastructure.export.ContenuRapport.Valeur;

/**
 * Export Excel (Apache POI) : une feuille par section, en-tête rappelant profil et période,
 * en-têtes de colonnes mis en forme, cellules numériques avec formats {@code FCFA} et {@code %}
 * (les valeurs restent calculables dans le classeur).
 */
@Component
public class ExcelExporter implements ExportateurTableau {

  private static final int LARGEUR_LIBELLE = 40 * 256;
  private static final int LARGEUR_VALEUR = 20 * 256;

  @Override
  public FormatExport format() {
    return FormatExport.XLSX;
  }

  @Override
  public byte[] exporter(Tableau tableau) {
    try (XSSFWorkbook classeur = new XSSFWorkbook();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
      Styles styles = new Styles(classeur);
      for (Bloc bloc : ContenuRapport.depuis(tableau).blocs()) {
        feuille(classeur.createSheet(bloc.section().libelle()), bloc, tableau, styles);
      }
      if (classeur.getNumberOfSheets() == 0) {
        classeur.createSheet("Tableau de bord");
      }
      classeur.write(sortie);
      return sortie.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Génération du classeur Excel impossible", e);
    }
  }

  private static void feuille(Sheet feuille, Bloc bloc, Tableau t, Styles s) {
    int r = 0;
    Cell titre = feuille.createRow(r++).createCell(0);
    titre.setCellValue(PdfExporter.ENTREPRISE + " — " + bloc.section().libelle());
    titre.setCellStyle(s.titre);
    feuille
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "Profil : "
                + t.profil().libelle()
                + " — période du "
                + FormatFr.date(t.du())
                + " au "
                + FormatFr.date(t.au())
                + " — généré le "
                + FormatFr.horodatage(t.genereLe()));
    r++;
    Row entete = feuille.createRow(r++);
    texte(entete, 0, "Indicateur", s.entete);
    texte(entete, 1, "Valeur", s.entete);
    for (Ligne l : bloc.indicateurs()) {
      Row row = feuille.createRow(r++);
      texte(row, 0, l.libelle(), s.texte);
      valeur(row, 1, l.valeur(), s);
    }
    int colonnes = 2;
    for (Table table : bloc.tables()) {
      r++;
      Cell st = feuille.createRow(r++).createCell(0);
      st.setCellValue(table.titre());
      st.setCellStyle(s.sousTitre);
      Row e = feuille.createRow(r++);
      for (int c = 0; c < table.colonnes().size(); c++) {
        texte(e, c, table.colonnes().get(c), s.entete);
      }
      for (List<Valeur> ligne : table.lignes()) {
        Row row = feuille.createRow(r++);
        for (int c = 0; c < ligne.size(); c++) {
          valeur(row, c, ligne.get(c), s);
        }
      }
      colonnes = Math.max(colonnes, table.colonnes().size());
    }
    feuille.setColumnWidth(0, LARGEUR_LIBELLE);
    for (int c = 1; c < colonnes; c++) {
      feuille.setColumnWidth(c, LARGEUR_VALEUR);
    }
    feuille.createFreezePane(0, 4);
  }

  private static void texte(Row row, int col, String valeur, CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(valeur);
    cell.setCellStyle(style);
  }

  private static void valeur(Row row, int col, Valeur v, Styles s) {
    Cell cell = row.createCell(col);
    if (v.nature() == Nature.TEXTE) {
      cell.setCellValue(v.texte());
      cell.setCellStyle(s.texte);
    } else if (v.nombre() == null) {
      cell.setCellValue(FormatFr.NON_DEFINI);
      cell.setCellStyle(s.texte);
    } else {
      cell.setCellValue(v.nombre().doubleValue());
      cell.setCellStyle(s.nombres.get(v.nature()));
    }
  }

  /** Styles du classeur (créés une fois : Excel limite le nombre de styles). */
  private static final class Styles {
    final CellStyle titre;
    final CellStyle sousTitre;
    final CellStyle entete;
    final CellStyle texte;
    final Map<Nature, CellStyle> nombres = new EnumMap<>(Nature.class);

    Styles(XSSFWorkbook c) {
      Font gras = c.createFont();
      gras.setBold(true);
      gras.setFontHeightInPoints((short) 14);
      titre = c.createCellStyle();
      titre.setFont(gras);

      Font grasMoyen = c.createFont();
      grasMoyen.setBold(true);
      sousTitre = c.createCellStyle();
      sousTitre.setFont(grasMoyen);

      Font blanc = c.createFont();
      blanc.setBold(true);
      blanc.setColor(IndexedColors.WHITE.getIndex());
      entete = c.createCellStyle();
      entete.setFont(blanc);
      entete.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
      entete.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      bordures(entete);

      texte = c.createCellStyle();
      bordures(texte);

      short fcfa = c.createDataFormat().getFormat("#,##0 \"FCFA\"");
      short pct = c.createDataFormat().getFormat("0.0 \"%\"");
      short entier = c.createDataFormat().getFormat("#,##0");
      short score = c.createDataFormat().getFormat("0.0");
      short jours = c.createDataFormat().getFormat("0.0 \"j\"");
      short heures = c.createDataFormat().getFormat("0.0 \"h\"");
      nombres.put(Nature.MONTANT, numerique(c, fcfa));
      nombres.put(Nature.TAUX, numerique(c, pct));
      nombres.put(Nature.NOMBRE, numerique(c, entier));
      nombres.put(Nature.SCORE, numerique(c, score));
      nombres.put(Nature.JOURS, numerique(c, jours));
      nombres.put(Nature.HEURES, numerique(c, heures));
    }

    private static CellStyle numerique(XSSFWorkbook c, short format) {
      CellStyle style = c.createCellStyle();
      style.setDataFormat(format);
      bordures(style);
      return style;
    }

    private static void bordures(CellStyle style) {
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
    }
  }
}
