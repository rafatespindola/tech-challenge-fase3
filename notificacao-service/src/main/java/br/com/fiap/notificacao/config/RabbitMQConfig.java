package br.com.fiap.notificacao.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Fila e bindings pertencem a este servico, nao ao produtor. Cada consumidor
 * tem a sua fila para que ambos recebam a mesma mensagem - fila compartilhada
 * viraria round-robin e cada servico veria so metade dos eventos.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${lab.rabbitmq.exchange}")
    private String exchange;

    @Value("${lab.rabbitmq.queue}")
    private String queue;

    // Notificar so faz sentido em criacao e edicao; exclusao nao avisa ninguem.
    @Value("${lab.rabbitmq.binding-patterns}")
    private String[] bindingPatterns;

    /**
     * Redeclarada de proposito: declaracao AMQP e idempotente, entao a ordem de
     * boot entre produtor e consumidor deixa de importar. Tipo e durabilidade
     * precisam bater com os do produtor, senao o broker responde 406.
     */
    @Bean
    public TopicExchange agendamentosExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue notificacaoQueue() {
        return new Queue(queue, true);
    }

    @Bean
    public Declarables notificacaoBindings(Queue notificacaoQueue, TopicExchange agendamentosExchange) {
        List<Binding> bindings = Arrays.stream(bindingPatterns)
                .map(padrao -> BindingBuilder.bind(notificacaoQueue).to(agendamentosExchange).with(padrao))
                .toList();
        return new Declarables(bindings);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
