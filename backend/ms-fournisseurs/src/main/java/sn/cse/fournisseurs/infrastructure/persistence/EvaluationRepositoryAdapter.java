package sn.cse.fournisseurs.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.fournisseurs.application.port.out.EvaluationRepository;
import sn.cse.fournisseurs.domain.model.Auteur;
import sn.cse.fournisseurs.domain.model.Evaluation;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;
import sn.cse.fournisseurs.infrastructure.persistence.entity.EvaluationEntity;
import sn.cse.fournisseurs.infrastructure.persistence.repository.EvaluationJpaRepository;

/** Adaptateur JPA du port {@link EvaluationRepository}. */
@Repository
@Transactional
public class EvaluationRepositoryAdapter implements EvaluationRepository {

  private final EvaluationJpaRepository jpa;

  /**
   * @param jpa dépôt Spring Data
   */
  public EvaluationRepositoryAdapter(EvaluationJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Evaluation enregistrer(Evaluation e) {
    NotesEvaluation n = e.notes();
    jpa.saveAndFlush(
        new EvaluationEntity(
            e.id(),
            e.fournisseurId(),
            e.contratId(),
            new int[] {n.qualite(), n.delais(), n.conformite(), n.sav(), n.prix()},
            e.noteGlobale(),
            e.commentaire(),
            e.evaluateur().id(),
            e.evaluateur().nom(),
            e.date()));
    return e;
  }

  @Override
  public List<Evaluation> parFournisseur(UUID fournisseurId) {
    return jpa.findByFournisseurIdOrderByDateEvaluationDesc(fournisseurId).stream()
        .map(EvaluationRepositoryAdapter::versDomaine)
        .toList();
  }

  private static Evaluation versDomaine(EvaluationEntity e) {
    return new Evaluation(
        e.getId(),
        e.getFournisseurId(),
        e.getContratId(),
        new NotesEvaluation(
            e.getQualite(), e.getDelais(), e.getConformite(), e.getSav(), e.getPrix()),
        e.getNoteGlobale(),
        e.getCommentaire(),
        new Auteur(e.getEvaluateurId(), e.getEvaluateurNom()),
        e.getDateEvaluation());
  }
}
