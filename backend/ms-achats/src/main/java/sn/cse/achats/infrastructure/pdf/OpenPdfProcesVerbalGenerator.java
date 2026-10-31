package sn.cse.achats.infrastructure.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import sn.cse.achats.application.port.out.ProcesVerbalGenerator;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.Approbation;
import sn.cse.achats.domain.model.Attribution;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.FournisseurInvite;
import sn.cse.achats.domain.model.Offre;
import sn.cse.achats.domain.model.TypeProcesVerbal;
import sn.cse.achats.domain.service.OffreEvaluee;

/**
 * Procès-verbaux d'appel d'offres générés avec OpenPDF : en-tête CSE, informations de la
 * consultation, tableau des offres (scores et rangs pour l'évaluation), décision et bloc de
 * signatures.
 */
@Component
public class OpenPdfProcesVerbalGenerator implements ProcesVerbalGenerator {

  private static final Color BLEU_CSE = new Color(0x0B, 0x3D, 0x6E);
  private static final Color GRIS_CLAIR = new Color(0xEE, 0xF2, 0xF6);
  private static final Locale FR = Locale.FRANCE;
  private static final DateTimeFormatter DATE_HEURE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm 'UTC'", FR).withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", FR).withZone(ZoneOffset.UTC);

  private static final Font TITRE = new Font(Font.HELVETICA, 15, Font.BOLD, BLEU_CSE);
  private static final Font SOUS_TITRE = new Font(Font.HELVETICA, 11, Font.BOLD, BLEU_CSE);
  private static final Font NORMAL = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, Color.BLACK);
  private static final Font GRAS = new Font(Font.HELVETICA, 9.5f, Font.BOLD, Color.BLACK);
  private static final Font ENTETE_TABLEAU = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
  private static final Font PETIT = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY);

  @Override
  public byte[] generer(
      TypeProcesVerbal type, AppelOffre ao, List<OffreEvaluee> offres, Instant genereLe) {
    ByteArrayOutputStream sortie = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 40, 40, 40, 40);
    try {
      PdfWriter.getInstance(document, sortie);
      document.addTitle(titre(type) + " — " + ao.getReference());
      document.addAuthor("Compagnie Sahélienne d'Entreprises");
      document.open();
      entete(document, type, ao, genereLe);
      informations(document, ao);
      tableauOffres(document, type, ao, offres);
      if (type == TypeProcesVerbal.EVALUATION) {
        decision(document, ao, offres);
      }
      signatures(document, type);
      document.close();
    } catch (DocumentException e) {
      throw new IllegalStateException("Génération du procès-verbal impossible", e);
    }
    return sortie.toByteArray();
  }

  private static void entete(
      Document document, TypeProcesVerbal type, AppelOffre ao, Instant genereLe) {
    PdfPTable bandeau = new PdfPTable(new float[] {3, 2});
    bandeau.setWidthPercentage(100);
    PdfPCell societe = new PdfPCell();
    societe.setBorder(Rectangle.NO_BORDER);
    societe.addElement(new Paragraph("COMPAGNIE SAHÉLIENNE D'ENTREPRISES", SOUS_TITRE));
    societe.addElement(new Paragraph("Direction des Achats — Dakar, Sénégal", NORMAL));
    bandeau.addCell(societe);
    PdfPCell ref = new PdfPCell();
    ref.setBorder(Rectangle.NO_BORDER);
    ref.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Paragraph p = new Paragraph("Réf. " + ao.getReference(), GRAS);
    p.setAlignment(Element.ALIGN_RIGHT);
    ref.addElement(p);
    Paragraph d = new Paragraph("Édité le " + DATE_HEURE.format(genereLe), PETIT);
    d.setAlignment(Element.ALIGN_RIGHT);
    ref.addElement(d);
    bandeau.addCell(ref);
    document.add(bandeau);
    PdfPTable filet = new PdfPTable(1);
    filet.setWidthPercentage(100);
    PdfPCell ligne = new PdfPCell(new Phrase(" "));
    ligne.setBorder(Rectangle.BOTTOM);
    ligne.setBorderColor(BLEU_CSE);
    ligne.setBorderWidth(1.5f);
    ligne.setFixedHeight(4);
    filet.addCell(ligne);
    document.add(filet);
    Paragraph titre = new Paragraph(titre(type).toUpperCase(FR), TITRE);
    titre.setAlignment(Element.ALIGN_CENTER);
    titre.setSpacingBefore(14);
    titre.setSpacingAfter(12);
    document.add(titre);
  }

  private static void informations(Document document, AppelOffre ao) {
    document.add(section("1. Objet de la consultation"));
    PdfPTable t = new PdfPTable(new float[] {1.3f, 2.7f});
    t.setWidthPercentage(100);
    ligneInfo(t, "Objet", ao.getObjet());
    ligneInfo(
        t,
        "Procédure",
        switch (ao.getType()) {
          case APPEL_OFFRES -> "Appel d'offres ouvert";
          case DEMANDE_PRIX -> "Demande de prix restreinte";
        });
    ligneInfo(
        t, "Catégorie / quantité", ao.getCategorie() + " — " + ao.getQuantite() + " unité(s)");
    ao.getOrigine()
        .ifPresent(
            o ->
                ligneInfo(
                    t,
                    "Besoin d'origine",
                    o.reference() + (o.chantierNom() == null ? "" : " — " + o.chantierNom())));
    if (ao.getBudgetEstime() != null) {
      ligneInfo(t, "Budget estimé", fcfa(ao.getBudgetEstime()));
    }
    ligneInfo(t, "Publication", DATE_HEURE.format(ao.getDatePublication()));
    ligneInfo(t, "Date limite de dépôt", DATE_HEURE.format(ao.getDateLimite()));
    if (ao.getDateCloture() != null) {
      ligneInfo(t, "Ouverture des plis", DATE_HEURE.format(ao.getDateCloture()));
    }
    ligneInfo(
        t,
        "Critères (poids)",
        String.join(
            ", ",
            ao.getPonderation().criteres().stream()
                .map(OpenPdfProcesVerbalGenerator::critere)
                .toList()));
    if (!ao.getInvites().isEmpty()) {
      ligneInfo(
          t,
          "Fournisseurs invités",
          String.join(
              ", ", ao.getInvites().stream().map(FournisseurInvite::raisonSociale).toList()));
    }
    document.add(t);
  }

  private static void tableauOffres(
      Document document, TypeProcesVerbal type, AppelOffre ao, List<OffreEvaluee> offres) {
    document.add(
        section(
            type == TypeProcesVerbal.OUVERTURE
                ? "2. Offres reçues (" + offres.size() + ")"
                : "2. Évaluation des offres (" + offres.size() + ")"));
    if (offres.isEmpty()) {
      document.add(new Paragraph("Aucune offre n'a été déposée.", NORMAL));
      return;
    }
    boolean evaluation = type == TypeProcesVerbal.EVALUATION;
    String[] colonnes =
        evaluation
            ? new String[] {
              "Rang", "Code", "Soumissionnaire", "Montant", "Délai", "Score", "Retenue"
            }
            : new String[] {
              "N°", "Code", "Soumissionnaire", "Montant", "Délai", "Garantie", "Dépôt"
            };
    PdfPTable t =
        new PdfPTable(
            evaluation
                ? new float[] {0.6f, 1.2f, 2.6f, 1.9f, 0.9f, 0.9f, 0.9f}
                : new float[] {0.5f, 1.2f, 2.6f, 1.9f, 0.9f, 0.9f, 1.3f});
    t.setWidthPercentage(100);
    t.setHeaderRows(1);
    for (String c : colonnes) {
      PdfPCell cell = new PdfPCell(new Phrase(c, ENTETE_TABLEAU));
      cell.setBackgroundColor(BLEU_CSE);
      cell.setPadding(4);
      t.addCell(cell);
    }
    int i = 0;
    for (OffreEvaluee e : offres) {
      Offre o = e.offre();
      Color fond = i++ % 2 == 0 ? Color.WHITE : GRIS_CLAIR;
      cellule(
          t,
          evaluation ? (e.rang() == null ? "—" : String.valueOf(e.rang())) : String.valueOf(i),
          fond);
      cellule(t, o.getCodeAnonyme(), fond);
      cellule(t, ao.estAnonyme() ? "(anonyme)" : o.getFournisseurNom(), fond);
      celluleDroite(t, fcfa(o.getMontantPropose()), fond);
      celluleDroite(t, o.getDelaiLivraisonJours() + " j", fond);
      if (evaluation) {
        celluleDroite(t, e.scoreGlobal() == null ? "—" : e.scoreGlobal().toPlainString(), fond);
        cellule(t, o.isRetenue() ? "Oui" : "", fond);
      } else {
        celluleDroite(t, o.getGarantieMois() == null ? "—" : o.getGarantieMois() + " mois", fond);
        cellule(t, DATE.format(o.getDateDepot()), fond);
      }
    }
    document.add(t);
  }

  private static void decision(Document document, AppelOffre ao, List<OffreEvaluee> offres) {
    document.add(section("3. Décision d'attribution"));
    Attribution a = ao.getAttribution().orElse(null);
    Offre retenue = ao.offreRetenue().orElse(null);
    if (a == null || retenue == null) {
      document.add(new Paragraph("Aucune attribution.", NORMAL));
      return;
    }
    Paragraph p = new Paragraph();
    p.add(new Chunk("Le marché est attribué à ", NORMAL));
    p.add(new Chunk(retenue.getFournisseurNom(), GRAS));
    p.add(new Chunk(" (offre " + retenue.getCodeAnonyme() + ") pour un montant de ", NORMAL));
    p.add(new Chunk(fcfa(a.montant()), GRAS));
    p.add(new Chunk(".", NORMAL));
    document.add(p);
    offres.stream()
        .filter(e -> e.rang() != null && e.rang() == 1)
        .findFirst()
        .filter(e -> !e.offre().getId().equals(retenue.getId()))
        .ifPresent(
            e ->
                document.add(
                    new Paragraph(
                        "Nota : l'offre retenue n'est pas la mieux classée ("
                            + e.offre().getCodeAnonyme()
                            + ").",
                        PETIT)));
    Paragraph j = new Paragraph("Justification : " + a.justification(), NORMAL);
    j.setSpacingBefore(4);
    document.add(j);
    PdfPTable t = new PdfPTable(new float[] {1.6f, 1.2f, 1.6f, 1.4f});
    t.setWidthPercentage(100);
    t.setSpacingBefore(8);
    for (String c : new String[] {"Étape (R11)", "Décision", "Par", "Date"}) {
      PdfPCell cell = new PdfPCell(new Phrase(c, ENTETE_TABLEAU));
      cell.setBackgroundColor(BLEU_CSE);
      cell.setPadding(4);
      t.addCell(cell);
    }
    approbation(t, "Proposition (RA)", "PROPOSÉE", a.proposeePar().nom(), a.dateProposition());
    approbation(t, "Co-validation (RM)", a.coValidationMateriel());
    if (a.approbationDg() != null) {
      approbation(t, "Approbation (DG)", a.approbationDg());
    }
    document.add(t);
  }

  private static void signatures(Document document, TypeProcesVerbal type) {
    Paragraph titre =
        section(type == TypeProcesVerbal.OUVERTURE ? "3. Signatures" : "4. Signatures");
    titre.setSpacingBefore(18);
    document.add(titre);
    PdfPTable t = new PdfPTable(3);
    t.setWidthPercentage(100);
    for (String role :
        new String[] {"Responsable des Achats", "Responsable Matériel", "Direction Générale"}) {
      PdfPCell cell = new PdfPCell();
      cell.setFixedHeight(70);
      cell.setPadding(6);
      cell.addElement(new Paragraph(role, GRAS));
      cell.addElement(new Paragraph("Nom, date et signature", PETIT));
      t.addCell(cell);
    }
    document.add(t);
    Paragraph pied =
        new Paragraph(
            "Document généré par la plateforme CSE — toute modification manuscrite le rend nul.",
            PETIT);
    pied.setSpacingBefore(10);
    pied.setAlignment(Element.ALIGN_CENTER);
    document.add(pied);
  }

  private static void approbation(PdfPTable t, String etape, Approbation a) {
    approbation(
        t,
        etape,
        switch (a.statut()) {
          case EN_ATTENTE -> "EN ATTENTE";
          case APPROUVEE -> "APPROUVÉE";
          case REFUSEE -> "REFUSÉE";
        },
        a.parNom(),
        a.date());
  }

  private static void approbation(
      PdfPTable t, String etape, String decision, String par, Instant date) {
    cellule(t, etape, Color.WHITE);
    cellule(t, decision, Color.WHITE);
    cellule(t, par == null ? "—" : par, Color.WHITE);
    cellule(t, date == null ? "—" : DATE_HEURE.format(date), Color.WHITE);
  }

  private static Paragraph section(String texte) {
    Paragraph p = new Paragraph(texte, SOUS_TITRE);
    p.setSpacingBefore(10);
    p.setSpacingAfter(6);
    return p;
  }

  private static void ligneInfo(PdfPTable t, String libelle, String valeur) {
    PdfPCell l = new PdfPCell(new Phrase(libelle, GRAS));
    l.setBackgroundColor(GRIS_CLAIR);
    l.setPadding(4);
    t.addCell(l);
    PdfPCell v = new PdfPCell(new Phrase(valeur == null ? "—" : valeur, NORMAL));
    v.setPadding(4);
    t.addCell(v);
  }

  private static void cellule(PdfPTable t, String texte, Color fond) {
    PdfPCell c = new PdfPCell(new Phrase(texte, NORMAL));
    c.setBackgroundColor(fond);
    c.setPadding(4);
    t.addCell(c);
  }

  private static void celluleDroite(PdfPTable t, String texte, Color fond) {
    PdfPCell c = new PdfPCell(new Phrase(texte, NORMAL));
    c.setBackgroundColor(fond);
    c.setPadding(4);
    c.setHorizontalAlignment(Element.ALIGN_RIGHT);
    t.addCell(c);
  }

  private static String titre(TypeProcesVerbal type) {
    return type == TypeProcesVerbal.OUVERTURE
        ? "Procès-verbal d'ouverture des plis"
        : "Procès-verbal d'évaluation et d'attribution";
  }

  private static String critere(CriterePondere c) {
    return c.code().name().replace('_', ' ').toLowerCase(FR) + " (" + c.poids() + " %)";
  }

  private static String fcfa(BigDecimal montant) {
    NumberFormat f = NumberFormat.getIntegerInstance(FR);
    // Les polices standard PDF (WinAnsi) n'ont pas l'espace fine insécable du format français.
    return f.format(montant).replace(' ', ' ').replace(' ', ' ') + " FCFA";
  }
}
