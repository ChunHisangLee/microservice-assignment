package com.jack.outboxservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.entity.Outbox;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.userservice.repository.OutboxRepository;
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

    @Value("${app.wallet.queue.create}")
    private String walletCreationQueue;

    public OutboxService(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // Save an Outbox entry
    public OutboxDTO saveOutbox(OutboxDTO outboxDTO) {
        Outbox outboxEntity = OutboxMapper.mapToEntity(outboxDTO);
        outboxEntity.setCreatedAt(LocalDateTime.now());
        outboxEntity.setProcessed(false);  // Mark as not yet processed
        Outbox savedEntity = outboxRepository.save(outboxEntity);
        return OutboxMapper.mapToDTO(savedEntity);
    }

    // Process unprocessed outbox messages every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        List<Outbox> unprocessedMessages = outboxRepository.findByProcessedFalse();

        for (Outbox outbox : unprocessedMessages) {
            try {
                // Deserialize the message payload into WalletCreationMessage
                WalletCreateMessageDto message = objectMapper.readValue(outbox.getPayload(), WalletCreateMessageDto.class);

                // Send the message to RabbitMQ
                rabbitTemplate.convertAndSend(walletCreationQueue, message);

                // Mark the message as processed
                outbox.setProcessed(true);
                outbox.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(outbox);  // Update the outbox entry
            } catch (Exception e) {
                System.err.println("Failed to process outbox message: " + e.getMessage());
            }
        }
    }
}
