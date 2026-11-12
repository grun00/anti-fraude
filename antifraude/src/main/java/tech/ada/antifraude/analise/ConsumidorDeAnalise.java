package tech.ada.antifraude.analise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tech.ada.antifraude.contracts.FilaDeAnalise;
import tech.ada.antifraude.contracts.TransacaoRecebida;

/**
 * Escuta a fila de análise. O Spring manda o ack quando este método termina sem erro: só aí a
 * mensagem sai da fila. Se der erro, a mensagem é entregue de novo (até 3 tentativas, no
 * application.yml) e, se falhar de vez, vai para a DLQ.
 *
 * A entrega é "pelo menos uma vez": a mesma mensagem pode chegar duas vezes. A segunda não
 * grava nada (ADR-002).
 */
@Component
public class ConsumidorDeAnalise {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorDeAnalise.class);

    private final AnaliseService servico;

    public ConsumidorDeAnalise(AnaliseService servico) {
        this.servico = servico;
    }

    @RabbitListener(queues = FilaDeAnalise.FILA)
    public void receber(TransacaoRecebida transacao,
                        @Header(name = AmqpHeaders.REDELIVERED, required = false) Boolean reentrega) {
        validar(transacao);
        log.info("Recebi pela fila a transação {} (reentrega: {})",
                transacao.transacaoId(), Boolean.TRUE.equals(reentrega) ? "sim" : "não");
        try {
            if (servico.analisar(transacao)) {
                log.info("Analisei e gravei a transação {}", transacao.transacaoId());
            } else {
                log.info("A transação {} já estava analisada: mensagem repetida, nada a fazer", transacao.transacaoId());
            }
        } catch (DataIntegrityViolationException chegaramJuntas) {
            log.info("A transação {} já estava analisada (duas cópias ao mesmo tempo): nada a fazer",
                    transacao.transacaoId());
        }
    }

    /** Mensagem com defeito: tentar de novo nunca vai resolver. Vai direto para a DLQ. */
    private void validar(TransacaoRecebida t) {
        if (t == null || t.transacaoId() == null || t.contaOrigem() == null || t.contaDestino() == null
                || t.valor() == null || t.dataHora() == null) {
            throw new AmqpRejectAndDontRequeueException("mensagem incompleta: " + t);
        }
    }
}
