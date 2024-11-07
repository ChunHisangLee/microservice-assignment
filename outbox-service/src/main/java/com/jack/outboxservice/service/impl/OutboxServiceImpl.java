package com.jack.outboxservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.EventStatus;
import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.OutboxDto;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.common.dto.response.WalletUpdateMessageDto;
import com.jack.outboxservice.entity.Outbox;
import com.jack.outboxservice.mapper.OutboxMapper;
import com.jack.outboxservice.repository.OutboxRepository;
import com.jack.outboxservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class OutboxServiceImpl implements OutboxService {
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxMapper outboxMapper;

    // Save an Outbox entry
    @Override
    public OutboxDto saveOutbox(OutboxDto outboxDTO) {
        Outbox outboxEntity = outboxMapper.toEntity(outboxDTO);
        outboxEntity.setCreatedAt(LocalDateTime.now());
        outboxEntity.setStatus(EventStatus.PENDING);
        Outbox savedEntity = outboxRepository.save(outboxEntity);
        return outboxMapper.toDto(savedEntity);
    }

    @Override
    public Optional<OutboxDto> getOutboxById(Long id) {
        return outboxRepository.findById(id)
                .map(outboxMapper::toDto);
    }

    // Process unprocessed outbox messages every 5 seconds
    @Override
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processOutbox() {
        List<Outbox> unprocessedMessages = outboxRepository.findByStatus(EventStatus.PENDING);

        for (Outbox outbox : unprocessedMessages) {
            try {
                if (outbox.getPayload() != null) {
                    // Deserialize based on a message type
                    String messageType = outbox.getEventType();
                    switch (messageType) {
                        case WalletConstants.WALLET_CREATE:
                            WalletCreateMessageDto createMessage = objectMapper.readValue(outbox.getPayload(), WalletCreateMessageDto.class);
                            rabbitTemplate.convertAndSend(
                                    WalletConstants.WALLET_EXCHANGE,
                                    WalletConstants.WALLET_CREATE_ROUTING_KEY,
                                    createMessage);
                            break;
                        case WalletConstants.WALLET_UPDATE:
                            WalletUpdateMessageDto updateMessage = objectMapper.readValue(outbox.getPayload(), WalletUpdateMessageDto.class);
                            rabbitTemplate.convertAndSend(
                                    WalletConstants.WALLET_EXCHANGE,
                                    WalletConstants.WALLET_UPDATE_ROUTING_KEY,
                                    updateMessage);
                            break;
                        // Add cases for USER_UPDATE, TRANSACTION_UPDATE if needed










                        default:
                            log.warn("Unknown message type: {}", messageType);
                            continue;
                    }

                    // Update Outbox status
                    outbox.setStatus(EventStatus.PROCESSED);
                    outbox.setProcessedAt(LocalDateTime.now());
                    outboxRepository.save(outbox);
                    log.info("Processed outbox message with ID: {}", outbox.getId());
                } else {
                    log.warn("Payload is null for outbox entry with ID: {}", outbox.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process outbox message with ID: {}: {}", outbox.getId(), e.getMessage());
            }
        }
    }

    @Override
    public void processTransactionEvent(Long transactionId, Long userId, BigDecimal btcAmount, BigDecimal usdAmount) {
        try {
            WalletUpdateMessageDto updateMessage = new WalletUpdateMessageDto(userId, btcAmount, usdAmount);

            // Publish the wallet update event to RabbitMQ
            rabbitTemplate.convertAndSend(
                    WalletConstants.WALLET_EXCHANGE,
                    WalletConstants.WALLET_UPDATE_ROUTING_KEY,
                    updateMessage);

            // Log the transaction event
            log.info("Transaction event successfully published: transactionId={}, userId={}, btcAmount={}", transactionId, userId, btcAmount);
        } catch (Exception e) {
            log.error("Failed to process transaction event: transactionId={}, userId={}, btcAmount={}, error={}", transactionId, userId, btcAmount, e.getMessage());
        }
    }
}
