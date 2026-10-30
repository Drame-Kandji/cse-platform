package sn.cse.achats.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.cse.achats.application.port.out.AchatsRepositoryPort;
import sn.cse.achats.infrastructure.persistence.entity.AppelOffreEntity;
import sn.cse.achats.infrastructure.persistence.entity.ContratAchatEntity;
import sn.cse.achats.infrastructure.persistence.entity.OffreEntity;
import sn.cse.achats.infrastructure.persistence.repository.AppelOffreJpaRepository;
import sn.cse.achats.infrastructure.persistence.repository.ContratAchatJpaRepository;
import sn.cse.achats.infrastructure.persistence.repository.OffreJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AchatsRepositoryAdapter implements AchatsRepositoryPort {

    private final AppelOffreJpaRepository aoRepo;
    private final OffreJpaRepository offreRepo;
    private final ContratAchatJpaRepository contratRepo;

    @Override
    public List<AppelOffreDto> findAllAppelsOffres() {
        return aoRepo.findAll().stream().map(this::toAoDto).toList();
    }

    @Override
    public Optional<AppelOffreDto> findAppelOffreById(UUID id) {
        return aoRepo.findById(id).map(this::toAoDto);
    }

    @Override
    public AppelOffreDto saveAppelOffre(AppelOffreDto ao) {
        AppelOffreEntity e = aoRepo.findById(ao.id())
                .orElse(AppelOffreEntity.builder().id(ao.id()).createdAt(Instant.now()).build());
        e.setReference(ao.reference());
        e.setObjet(ao.objet());
        e.setTypeAo(ao.typeAo());
        e.setBesoinId(ao.besoinId());
        e.setDateLimite(ao.dateLimite());
        e.setStatut(ao.statut());
        e.setOffreRetenueId(ao.offreRetenueId());
        return toAoDto(aoRepo.save(e));
    }

    @Override
    public List<OffreDto> findOffresByAppelOffreId(UUID appelOffreId) {
        return offreRepo.findByAppelOffreId(appelOffreId).stream().map(this::toOffreDto).toList();
    }

    @Override
    public OffreDto saveOffre(OffreDto o) {
        OffreEntity e = OffreEntity.builder()
                .id(o.id())
                .appelOffreId(o.appelOffreId())
                .fournisseurId(o.fournisseurId())
                .codeAnonyme(o.codeAnonyme())
                .montantPropose(o.montantPropose())
                .delaiLivraisonJours(o.delaiLivraisonJours())
                .noteTechnique(o.noteTechnique())
                .createdAt(Instant.now())
                .build();
        return toOffreDto(offreRepo.save(e));
    }

    @Override
    public List<ContratDto> findAllContrats() {
        return contratRepo.findAll().stream().map(this::toContratDto).toList();
    }

    @Override
    public Optional<ContratDto> findContratById(UUID id) {
        return contratRepo.findById(id).map(this::toContratDto);
    }

    @Override
    public ContratDto saveContrat(ContratDto c) {
        ContratAchatEntity e = contratRepo.findById(c.id())
                .orElse(ContratAchatEntity.builder().id(c.id()).createdAt(Instant.now()).build());
        e.setNumero(c.numero());
        e.setAppelOffreId(c.appelOffreId());
        e.setOffreId(c.offreId());
        e.setFournisseurId(c.fournisseurId());
        e.setMontantTotal(c.montantTotal());
        e.setStatut(c.statut());
        e.setEngagementValideDaf(c.engagementValideDaf());
        return toContratDto(contratRepo.save(e));
    }

    private AppelOffreDto toAoDto(AppelOffreEntity e) {
        return new AppelOffreDto(e.getId(), e.getReference(), e.getObjet(), e.getTypeAo(),
                e.getBesoinId(), e.getDateLimite(), e.getStatut(), e.getOffreRetenueId());
    }

    private OffreDto toOffreDto(OffreEntity e) {
        return new OffreDto(e.getId(), e.getAppelOffreId(), e.getFournisseurId(),
                e.getCodeAnonyme(), e.getMontantPropose(),
                e.getDelaiLivraisonJours() != null ? e.getDelaiLivraisonJours() : 0,
                e.getNoteTechnique() != null ? e.getNoteTechnique() : 80.0);
    }

    private ContratDto toContratDto(ContratAchatEntity e) {
        return new ContratDto(e.getId(), e.getNumero(), e.getAppelOffreId(), e.getOffreId(),
                e.getFournisseurId(), e.getMontantTotal(), e.getStatut(),
                Boolean.TRUE.equals(e.getEngagementValideDaf()));
    }
}

