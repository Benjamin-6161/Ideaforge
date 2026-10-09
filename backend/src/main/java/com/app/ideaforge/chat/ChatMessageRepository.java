package com.app.ideaforge.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

	List<ChatMessage> findByIdeaIdOrderByCreatedAtAsc(UUID ideaId);

	Page<ChatMessage> findByIdeaIdOrderByCreatedAtDesc(UUID ideaId, Pageable pageable);

	Optional<ChatMessage> findBySpendLedgerId(UUID spendLedgerId);

	Optional<ChatMessage> findFirstByIdeaIdAndRoleOrderByCreatedAtDesc(UUID ideaId, ChatMessage.Role role);

	long countByIdeaIdAndRole(UUID ideaId, ChatMessage.Role role);

	long countByUserIdAndRoleAndCreatedAtAfter(UUID userId, ChatMessage.Role role, Instant after);

	Optional<ChatMessage> findFirstByIdeaIdAndRoleAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
			UUID ideaId, ChatMessage.Role role, Instant createdAt);
}
