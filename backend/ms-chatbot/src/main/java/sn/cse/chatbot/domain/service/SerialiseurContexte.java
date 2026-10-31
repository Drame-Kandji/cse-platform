package sn.cse.chatbot.domain.service;

import java.util.ArrayList;
import java.util.List;
import sn.cse.chatbot.domain.model.ContexteDonnees;

/**
 * Sérialise les données filtrées en texte compact pour le LLM : une ligne par élément, préfixée du
 * type et de l'identifiant (citables comme sources). Chaque valeur est assainie (aucun délimiteur
 * ni caractère de contrôle ne peut provenir des données) et le tout est borné.
 */
public class SerialiseurContexte {

  private final int longueurMax;

  /**
   * @param longueurMax nombre maximal de caractères du contexte
   */
  public SerialiseurContexte(int longueurMax) {
    this.longueurMax = longueurMax;
  }

  /**
   * @param c données filtrées
   * @return le texte du contexte (« Aucune donnée » si vide)
   */
  public String serialiser(ContexteDonnees c) {
    List<String> lignes = new ArrayList<>();
    c.materiels()
        .forEach(
            m ->
                lignes.add(
                    ligne(
                        "MATERIEL",
                        m.id(),
                        "code=" + m.code(),
                        "designation=" + m.designation(),
                        "categorie=" + Libelles.de(m.categorie()),
                        "etat=" + Libelles.de(m.etat()),
                        "chantier=" + val(m.chantierNom()),
                        "gps=" + (m.latitude() == null ? "-" : m.latitude() + "," + m.longitude()),
                        "prochaineMaintenance=" + Libelles.date(m.prochaineMaintenance()),
                        "maintenanceDue=" + (m.maintenanceDue() ? "oui" : "non"))));
    c.besoins()
        .forEach(
            b ->
                lignes.add(
                    ligne(
                        "BESOIN",
                        b.id(),
                        "reference=" + b.reference(),
                        "designation=" + b.designation(),
                        "quantite=" + b.quantite(),
                        "statut=" + Libelles.de(b.statut()),
                        "priorite=" + Libelles.de(b.priorite()),
                        "chantier=" + val(b.chantierNom()),
                        "dateBesoin=" + Libelles.date(b.dateBesoin()),
                        "motifRejet=" + val(b.motifRejet()),
                        "validateur=" + val(b.validateurNom()))));
    c.contrats()
        .forEach(
            k ->
                lignes.add(
                    ligne(
                        "CONTRAT",
                        k.id(),
                        "numero=" + k.numero(),
                        "objet=" + k.objet(),
                        "fournisseur=" + val(k.fournisseurNom()),
                        "statut=" + Libelles.de(k.statut()),
                        "montant="
                            + (k.montant() == null
                                ? "non communiqué"
                                : Libelles.montant(k.montant())),
                        "livraisonPrevue=" + Libelles.date(k.dateLivraisonPrevue()),
                        "livraisonReelle=" + Libelles.date(k.dateLivraisonReelle()),
                        "chantier=" + val(k.chantierNom()))));
    c.appelsOffres()
        .forEach(
            a ->
                lignes.add(
                    ligne(
                        "APPEL_OFFRE",
                        a.id(),
                        "reference=" + a.reference(),
                        "objet=" + a.objet(),
                        "statut=" + Libelles.de(a.statut()),
                        "dateLimite=" + Libelles.date(a.dateLimite()),
                        "offres=" + a.nombreOffres(),
                        "montantAttribue="
                            + (a.montantAttribue() == null
                                ? "-"
                                : Libelles.montant(a.montantAttribue())))));
    c.fournisseurs()
        .forEach(
            f ->
                lignes.add(
                    ligne(
                        "FOURNISSEUR",
                        f.id(),
                        "rang=" + f.rang(),
                        "raisonSociale=" + f.raisonSociale(),
                        "score="
                            + (f.scorePerformance() == null
                                ? "-"
                                : Libelles.nombre(f.scorePerformance()) + "/100"),
                        "livraisonALHeure="
                            + (f.tauxLivraisonALHeure() == null
                                ? "-"
                                : Libelles.nombre(f.tauxLivraisonALHeure()) + "%"),
                        "litiges=" + f.nombreLitiges(),
                        "specialites="
                            + String.join(
                                "/", f.specialites().stream().map(Libelles::de).toList()))));
    c.maintenances()
        .forEach(
            m ->
                lignes.add(
                    ligne(
                        "MAINTENANCE",
                        m.id(),
                        "ot=" + m.numeroOt(),
                        "materiel=" + m.materielCode() + " " + m.materielDesignation(),
                        "type=" + Libelles.de(m.type()),
                        "statut=" + Libelles.de(m.statut()),
                        "datePlanifiee=" + Libelles.date(m.datePlanifiee()),
                        "dateFin=" + Libelles.date(m.dateFin()),
                        "description=" + val(m.description()),
                        "cout="
                            + (m.cout() == null ? "non communiqué" : Libelles.montant(m.cout())))));
    c.affectations()
        .forEach(
            a ->
                lignes.add(
                    ligne(
                        "AFFECTATION",
                        a.id(),
                        "materiel=" + a.materielCode() + " " + a.materielDesignation(),
                        "chantier=" + val(a.chantierNom()),
                        "du=" + Libelles.date(a.dateDebut()),
                        "au=" + Libelles.date(a.dateFinPrevue()),
                        "statut=" + Libelles.de(a.statut()))));
    c.chantiers()
        .forEach(
            ch ->
                lignes.add(
                    ligne(
                        "CHANTIER",
                        ch.id(),
                        "code=" + ch.code(),
                        "nom=" + ch.nom(),
                        "localisation=" + val(ch.localisation()),
                        "statut=" + Libelles.de(ch.statut()))));
    c.procedures()
        .forEach(
            p ->
                lignes.add(
                    ligne("PROCEDURE", p.sujet(), "etapes=" + String.join(" | ", p.etapes()))));
    if (!c.indisponibles().isEmpty()) {
      lignes.add("NOTE sections indisponibles (panne technique) : " + c.indisponibles());
    }
    if (!c.masquees().isEmpty()) {
      lignes.add("NOTE sections non accessibles au profil de l'utilisateur : " + c.masquees());
    }
    String texte = lignes.isEmpty() ? "Aucune donnée trouvée." : String.join("\n", lignes);
    return AssainisseurTexte.tronquer(texte, longueurMax);
  }

  private static String ligne(String type, String id, String... champs) {
    StringBuilder sb =
        new StringBuilder("[")
            .append(type)
            .append(' ')
            .append(AssainisseurTexte.nettoyer(id))
            .append(']');
    for (String champ : champs) {
      sb.append(' ').append(AssainisseurTexte.nettoyer(champ).replace('\n', ' ')).append(';');
    }
    return sb.toString();
  }

  private static String val(String v) {
    return v == null || v.isBlank() ? "-" : v;
  }
}
