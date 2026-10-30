package sn.cse.suivi.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.infrastructure.persistence.entity.MaterielEntity;

@Component
public class MaterielMapper {

    public Materiel toDomain(MaterielEntity entity) {
        if (entity == null) {
            return null;
        }
        return Materiel.reconstituer(
                entity.getId(),
                entity.getImmatriculation(),
                entity.getDesignation(),
                entity.getCategorie(),
                entity.getEtat(),
                entity.getHeuresFonctionnement() != null ? entity.getHeuresFonctionnement() : 0L,
                entity.getChantierActuelId(),
                entity.getCreatedAt()
        );
    }

    public MaterielEntity toEntity(Materiel domain) {
        if (domain == null) {
            return null;
        }
        return MaterielEntity.builder()
                .id(domain.getId())
                .immatriculation(domain.getImmatriculation())
                .designation(domain.getDesignation())
                .categorie(domain.getCategorie())
                .etat(domain.getEtat())
                .heuresFonctionnement(domain.getHeuresFonctionnement())
                .chantierActuelId(domain.getChantierActuelId())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
