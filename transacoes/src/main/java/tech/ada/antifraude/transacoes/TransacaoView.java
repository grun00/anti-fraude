package tech.ada.antifraude.transacoes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** A resposta do POST e do GET /transacoes. */
public record TransacaoView(
        UUID transacaoId,
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        LocalDateTime dataHora,
        LocalDateTime recebidaEm) {

    static TransacaoView de(TransacaoEntity t) {
        return new TransacaoView(t.getId(), t.getContaOrigem(), t.getContaDestino(), t.getValor(),
                t.getDataHora(), t.getRecebidaEm());
    }
}
