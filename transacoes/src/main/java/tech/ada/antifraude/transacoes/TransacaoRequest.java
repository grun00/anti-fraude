package tech.ada.antifraude.transacoes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** O corpo do POST /transacoes. */
public record TransacaoRequest(
        @NotBlank String contaOrigem,
        @NotBlank String contaDestino,
        @NotNull @Positive BigDecimal valor,
        @NotNull LocalDateTime dataHora) {
}
