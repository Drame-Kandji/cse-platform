package sn.cse.chatbot.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès Spring Data aux conversations. */
interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {

  Page<ConversationEntity> findByUtilisateurId(UUID utilisateurId, Pageable pageable);
}
