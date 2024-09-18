package com.jack.outboxservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.EventStatus;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.entity.Outbox;
import com.jack.outboxservice.mapper.OutboxMapper;
import com.jack.outboxservice.repository.OutboxRepository;
import com.jack.outboxservice.service.OutboxService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OutboxServiceImpl implements OutboxService {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.wallet.queue.create}")
    private String walletCreationQueue;

    public OutboxServiceImpl(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // Save an Outbox entry
    @Override
    public OutboxDto saveOutbox(OutboxDto outboxDTO) {
        Outbox outboxEntity = OutboxMapper.INSTANCE.toEntity(outboxDTO);  // Use the mapper
        outboxEntity.setCreatedAt(LocalDateTime.now());
        outboxEntity.setStatus(EventStatus.PENDING);  // Set status as pending

        Outbox savedEntity = outboxRepository.save(outboxEntity);
        return OutboxMapper.INSTANCE.toDto(savedEntity);
    }

    @Override
    public Optional<OutboxDto> getOutboxById(Long id) {
        return outboxRepository.findById(id)
                .map(outbox -> OutboxMapper.INSTANCE.toDto(outbox)); // Return mapped DTO or empty Optional if not found
    }


    // Process unprocessed outbox messages every 5 seconds
    @Override
    @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        List<Outbox> unprocessedMessages = outboxRepository.findByStatus(EventStatus.PENDING);  // Fetch unprocessed messages

        for (Outbox outbox : unprocessedMessages) {
            try {
                // Deserialize the message payload into WalletCreationMessage
                WalletCreateMessageDto message = objectMapper.readValue(outbox.getPayload(), WalletCreateMessageDto.class);

                // Send the message to RabbitMQ
                rabbitTemplate.convertAndSend(walletCreationQueue, message);

                // Mark the message as processed
                outbox.setStatus(EventStatus.PROCESSED);  // Update status
                outbox.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(outbox);  // Update the outbox entry
            } catch (Exception e) {
                System.err.println("Failed to process outbox message: " + e.getMessage());
            }
        }
    }
}