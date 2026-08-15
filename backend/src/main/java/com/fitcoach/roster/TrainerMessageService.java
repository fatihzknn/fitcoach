package com.fitcoach.roster;

import com.fitcoach.roster.dto.TrainerMessageDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Pure domain service for a trainer/client message thread — no {@code CurrentUser},
 * no role/ownership checks. Authorization stays centralized in {@link TrainerRosterService},
 * which resolves the {@link TrainerClient} link before calling in here.
 */
@Service
public class TrainerMessageService {

    private final TrainerMessageRepository messageRepository;

    public TrainerMessageService(TrainerMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public TrainerMessageDto sendMessage(TrainerClient link, UUID senderId, String content) {
        TrainerMessage saved = messageRepository.save(new TrainerMessage(link.getId(), senderId, content));
        return TrainerMessageDto.from(saved, senderId);
    }

    @Transactional(readOnly = true)
    public List<TrainerMessageDto> getHistory(TrainerClient link, UUID callerId) {
        return messageRepository.findByTrainerClientIdOrderByCreatedAtAsc(link.getId()).stream()
                .map(m -> TrainerMessageDto.from(m, callerId))
                .toList();
    }
}
