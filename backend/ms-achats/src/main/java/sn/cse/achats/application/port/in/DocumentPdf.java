package sn.cse.achats.application.port.in;

/**
 * Document PDF produit.
 *
 * @param nomFichier nom de fichier proposé au téléchargement
 * @param contenu octets du PDF
 */
public record DocumentPdf(String nomFichier, byte[] contenu) {}
