package com.fitcoach.coach;

import com.fitcoach.common.BaseEntity;
import com.fitcoach.coach.domain.MessageRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_conversations")
public class ChatConversation extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    protected ChatConversation() {}

    public ChatConversation(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
    }

    public ChatMessage addMessage(MessageRole role, String content) {
        ChatMessage msg = new ChatMessage(this, role, content);
        messages.add(msg);
        return msg;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public List<ChatMessage> getMessages() { return messages; }
}
