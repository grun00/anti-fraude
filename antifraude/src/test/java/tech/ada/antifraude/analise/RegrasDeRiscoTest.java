package tech.ada.antifraude.analise;

import org.junit.jupiter.api.Test;
import tech.ada.antifraude.contracts.TransacaoRecebida;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasDeRiscoTest {

    private static final LocalDateTime TARDE = LocalDateTime.of(2026, 11, 12, 14, 30);
    private static final LocalDateTime MADRUGADA = LocalDateTime.of(2026, 11, 12, 3, 0);

    private static TransacaoRecebida transacao(String valor, LocalDateTime quando) {
        return new TransacaoRecebida(UUID.randomUUID(), "0001-123", "0002-456", new BigDecimal(valor), quando);
    }

    @Test
    void transacaoComumEaprovadaSemMotivos() {
        var avaliacao = RegrasDeRisco.avaliar(transacao("150.00", TARDE), false, 0);

        assertThat(avaliacao.score()).isZero();
        assertThat(avaliacao.resultado()).isEqualTo(Resultado.APROVADA);
        assertThat(avaliacao.motivos()).isNull();
    }

    @Test
    void valorAltoSozinhoVaiParaRevisao() {
        var avaliacao = RegrasDeRisco.avaliar(transacao("5000.01", TARDE), false, 0);

        assertThat(avaliacao.score()).isEqualTo(50);
        assertThat(avaliacao.resultado()).isEqualTo(Resultado.EM_REVISAO);
        assertThat(avaliacao.motivos()).isEqualTo("valor acima de R$ 5.000");
    }

    @Test
    void cincoMilExatosNaoContaComoValorAlto() {
        assertThat(RegrasDeRisco.avaliar(transacao("5000.00", TARDE), false, 0).score()).isZero();
    }

    @Test
    void valorAltoDeMadrugadaEreprovada() {
        var avaliacao = RegrasDeRisco.avaliar(transacao("8000", MADRUGADA), false, 0);

        assertThat(avaliacao.score()).isEqualTo(70);
        assertThat(avaliacao.resultado()).isEqualTo(Resultado.REPROVADA);
        assertThat(avaliacao.motivos()).isEqualTo("valor acima de R$ 5.000; de madrugada");
    }

    @Test
    void madrugadaVaiAteAs5e59() {
        assertThat(RegrasDeRisco.avaliar(transacao("10", LocalDateTime.of(2026, 11, 12, 5, 59)), false, 0).score())
                .isEqualTo(20);
        assertThat(RegrasDeRisco.avaliar(transacao("10", LocalDateTime.of(2026, 11, 12, 6, 0)), false, 0).score())
                .isZero();
    }

    @Test
    void destinoNaListaNegraSozinhoJaReprova() {
        var avaliacao = RegrasDeRisco.avaliar(transacao("10", TARDE), true, 0);

        assertThat(avaliacao.score()).isEqualTo(100);
        assertThat(avaliacao.resultado()).isEqualTo(Resultado.REPROVADA);
    }

    @Test
    void aQuartaTransacaoEm10MinutosSomaPontos() {
        assertThat(RegrasDeRisco.avaliar(transacao("10", TARDE), false, 2).score()).isZero();
        assertThat(RegrasDeRisco.avaliar(transacao("10", TARDE), false, 3).score()).isEqualTo(30);
    }

    @Test
    void limitesDoResultado() {
        assertThat(RegrasDeRisco.resultadoPara(39)).isEqualTo(Resultado.APROVADA);
        assertThat(RegrasDeRisco.resultadoPara(40)).isEqualTo(Resultado.EM_REVISAO);
        assertThat(RegrasDeRisco.resultadoPara(69)).isEqualTo(Resultado.EM_REVISAO);
        assertThat(RegrasDeRisco.resultadoPara(70)).isEqualTo(Resultado.REPROVADA);
    }
}
