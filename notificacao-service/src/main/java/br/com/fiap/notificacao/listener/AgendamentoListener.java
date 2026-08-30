package br.com.fiap.notificacao.listener;

import br.com.fiap.notificacao.dto.input.MensagemFila;
import br.com.fiap.notificacao.service.ConsumidorService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AgendamentoListener {
    private final ConsumidorService consumidorService;

    public AgendamentoListener(ConsumidorService consumidorService) {
        this.consumidorService = consumidorService;
    }

    @RabbitListener(queues = "${lab.rabbitmq.queue}")
    public void receber(@Payload MensagemFila mensagemFila,
                        @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId,
                        @Header(name = AmqpHeaders.CONSUMER_QUEUE, required = false) String fila) {
        consumidorService.processar(mensagemFila, messageId, fila);
    }
}
