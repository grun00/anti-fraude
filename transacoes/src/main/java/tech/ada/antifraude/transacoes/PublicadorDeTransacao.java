package tech.ada.antifraude.transacoes;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tech.ada.antifraude.contracts.FilaDeAnalise;
import tech.ada.antifraude.contracts.TransacaoRecebida;

/**
 * Publica a transação na fila do antifraude e espera o RabbitMQ confirmar que recebeu
 * (publisher confirm). Se ele não confirmar, lança AmqpException: quem chamou desfaz a gravação.
 */
@Component
public class PublicadorDeTransacao {

    private static final long ESPERA_DA_CONFIRMACAO_MS = 5_000;

    private final RabbitTemplate rabbit;

    public PublicadorDeTransacao(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publicar(TransacaoRecebida mensagem) {
        rabbit.invoke(operacoes -> {
            operacoes.convertAndSend(FilaDeAnalise.EXCHANGE, FilaDeAnalise.ROUTING_KEY, mensagem);
            operacoes.waitForConfirmsOrDie(ESPERA_DA_CONFIRMACAO_MS);
            return null;
        });
    }
}
