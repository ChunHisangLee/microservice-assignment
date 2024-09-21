package com.jack.walletservice.config;

import com.jack.common.constants.WalletConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    // Define queues
    @Bean
    public Queue walletCreateQueue() {
        logger.info("Creating walletCreateQueue: {}", WalletConstants.WALLET_CREATE_QUEUE);
        return new Queue(WalletConstants.WALLET_CREATE_QUEUE, true);
    }

    @Bean
    public Queue walletUpdateQueue() {
        logger.info("Creating walletUpdateQueue: {}", WalletConstants.WALLET_UPDATE_QUEUE);
        return new Queue(WalletConstants.WALLET_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue walletBalanceQueue() {
        logger.info("Creating walletBalanceQueue: {}", WalletConstants.WALLET_BALANCE_QUEUE);
        return new Queue(WalletConstants.WALLET_BALANCE_QUEUE, true);
    }

    // Define exchange
    @Bean
    public TopicExchange walletExchange() {
        logger.info("Creating exchange: {}", WalletConstants.WALLET_EXCHANGE);
        return new TopicExchange(WalletConstants.WALLET_EXCHANGE);
    }

    // Bind queues to exchange with respective routing keys
    @Bean
    public Binding bindingCreateQueue() {
        logger.info("Binding create queue with routing key: {}", WalletConstants.WALLET_CREATE_ROUTING_KEY);
        return BindingBuilder.bind(walletCreateQueue()).to(walletExchange()).with(WalletConstants.WALLET_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingUpdateQueue() {
        logger.info("Binding update queue with routing key: {}", WalletConstants.WALLET_UPDATE_ROUTING_KEY);
        return BindingBuilder.bind(walletUpdateQueue()).to(walletExchange()).with(WalletConstants.WALLET_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingBalanceQueue() {
        logger.info("Binding balance queue with routing key: {}", WalletConstants.WALLET_BALANCE_ROUTING_KEY);
        return BindingBuilder.bind(walletBalanceQueue()).to(walletExchange()).with(WalletConstants.WALLET_BALANCE_ROUTING_KEY);
    }
}
