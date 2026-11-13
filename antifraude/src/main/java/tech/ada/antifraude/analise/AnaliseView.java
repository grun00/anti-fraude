package tech.ada.antifraude.analise;

import java.time.LocalDateTime;
import java.util.UUID;

/** A resposta do GET /analises/{transacaoId}. É também o que fica guardado no cache. */
public record AnaliseView(
        UUID transacaoId,
        int score,
        Resultado resultado,
        String motivos,
        LocalDateTime analisadaEm,
        String revisadaPor,
        LocalDateTime revisadaEm) {

    static AnaliseView de(AnaliseEntity a) {
        return new AnaliseView(a.getTransacaoId(), a.getScore(), a.getResultado(), a.getMotivos(),
                a.getAnalisadaEm(), a.getRevisadaPor(), a.getRevisadaEm());
    }
}
