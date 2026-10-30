package sn.cse.besoins.domain.service;

import java.time.LocalDate;
import java.util.UUID;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Vue minimale d'un besoin pour la consolidation.
 *
 * @param id identifiant du besoin
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param categorie catégorie
 * @param designation désignation saisie
 * @param quantite quantité
 * @param priorite priorité
 * @param dateBesoin date de besoin
 */
public record BesoinAConsolider(
    UUID id,
    UUID chantierId,
    String chantierNom,
    CategorieMateriel categorie,
    String designation,
    int quantite,
    Priorite priorite,
    LocalDate dateBesoin) {}
