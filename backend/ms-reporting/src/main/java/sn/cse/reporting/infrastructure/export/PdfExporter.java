package sn.cse.reporting.infrastructure.export;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
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
 * Export PDF (OpenPDF) : page de titre CSE (profil, période, date de génération) puis une section
 * par page avec ses indicateurs et tableaux ; montants et taux au format français ; numéro de page
 * en pied de page.
 */
@Component
public class PdfExporter implements ExportateurTableau {

  /** Raison sociale affichée en titre. */
  static final String ENTREPRISE = "Compagnie Sahélienne d'Entreprises (CSE)";

  private static final Color BLEU = new Color(0x1F, 0x3A, 0x5F);
  private static final Color GRIS = new Color(0xEE, 0xF1, 0xF5);
  private static final Font TITRE = new Font(Font.HELVETICA, 22, Font.BOLD, BLEU);
  private static final Font SOUS_TITRE = new Font(Font.HELVETICA, 14, Font.NORMAL, Color.DARK_GRAY);
  private static final Font SECTION = new Font(Font.HELVETICA, 16, Font.BOLD, BLEU);
  private static final Font SOUS_SECTION = new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY);
  private static final Font ENTETE = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
  private static final Font CELLULE = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
  private static final Font PIED = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);

  @Override
  public FormatExport format() {
    return FormatExport.PDF;
  }

  @Override
  public byte[] exporter(Tableau tableau) {
    ByteArrayOutputStream sortie = new ByteArrayOutputStream();
    Document doc = new Document(PageSize.A4.rotate(), 36, 36, 42, 42);
    try {
      PdfWriter writer = PdfWriter.getInstance(doc, sortie);
      writer.setPageEvent(new PiedDePage());
      doc.addTitle("Tableau de bord · " + tableau.profil().libelle());
      doc.addAuthor(ENTREPRISE);
      doc.addCreator("ms-reporting");
      doc.open();
      pageDeTitre(doc, tableau);
      for (Bloc bloc : ContenuRapport.depuis(tableau).blocs()) {
        doc.newPage();
        section(doc, bloc);
      }
    } catch (DocumentException e) {
      throw new IllegalStateException("Génération du PDF impossible", e);
    } finally {
      doc.close();
    }
    return sortie.toByteArray();
  }

  private static void pageDeTitre(Document doc, Tableau t) throws DocumentException {
    Paragraph espace = new Paragraph(" ");
    espace.setSpacingBefore(120);
    doc.add(espace);
    Paragraph entreprise = new Paragraph(ENTREPRISE, TITRE);
    entreprise.setAlignment(Element.ALIGN_CENTER);
    doc.add(entreprise);
    Paragraph titre = new Paragraph("Tableau de bord · " + t.profil().libelle(), SOUS_TITRE);
    titre.setAlignment(Element.ALIGN_CENTER);
    titre.setSpacingBefore(18);
    doc.add(titre);
    Paragraph periode =
        new Paragraph(
            "Période du " + FormatFr.date(t.du()) + " au " + FormatFr.date(t.au()), SOUS_TITRE);
    periode.setAlignment(Element.ALIGN_CENTER);
    periode.setSpacingBefore(8);
    doc.add(periode);
    Paragraph genere = new Paragraph("Généré le " + FormatFr.horodatage(t.genereLe()), PIED);
    genere.setAlignment(Element.ALIGN_CENTER);
    genere.setSpacingBefore(30);
    doc.add(genere);
  }

  private static void section(Document doc, Bloc bloc) throws DocumentException {
    Paragraph titre = new Paragraph(bloc.section().libelle(), SECTION);
    titre.setSpacingAfter(10);
    doc.add(titre);
    PdfPTable indicateurs = new PdfPTable(new float[] {3f, 2f});
    indicateurs.setWidthPercentage(60);
    indicateurs.setHorizontalAlignment(Element.ALIGN_LEFT);
    entete(indicateurs, List.of("Indicateur", "Valeur"));
    boolean pair = false;
    for (Ligne l : bloc.indicateurs()) {
      cellule(indicateurs, Valeur.texte(l.libelle()), pair);
      cellule(indicateurs, l.valeur(), pair);
      pair = !pair;
    }
    doc.add(indicateurs);
    for (Table table : bloc.tables()) {
      Paragraph sous = new Paragraph(table.titre(), SOUS_SECTION);
      sous.setSpacingBefore(14);
      sous.setSpacingAfter(6);
      doc.add(sous);
      PdfPTable pdf = new PdfPTable(table.colonnes().size());
      pdf.setWidthPercentage(100);
      pdf.setHeaderRows(1);
      entete(pdf, table.colonnes());
      boolean p = false;
      for (List<Valeur> ligne : table.lignes()) {
        for (Valeur v : ligne) {
          cellule(pdf, v, p);
        }
        p = !p;
      }
      if (table.lignes().isEmpty()) {
        PdfPCell vide = new PdfPCell(new Phrase("Aucune donnée sur la période", CELLULE));
        vide.setColspan(table.colonnes().size());
        pdf.addCell(vide);
      }
      doc.add(pdf);
    }
  }

  private static void entete(PdfPTable table, List<String> colonnes) {
    for (String c : colonnes) {
      PdfPCell cell = new PdfPCell(new Phrase(c, ENTETE));
      cell.setBackgroundColor(BLEU);
      cell.setPadding(5);
      table.addCell(cell);
    }
  }

  private static void cellule(PdfPTable table, Valeur v, boolean pair) {
    PdfPCell cell = new PdfPCell(new Phrase(v.formatee(), CELLULE));
    cell.setPadding(4);
    cell.setHorizontalAlignment(
        v.nature() == Nature.TEXTE ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
    if (pair) {
      cell.setBackgroundColor(GRIS);
    }
    table.addCell(cell);
  }

  /** Pied de page : raison sociale et numéro de page. */
  private static final class PiedDePage extends PdfPageEventHelper {
    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      Rectangle page = document.getPageSize();
      ColumnText.showTextAligned(
          writer.getDirectContent(),
          Element.ALIGN_CENTER,
          new Phrase(new Chunk(ENTREPRISE + " · page " + writer.getPageNumber(), PIED)),
          (page.getLeft() + page.getRight()) / 2,
          page.getBottom() + 20,
          0);
    }
  }
}
