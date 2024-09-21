package com.jack.outboxservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.EventStatus;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.common.dto.response.WalletUpdateMessageDto;
import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.entity.Outbox;
import com.jack.outboxservice.mapper.OutboxMapper;
import com.jack.outboxservice.repository.OutboxRepository;
import com.jack.outboxservice.service.OutboxService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OutboxServiceImpl implements OutboxService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxServiceImpl.class);

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
                .map(OutboxMapper.INSTANCE::toDto); // Return mapped DTO or empty Optional if not found
    }

    // Process unprocessed outbox messages every 5 seconds
    @Override
    @Scheduled(fixedRate = 5000)
    public void processOutbox() {
        List<Outbox> unprocessedMessages = outboxRepository.findByStatus(EventStatus.PENDING);  // Fetch unprocessed messages

        for (Outbox outbox : unprocessedMessages) {
            try {
                // Ensure the payload is not null
                if (outbox.getPayload() != null) {
                    // Deserialize the message payload into WalletCreationMessage
                    WalletCreateMessageDto message = objectMapper.readValue(outbox.getPayload(), WalletCreateMessageDto.class);

                    // Send the message to RabbitMQ
                    rabbitTemplate.convertAndSend(outbox.getRoutingKey(), message);

                    // Mark the message as processed
                    outbox.setStatus(EventStatus.PROCESSED);  // Update status
                    outbox.setProcessedAt(LocalDateTime.now());
                    outboxRepository.save(outbox);  // Update the outbox entry
                } else {
                    logger.warn("Payload is null for outbox entry with ID: {}", outbox.getId());
                }
            } catch (Exception e) {
                logger.error("Failed to process outbox message with ID: {}: {}", outbox.getId(), e.getMessage());
            }
        }
    }

    // Process a transaction event from transaction-service
    @Override
    public void processTransactionEvent(Long transactionId, Long userId, double btcAmount) {
        try {
            // Create a new outbox entry for the transaction event
            Outbox outbox = Outbox.builder()
                    .eventType("WALLET_CREATION") // Assuming the event type is wallet creation
                    .routingKey(walletCreationQueue)
                    .payload(objectMapper.writeValueAsString(new WalletUpdateMessageDto( userId, btcAmount)))
                    .createdAt(LocalDateTime.now())
                    .status(EventStatus.PENDING)
                    .build();

            // Save the outbox entry to the repository
            outboxRepository.save(outbox);

            logger.info("Transaction event successfully processed: transactionId={}, userId={}, btcAmount={}",
                    transactionId, userId, btcAmount);
        } catch (Exception e) {
            logger.error("Failed to process transaction event: transactionId={}, userId={}, btcAmount={}, error={}",
                    transactionId, userId, btcAmount, e.getMessage());
        }
    }
}
