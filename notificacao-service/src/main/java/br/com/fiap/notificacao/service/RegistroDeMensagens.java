package br.com.fiap.notificacao.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lembra os messageId ja processados para que uma reentrega do broker nao vire
 * uma segunda notificacao para o mesmo paciente.
 *
 * A memoria e do processo, e nao de uma base: este servico nao tem banco
 * proprio. Isso cobre a reentrega, que e o caso comum, e nao sobrevive a um
 * restart - o historico-service, que precisa da garantia forte, deduplica pelo
 * unique de message_id no banco. No dia em que a notificacao deixar de ser um
 * log e virar e-mail ou SMS, esta garantia tem que migrar para armazenamento
 * compartilhado.
 */
@Component
public class RegistroDeMensagens {

    /** Teto de ids lembrados. Passou disso, o mais antigo sai. */
    private static final int CAPACIDADE = 1000;

    private final Map<String, Boolean> processadas = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> maisAntiga) {
                    return size() > CAPACIDADE;
                }
            });

    /** Mensagem sem messageId nao da para deduplicar: passa como entrega nova. */
    public boolean jaProcessada(String messageId) {
        return messageId != null && processadas.containsKey(messageId);
    }

    public void marcarProcessada(String messageId) {
        if (messageId != null) {
            processadas.put(messageId, Boolean.TRUE);
        }
    }
}
