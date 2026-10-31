package sn.cse.chatbot.domain.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sn.cse.chatbot.domain.model.ContexteDonnees;
import sn.cse.chatbot.domain.model.Source;
import sn.cse.chatbot.domain.model.TypeSource;

/**
 * Construit les sources citées à partir des données réellement utilisées. Les liens sont des
 * modèles configurables ({@code {id}}, {@code {parentId}}).
 */
public class CatalogueSources {

  private final Map<TypeSource, String> liens;
  private final int maximum;

  /**
   * @param liens modèle de lien par type
   * @param maximum nombre maximal de sources
   */
  public CatalogueSources(Map<TypeSource, String> liens, int maximum) {
    this.liens = new EnumMap<>(TypeSource.class);
    this.liens.putAll(liens);
    this.maximum = maximum;
  }

  /**
   * @param c données filtrées
   * @return les sources dédoublonnées, dans l'ordre d'apparition
   */
  public List<Source> sources(ContexteDonnees c) {
    Map<String, Source> resultat = new LinkedHashMap<>();
    c.materiels()
        .forEach(
            m ->
                ajouter(
                    resultat,
                    TypeSource.MATERIEL,
                    m.id(),
                    m.code() + " — " + m.designation(),
                    null));
    c.besoins()
        .forEach(
            b ->
                ajouter(
                    resultat,
                    TypeSource.BESOIN,
                    b.id(),
                    b.reference() + " — " + b.designation(),
                    null));
    c.contrats()
        .forEach(
            k ->
                ajouter(
                    resultat, TypeSource.CONTRAT, k.id(), k.numero() + " — " + k.objet(), null));
    c.appelsOffres()
        .forEach(
            a ->
                ajouter(
                    resultat,
                    TypeSource.APPEL_OFFRE,
                    a.id(),
                    a.reference() + " — " + a.objet(),
                    null));
    c.fournisseurs()
        .forEach(f -> ajouter(resultat, TypeSource.FOURNISSEUR, f.id(), f.raisonSociale(), null));
    c.maintenances()
        .forEach(
            m ->
                ajouter(
                    resultat,
                    TypeSource.MAINTENANCE,
                    m.id(),
                    m.numeroOt() + " — " + m.materielDesignation(),
                    m.materielId()));
    c.affectations()
        .forEach(
            a -> {
              ajouter(
                  resultat,
                  TypeSource.MATERIEL,
                  a.materielId(),
                  a.materielCode() + " — " + a.materielDesignation(),
                  null);
              ajouter(resultat, TypeSource.CHANTIER, a.chantierId(), a.chantierNom(), null);
            });
    c.chantiers().forEach(ch -> ajouter(resultat, TypeSource.CHANTIER, ch.id(), ch.nom(), null));
    return new ArrayList<>(resultat.values()).subList(0, Math.min(maximum, resultat.size()));
  }

  private void ajouter(
      Map<String, Source> cible, TypeSource type, String id, String libelle, String parentId) {
    if (id == null || id.isBlank()) {
      return;
    }
    String modele = liens.get(type);
    String lien =
        modele == null
            ? null
            : modele.replace("{id}", id).replace("{parentId}", parentId == null ? id : parentId);
    cible.putIfAbsent(type + ":" + id, new Source(type, id, libelle, lien));
  }
}
