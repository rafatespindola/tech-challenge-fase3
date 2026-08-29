package br.com.fiap.agendamento.service;

import br.com.fiap.agendamento.dto.output.AgendamentoPublishOutput;
import br.com.fiap.agendamento.entity.Agendamento;
import br.com.fiap.agendamento.entity.AgendamentoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PublishService {

    private static final Logger log = LoggerFactory.getLogger(PublishService.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${lab.rabbitmq.exchange}")
    private String exchange;

    // So o prefixo e configuracao. O sufixo e o evento de dominio e vem do enum:
    // curinga aqui viraria routing key literal e nenhum consumidor conseguiria
    // distinguir criado de editado.
    @Value("${lab.rabbitmq.routing-key-prefix}")
    private String routingKeyPrefix;

    public PublishService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarAgendamentoCriado(Agendamento agendamento) {
        publicar(AgendamentoEvento.AGENDAMENTO_CRIADO, agendamento);
    }

    public void publicarAgendamentoAtualizado(Agendamento agendamento) {
        publicar(AgendamentoEvento.AGENDAMENTO_EDITADO, agendamento);
    }

    public void publicarAgendamentoExcluido(Agendamento agendamento) {
        publicar(AgendamentoEvento.AGENDAMENTO_EXCLUIDO, agendamento);
    }

    private void publicar(AgendamentoEvento evento, Agendamento agendamento) {
        String routingKey = routingKeyPrefix + "." + evento.getSufixo();
        String id = UUID.randomUUID().toString();

        AgendamentoPublishOutput payload = new AgendamentoPublishOutput(evento, agendamento);

        rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
            message.getMessageProperties().setMessageId(id);
            return message;
        });

        log.info("Mensagem de id {} publicada na exchange '{}' com routing key '{}' para o agendamento {}",
                id, exchange, routingKey, agendamento.getId());
    }
}
