package com.jack.outboxservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jack.common.constants.EventStatus;
import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletCreateMessageDto;
import com.jack.common.dto.response.WalletUpdateMessageDto;
import com.jack.outboxservice.dto.OutboxDto;
import com.jack.outboxservice.entity.Outbox;
import com.jack.outboxservice.mapper.OutboxMapper;
import com.jack.outboxservice.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxServiceImplTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxServiceImpl outboxService;

    private Outbox outboxEntity;
    private OutboxDto outboxDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        outboxEntity = Outbox.builder()
                .id(1L)
                .eventType("WALLET_CREATE")
                .payload("{\"id\":1}")
                .createdAt(LocalDateTime.now())
                .status(EventStatus.PENDING)
                .build();

        outboxDto = OutboxMapper.INSTANCE.toDto(outboxEntity);
    }

    @Test
    void testSaveOutbox() {
        when(outboxRepository.save(any(Outbox.class))).thenReturn(outboxEntity);

        OutboxDto result = outboxService.saveOutbox(outboxDto);

        assertEquals(outboxDto.getId(), result.getId());
        verify(outboxRepository, times(1)).save(any(Outbox.class));
    }

    @Test
    void testGetOutboxById() {
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(outboxEntity));

        Optional<OutboxDto> result = outboxService.getOutboxById(1L);

        assertEquals(outboxDto.getId(), result.get().getId());
        verify(outboxRepository, times(1)).findById(1L);
    }

    @Test
    void testProcessOutbox_Success() throws JsonProcessingException {
        WalletCreateMessageDto message = new WalletCreateMessageDto(1L, BigDecimal.valueOf(1000));

        when(outboxRepository.findByStatus(EventStatus.PENDING)).thenReturn(Arrays.asList(outboxEntity));
        when(objectMapper.readValue(outboxEntity.getPayload(), WalletCreateMessageDto.class)).thenReturn(message);

        outboxService.processOutbox();

        verify(rabbitTemplate, times(1)).convertAndSend(WalletConstants.WALLET_CREATE_ROUTING_KEY, message);
        verify(outboxRepository, times(1)).save(any(Outbox.class));
        assertEquals(EventStatus.PROCESSED, outboxEntity.getStatus());
    }

    @Test
    void testProcessOutbox_Failure() throws JsonProcessingException {
        when(outboxRepository.findByStatus(EventStatus.PENDING)).thenReturn(Arrays.asList(outboxEntity));
        when(objectMapper.readValue(anyString(), eq(WalletCreateMessageDto.class))).thenThrow(new JsonProcessingException("Error") {});

        outboxService.processOutbox();

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(Object.class));
        verify(outboxRepository, never()).save(any(Outbox.class));
    }


    @Test
    void testProcessTransactionEvent_Success() throws JsonProcessingException {
        // Initialize argument captor
        ArgumentCaptor<WalletUpdateMessageDto> messageCaptor = ArgumentCaptor.forClass(WalletUpdateMessageDto.class);

        WalletUpdateMessageDto updateMessage = new WalletUpdateMessageDto(1L, BigDecimal.valueOf(0.01), BigDecimal.valueOf(5000));

        // Mock objectMapper to return a JSON string
        when(objectMapper.writeValueAsString(any(WalletUpdateMessageDto.class))).thenReturn("{\"userId\":1}");

        // Call the method to test
        outboxService.processTransactionEvent(1L, 1L, BigDecimal.valueOf(0.01), BigDecimal.valueOf(5000));

        // Verify outboxRepository save is called
        verify(outboxRepository, times(1)).save(any(Outbox.class));

        // Verify objectMapper is called with the correct argument, capturing the argument passed
        verify(objectMapper, times(1)).writeValueAsString(messageCaptor.capture());

        // Assert that the captured WalletUpdateMessageDto has the expected values
        WalletUpdateMessageDto capturedMessage = messageCaptor.getValue();
        assertEquals(updateMessage.getUserId(), capturedMessage.getUserId());
        assertEquals(updateMessage.getBtcAmount(), capturedMessage.getBtcAmount());
        assertEquals(updateMessage.getUsdAmount(), capturedMessage.getUsdAmount());
    }

    @Test
    void testProcessTransactionEvent_Failure() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any(WalletUpdateMessageDto.class))).thenThrow(new JsonProcessingException("Serialization error") {});

        outboxService.processTransactionEvent(1L, 1L, BigDecimal.valueOf(0.01), BigDecimal.valueOf(5000));

        verify(outboxRepository, never()).save(any(Outbox.class));
        verify(objectMapper, times(1)).writeValueAsString(any(WalletUpdateMessageDto.class));
    }
}
