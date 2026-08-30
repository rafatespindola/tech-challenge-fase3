package br.com.fiap.notificacao.notificador;

import br.com.fiap.notificacao.dto.input.MensagemFila;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificadorLog {

    public NotificadorLog() {
    }

    private static Logger logger = LoggerFactory.getLogger(NotificadorLog.class);

    public void notificarAgendamentoCriado(MensagemFila mensagemFila) {
        logger.info(mensagemFila.agendamento().paciente().nome() + ", seu agendamento foi criado com sucesso!" +
                "\nData: " + mensagemFila.agendamento().dataHora().toString() +
                "\nProfissional: " +  mensagemFila.agendamento().profissional().nome() +
                "\nProcedimento: " + mensagemFila.agendamento().procedimento().nome());
    }

    public void notificarAgendamentoEditado(MensagemFila mensagemFila) {
        logger.info(mensagemFila.agendamento().paciente().nome() + ", atenção! " +
                "Seu agendamento foi alterado para: " +
                "\nData: " + mensagemFila.agendamento().dataHora().toString() +
                "\nProfissional: " +  mensagemFila.agendamento().profissional().nome() +
                "\nProcedimento: " + mensagemFila.agendamento().procedimento().nome());
    }


}
