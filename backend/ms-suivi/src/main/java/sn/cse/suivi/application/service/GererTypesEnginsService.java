package sn.cse.suivi.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.suivi.application.port.in.GererTypesEnginsUseCase;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.port.out.TypeEnginRepository;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.TypeEngin;

/** Référentiel des types d'engins (administration). */
@Service
@Transactional
public class GererTypesEnginsService implements GererTypesEnginsUseCase {

  private final TypeEnginRepository types;
  private final MaterielRepository materiels;

  /**
   * @param types stockage des types d'engins
   * @param materiels stockage des matériels
   */
  public GererTypesEnginsService(TypeEnginRepository types, MaterielRepository materiels) {
    this.types = types;
    this.materiels = materiels;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TypeEngin> lister() {
    return types.findAll();
  }

  @Override
  public TypeEngin creer(DonneesTypeEngin donnees) {
    verifierCodeLibre(donnees.code(), null);
    return types.save(TypeEngin.creer(donnees));
  }

  @Override
  public TypeEngin modifier(UUID id, DonneesTypeEngin donnees) {
    TypeEngin type =
        types.findById(id).orElseThrow(() -> new ResourceNotFoundException("TypeEngin", id));
    verifierCodeLibre(donnees.code(), id);
    List<Materiel> rattaches = materiels.findByTypeEngin(id);
    if (type.getDonnees().categorie() != donnees.categorie() && !rattaches.isEmpty()) {
      throw SuiviErreurs.regle(
          "TYPE_ENGIN_UTILISE",
          "La catégorie ne peut pas changer : "
              + rattaches.size()
              + " matériel(s) sont rattachés à ce type.");
    }
    if (type.modifier(donnees)) {
      for (Materiel materiel : rattaches) {
        materiel.appliquerSeuils(donnees.seuilMaintenanceHeures(), donnees.seuilMaintenanceJours());
        materiels.save(materiel);
      }
    }
    return types.save(type);
  }

  private void verifierCodeLibre(String code, UUID exclureId) {
    if (types.existsByCode(code, exclureId)) {
      throw SuiviErreurs.conflit(
          "CODE_DEJA_UTILISE", "Le code de type d'engin " + code + " est déjà utilisé.");
    }
  }
}
