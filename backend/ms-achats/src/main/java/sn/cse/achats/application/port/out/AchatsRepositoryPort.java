package sn.cse.achats.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AchatsRepositoryPort {

    record AppelOffreDto(
            UUID id,
            String reference,
            String objet,
            String typeAo,
            UUID besoinId,
            LocalDate dateLimite,
            String statut,
            UUID offreRetenueId
    ) {}

    record OffreDto(
            UUID id,
            UUID appelOffreId,
            UUID fournisseurId,
            String codeAnonyme,
            BigDecimal montantPropose,
            int delaiLivraisonJours,
            double noteTechnique
    ) {}

    record ContratDto(
            UUID id,
            String numero,
            UUID appelOffreId,
            UUID offreId,
            UUID fournisseurId,
            BigDecimal montantTotal,
            String statut,
            boolean engagementValideDaf
    ) {}

    List<AppelOffreDto> findAllAppelsOffres();
    Optional<AppelOffreDto> findAppelOffreById(UUID id);
    AppelOffreDto saveAppelOffre(AppelOffreDto ao);

    List<OffreDto> findOffresByAppelOffreId(UUID appelOffreId);
    OffreDto saveOffre(OffreDto offre);

    List<ContratDto> findAllContrats();
    Optional<ContratDto> findContratById(UUID id);
    ContratDto saveContrat(ContratDto contrat);
}

