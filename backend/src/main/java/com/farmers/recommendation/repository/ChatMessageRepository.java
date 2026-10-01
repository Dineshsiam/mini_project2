package com.farmers.recommendation.repository;

import com.farmers.recommendation.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Retrieves a limited window of chat history for a conversation.
     * Using 20 messages max to keep LLM context reasonable.
     */
    List<ChatMessage> findTop20ByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * Count messages in a conversation (for stats/validation).
     */
    long countByConversationId(UUID conversationId);
}
