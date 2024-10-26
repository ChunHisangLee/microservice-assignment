package com.jack.userservice.client;

import com.jack.common.constants.ErrorCode;
import com.jack.common.constants.ErrorPath;
import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.WalletBalanceMessageDto;
import com.jack.common.exception.CustomErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class WalletBalanceRequestSender {
    private final RabbitTemplate rabbitTemplate;

    public void sendBalanceRequest(Long userId) {
        // Build the new WalletBalanceMessageDto instead of WalletBalanceRequest
        WalletBalanceMessageDto request = WalletBalanceMessageDto.builder()
                .userId(userId)
                .build();

        try {
            // Send the request to the wallet service via RabbitMQ
            rabbitTemplate.convertAndSend(WalletConstants.WALLET_EXCHANGE, WalletConstants.WALLET_BALANCE_ROUTING_KEY, request, message -> {
                // Set the reply-to queue where the response will be sent
                message.getMessageProperties().setReplyTo(WalletConstants.WALLET_REPLY_TO_QUEUE);
                return message;
            });

            log.info("Sent wallet balance request for userId {} to exchange {} with routing key {}", userId, WalletConstants.WALLET_EXCHANGE, WalletConstants.WALLET_BALANCE_ROUTING_KEY);
        } catch (Exception e) {
            log.error("Failed to send wallet balance request for userId {}: {}", userId, e.getMessage());
            throw new CustomErrorException(ErrorCode.WALLET_SERVICE_ERROR, ErrorPath.GET_USER_BALANCE_API.getPath());
        }
    }
}
