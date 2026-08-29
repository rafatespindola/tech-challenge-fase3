package br.com.fiap.agendamento.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Este servico e produtor, entao declara apenas a exchange. Fila e binding
 * pertencem a quem consome: cada consumidor tem a sua fila, senao os dois
 * disputariam a mesma mensagem em round-robin e cada um veria so metade.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${lab.rabbitmq.exchange}")
    private String exchange;

    @Bean
    public TopicExchange agendamentosExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

}
