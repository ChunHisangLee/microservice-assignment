package com.jack.userservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.userservice.message.WalletCreationMessage;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.wallet-creation}")
    private String walletCreationQueue;

    public OutboxService(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // Save an Outbox entry from DTO
    public OutboxDTO saveOutbox(OutboxDTO outboxDTO) {
        Outbox outboxEntity = OutboxMapper.mapToEntity(outboxDTO);
        Outbox savedEntity = outboxRepository.save(outboxEntity);
        return OutboxMapper.mapToDTO(savedEntity);
    }

    // Fetch an Outbox entry and return a DTO
    public OutboxDTO getOutboxById(Long id) {
        Outbox outboxEntity = outboxRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outbox entry not found"));
        // Convert Entity to DTO
        return OutboxMapper.mapToDTO(outboxEntity);
    }

    // Scheduled method to process unprocessed messages in the Outbox
    @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        List<Outbox> unprocessedMessages = outboxRepository.findByProcessedFalse();

        for (Outbox outbox : unprocessedMessages) {
            try {
                // Deserialize the message payload
                WalletCreationMessage message = objectMapper.readValue(outbox.getPayload(), WalletCreationMessage.class);
                rabbitTemplate.convertAndSend(walletCreationQueue, message);
                outbox.setProcessed(true);
                outbox.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(outbox);
            } catch (Exception e) {
                System.err.println("Failed to process outbox message: " + e.getMessage());
            }
        }
    }
}
