package br.com.fiap.notificacao.service;

import br.com.fiap.notificacao.dto.input.AgendamentoEvento;
import br.com.fiap.notificacao.dto.input.MensagemFila;
import br.com.fiap.notificacao.notificador.NotificadorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsumidorService {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorService.class);
    private static final NotificadorLog notificadorLog = new NotificadorLog();

    public void processar(MensagemFila mensagemFila, String messageId, String fila) {
        if (mensagemFila.agendamentoEvento().name().equals(AgendamentoEvento.AGENDAMENTO_CRIADO.name())) {
            notificadorLog.notificarAgendamentoCriado(mensagemFila);
        } else if (mensagemFila.agendamentoEvento().name().equals(AgendamentoEvento.AGENDAMENTO_EDITADO.name())) {
            notificadorLog.notificarAgendamentoEditado(mensagemFila);
        }
    }
}
