package com.fitcoach.roster;

import com.fitcoach.roster.dto.TrainerMessageDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerMessageServiceTest {

    @Mock private TrainerMessageRepository messageRepository;

    @InjectMocks
    private TrainerMessageService service;

    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final TrainerClient LINK = new TrainerClient(TRAINER_ID, CLIENT_ID);

    @Test
    void sendMessage_persistsAndReturnsDtoWithFromCurrentUserTrue() {
        when(messageRepository.save(any(TrainerMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainerMessageDto result = service.sendMessage(LINK, TRAINER_ID, "Great work this week!");

        assertThat(result.content()).isEqualTo("Great work this week!");
        assertThat(result.fromCurrentUser()).isTrue();
    }

    @Test
    void getHistory_marksOtherPartyMessagesAsNotFromCurrentUser() {
        TrainerMessage fromTrainer = new TrainerMessage(LINK.getId(), TRAINER_ID, "Hi!");
        TrainerMessage fromClient = new TrainerMessage(LINK.getId(), CLIENT_ID, "Hey coach");
        when(messageRepository.findByTrainerClientIdOrderByCreatedAtAsc(LINK.getId()))
                .thenReturn(List.of(fromTrainer, fromClient));

        List<TrainerMessageDto> result = service.getHistory(LINK, CLIENT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).fromCurrentUser()).isFalse();
        assertThat(result.get(1).fromCurrentUser()).isTrue();
    }

    @Test
    void getHistory_ordersByCreatedAtAsc_delegatesToRepository() {
        when(messageRepository.findByTrainerClientIdOrderByCreatedAtAsc(LINK.getId())).thenReturn(List.of());

        service.getHistory(LINK, TRAINER_ID);

        org.mockito.Mockito.verify(messageRepository).findByTrainerClientIdOrderByCreatedAtAsc(LINK.getId());
    }
}
