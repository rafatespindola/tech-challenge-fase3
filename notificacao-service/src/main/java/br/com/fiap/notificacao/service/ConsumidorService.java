package br.com.fiap.notificacao.service;

import br.com.fiap.notificacao.dto.input.MensagemFila;
import br.com.fiap.notificacao.notificador.NotificadorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsumidorService {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorService.class);
    private static final NotificadorLog notificadorLog = new NotificadorLog();

    private final RegistroDeMensagens registroDeMensagens;

    public ConsumidorService(RegistroDeMensagens registroDeMensagens) {
        this.registroDeMensagens = registroDeMensagens;
    }

    public void processar(MensagemFila mensagemFila, String messageId, String fila) {
        if (naoNotificavel(mensagemFila)) {
            // Descarta em vez de relancar: uma mensagem malformada volta da fila
            // na mesma condicao e falha de novo, prendendo o consumidor num loop
            // que nao deixa nada atras dela ser processado.
            log.warn("Mensagem {} da fila {} descartada: evento ou agendamento ausente",
                    messageId, fila);
            return;
        }

        // A reentrega e normal: o broker reenvia o que nao foi confirmado, e o
        // retry reexecuta este metodo. Sem esta checagem, cada reenvio vira mais
        // uma notificacao para o paciente.
        if (registroDeMensagens.jaProcessada(messageId)) {
            log.info("Mensagem {} ja notificada, ignorando reentrega", messageId);
            return;
        }

        switch (mensagemFila.agendamentoEvento()) {
            case AGENDAMENTO_CRIADO -> notificadorLog.notificarAgendamentoCriado(mensagemFila);
            case AGENDAMENTO_EDITADO -> notificadorLog.notificarAgendamentoEditado(mensagemFila);
            // AGENDAMENTO_EXCLUIDO nao tem binding para esta fila. Se um evento
            // novo passar a ser roteado para ca, o consumidor registra e segue,
            // em vez de estourar por falta de branch.
            default -> log.info("Evento {} sem notificacao definida (mensagem {})",
                    mensagemFila.agendamentoEvento(), messageId);
        }

        // Marca depois de notificar, e nao antes: se o notificador estourar no
        // meio, a mensagem tem que poder ser reprocessada pelo retry.
        registroDeMensagens.marcarProcessada(messageId);
    }

    /**
     * O notificador navega paciente, profissional e procedimento do agendamento,
     * entao a mensagem so serve se esses tres vierem preenchidos.
     */
    private boolean naoNotificavel(MensagemFila mensagemFila) {
        return mensagemFila == null
                || mensagemFila.agendamentoEvento() == null
                || mensagemFila.agendamento() == null
                || mensagemFila.agendamento().paciente() == null
                || mensagemFila.agendamento().profissional() == null
                || mensagemFila.agendamento().procedimento() == null;
    }
}
