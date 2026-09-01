package br.com.fiap.historico.listener;

import br.com.fiap.historico.dto.input.MensagemFila;
import br.com.fiap.historico.service.HistoricoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AgendamentoListener {

    private final HistoricoService historicoService;

    public AgendamentoListener(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    /**
     * Uma unica assinatura para os tres eventos: o binding e agendamento.*, e
     * quem separa criado de editado e de excluido e a coluna evento, nao o
     * codigo. Evento novo no produtor passa a ser gravado sem tocar aqui.
     *
     * O messageId vem do header AMQP e e o que torna a gravacao idempotente: se
     * a mensagem voltar para a fila apos uma falha, a segunda entrega traz o
     * mesmo id e nao duplica a linha.
     */
    @RabbitListener(queues = "${lab.rabbitmq.queue}")
    public void receber(@Payload MensagemFila mensagemFila,
                        @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        historicoService.registrar(mensagemFila, messageId);
    }
}
