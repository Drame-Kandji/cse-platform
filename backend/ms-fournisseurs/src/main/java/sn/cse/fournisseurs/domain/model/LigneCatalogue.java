package sn.cse.fournisseurs.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Ligne du catalogue de prix d'un fournisseur, identifiée par sa référence (upsert). */
public final class LigneCatalogue {

  private final UUID id;
  private final UUID fournisseurId;
  private DonneesLigneCatalogue donnees;
  private Instant dateMaj;
  private final long version;

  private LigneCatalogue(
      UUID id, UUID fournisseurId, DonneesLigneCatalogue donnees, Instant dateMaj, long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.fournisseurId = Objects.requireNonNull(fournisseurId, "fournisseurId");
    this.donnees = Objects.requireNonNull(donnees, "donnees");
    this.dateMaj = Objects.requireNonNull(dateMaj, "dateMaj");
    this.version = version;
  }

  /**
   * Crée une ligne de catalogue.
   *
   * @param fournisseur fournisseur (catégorie vérifiée contre ses spécialités)
   * @param donnees données de la ligne
   * @param date instant de publication
   * @return nouvelle ligne
   */
  public static LigneCatalogue creer(
      Fournisseur fournisseur, DonneesLigneCatalogue donnees, Instant date) {
    fournisseur.verifierCatalogueModifiable(donnees.categorie());
    return new LigneCatalogue(UUID.randomUUID(), fournisseur.getId(), donnees, date, 0L);
  }

  /**
   * Reconstitue une ligne depuis la persistance.
   *
   * @param id identifiant
   * @param fournisseurId fournisseur
   * @param donnees données
   * @param dateMaj date de dernière mise à jour
   * @param version version
   * @return ligne
   */
  public static LigneCatalogue reconstituer(
      UUID id, UUID fournisseurId, DonneesLigneCatalogue donnees, Instant dateMaj, long version) {
    return new LigneCatalogue(id, fournisseurId, donnees, dateMaj, version);
  }

  /**
   * Met à jour la ligne (même référence).
   *
   * @param fournisseur fournisseur propriétaire
   * @param nouvelles nouvelles données
   * @param date instant de mise à jour
   * @return {@code true} si le prix unitaire a changé (à historiser)
   */
  public boolean mettreAJour(
      Fournisseur fournisseur, DonneesLigneCatalogue nouvelles, Instant date) {
    if (!fournisseur.getId().equals(fournisseurId)
        || !donnees.reference().equals(nouvelles.reference())) {
      throw new IllegalArgumentException("La ligne ne correspond pas à cette référence");
    }
    fournisseur.verifierCatalogueModifiable(nouvelles.categorie());
    boolean prixChange = donnees.prixUnitaire().compareTo(nouvelles.prixUnitaire()) != 0;
    this.donnees = nouvelles;
    this.dateMaj = date;
    return prixChange;
  }

  public UUID getId() {
    return id;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public DonneesLigneCatalogue getDonnees() {
    return donnees;
  }

  public String getReference() {
    return donnees.reference();
  }

  public String getDesignation() {
    return donnees.designation();
  }

  public CategorieMateriel getCategorie() {
    return donnees.categorie();
  }

  public BigDecimal getPrixUnitaire() {
    return donnees.prixUnitaire();
  }

  public Instant getDateMaj() {
    return dateMaj;
  }

  public long getVersion() {
    return version;
  }
}
