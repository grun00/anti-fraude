package tech.ada.antifraude.analise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** O corpo do PUT /analises/{transacaoId}/revisao. */
public record RevisaoRequest(@NotNull Resultado resultado, @NotBlank String analista) {
}
