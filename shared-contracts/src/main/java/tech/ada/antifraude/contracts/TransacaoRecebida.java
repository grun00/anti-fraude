package tech.ada.antifraude.contracts;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A mensagem da fila: "chegou esta transação, analise". O transacoes publica, o antifraude consome.
 *
 * Leva só o que a análise precisa. O transacaoId é a chave da idempotência: a mesma mensagem
 * pode chegar duas vezes, e o antifraude grava uma análise só por transação (ADR-002).
 */
public record TransacaoRecebida(
        UUID transacaoId,
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        LocalDateTime dataHora) {
}
