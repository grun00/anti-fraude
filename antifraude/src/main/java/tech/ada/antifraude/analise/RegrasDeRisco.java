package tech.ada.antifraude.analise;

import tech.ada.antifraude.contracts.TransacaoRecebida;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * As regras de pontuação. Sem banco e sem Spring: recebe tudo pronto e só calcula.
 *
 * <pre>
 *   valor acima de R$ 5.000                          +50
 *   entre 0h e 6h                                    +20
 *   conta de destino na lista negra                 +100
 *   mais de 3 transações da conta em 10 minutos      +30
 *
 *   menos de 40: APROVADA | 40 a 69: EM_REVISAO | 70 ou mais: REPROVADA
 * </pre>
 */
public final class RegrasDeRisco {

    static final BigDecimal VALOR_ALTO = new BigDecimal("5000");
    static final int HORA_FIM_DA_MADRUGADA = 6;
    static final int LIMITE_DE_TRANSACOES_RECENTES = 3;

    private RegrasDeRisco() {
    }

    /** O resultado do cálculo: a pontuação, o veredito e os motivos, para o analista entender. */
    public record Avaliacao(int score, Resultado resultado, String motivos) {
    }

    /**
     * @param destinoNaListaNegra se a conta de destino está na lista negra
     * @param transacoesRecentes  quantas transações da mesma conta de origem já foram analisadas
     *                            nos 10 minutos antes desta (sem contar esta)
     */
    public static Avaliacao avaliar(TransacaoRecebida t, boolean destinoNaListaNegra, long transacoesRecentes) {
        int score = 0;
        List<String> motivos = new ArrayList<>();

        if (t.valor().compareTo(VALOR_ALTO) > 0) {
            score += 50;
            motivos.add("valor acima de R$ 5.000");
        }
        if (t.dataHora().getHour() < HORA_FIM_DA_MADRUGADA) {
            score += 20;
            motivos.add("de madrugada");
        }
        if (destinoNaListaNegra) {
            score += 100;
            motivos.add("conta de destino na lista negra");
        }
        if (transacoesRecentes >= LIMITE_DE_TRANSACOES_RECENTES) {
            score += 30;
            motivos.add("mais de 3 transações da conta em 10 minutos");
        }

        return new Avaliacao(score, resultadoPara(score), motivos.isEmpty() ? null : String.join("; ", motivos));
    }

    static Resultado resultadoPara(int score) {
        if (score >= 70) {
            return Resultado.REPROVADA;
        }
        if (score >= 40) {
            return Resultado.EM_REVISAO;
        }
        return Resultado.APROVADA;
    }
}
