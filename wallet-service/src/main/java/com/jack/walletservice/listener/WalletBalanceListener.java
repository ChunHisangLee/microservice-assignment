package com.jack.walletservice.listener;

import com.jack.common.constants.WalletConstants;
import com.jack.common.dto.response.ResponseDto;
import com.jack.common.dto.response.WalletBalanceMessageDto;
import com.jack.common.dto.response.WalletResponseDto;
import com.jack.walletservice.entity.Wallet;
import com.jack.walletservice.exception.WalletNotFoundException;
import com.jack.walletservice.repository.WalletRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class WalletBalanceListener {
    private final WalletRepository walletRepository;
    private final RabbitTemplate rabbitTemplate;

    public WalletBalanceListener(WalletRepository walletRepository, RabbitTemplate rabbitTemplate) {
        this.walletRepository = walletRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = WalletConstants.WALLET_BALANCE_QUEUE)
    public void handleWalletBalanceRequest(WalletBalanceMessageDto walletBalanceMessageDto) {
        Long userId = walletBalanceMessageDto.getUserId();
        log.info("Received Wallet Balance Request for UserID: {}", userId);

        // Define a constant for the reply-to queue
        String replyToQueue = WalletConstants.WALLET_REPLY_TO_QUEUE;

        // Validate userId
        if (userId == null) {
            log.error("Invalid Wallet Balance Request: userId is null");
            ResponseDto<String> errorResponse = ResponseDto.<String>builder()
                    .success(false)
                    .error("Invalid request: userId is null")
                    .build();
            rabbitTemplate.convertAndSend(replyToQueue, errorResponse);
            return;
        }

        try {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user ID: " + userId));

            WalletResponseDto responseDTO = WalletResponseDto.builder()
                    .userId(wallet.getUserId())
                    .usdBalance(wallet.getUsdBalance())
                    .btcBalance(wallet.getBtcBalance())
                    .build();

            // Send the successful response to the predefined replyToQueue
            ResponseDto<WalletResponseDto> successResponse = ResponseDto.<WalletResponseDto>builder()
                    .success(true)
                    .data(responseDTO)
                    .build();

            rabbitTemplate.convertAndSend(replyToQueue, successResponse);
            log.info("Wallet balance sent to replyToQueue: {} for UserID: {}", replyToQueue, userId);
        } catch (WalletNotFoundException e) {
            log.error("Wallet not found for user ID: {}", userId);
            // Send an error message to the replyToQueue
            ResponseDto<String> errorResponse = ResponseDto.<String>builder()
                    .success(false)
                    .error("Wallet not found for user ID: " + userId)
                    .build();
            rabbitTemplate.convertAndSend(replyToQueue, errorResponse);
        } catch (Exception e) {
            log.error("Failed to retrieve wallet balance for user ID: {}. Error: {}", userId, e.getMessage(), e);
            // Send a generic error message to the replyToQueue
            ResponseDto<String> errorResponse = ResponseDto.<String>builder()
                    .success(false)
                    .error("Failed to retrieve wallet balance for user ID: " + userId)
                    .build();
            rabbitTemplate.convertAndSend(replyToQueue, errorResponse);
        }
    }
}
