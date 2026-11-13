package tech.ada.antifraude.analise;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import tech.ada.antifraude.contracts.TransacaoRecebida;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chama o consumidor direto, sem RabbitMQ: o que importa aqui é o que ele grava. */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.cache.type=simple"})
class ConsumidorDeAnaliseTest {

    private static final LocalDateTime TARDE = LocalDateTime.of(2026, 11, 12, 14, 30);

    @Autowired
    ConsumidorDeAnalise consumidor;

    @Autowired
    AnaliseRepository analises;

    /** Cada teste usa a sua conta de origem: o banco é o mesmo para todos os testes. */
    private static TransacaoRecebida transacao(String contaOrigem, String contaDestino, String valor,
                                               LocalDateTime quando) {
        return new TransacaoRecebida(UUID.randomUUID(), contaOrigem, contaDestino, new BigDecimal(valor), quando);
    }

    @Test
    void mensagemRepetidaGravaUmaAnaliseSo() {
        TransacaoRecebida mensagem = transacao(UUID.randomUUID().toString(), "0002-456", "8000", TARDE);

        consumidor.receber(mensagem, false);
        AnaliseEntity primeira = analises.findById(mensagem.transacaoId()).orElseThrow();
        consumidor.receber(mensagem, true);

        assertThat(analises.findAll()).filteredOn(a -> a.getTransacaoId().equals(mensagem.transacaoId())).hasSize(1);
        AnaliseEntity depois = analises.findById(mensagem.transacaoId()).orElseThrow();
        assertThat(depois.getAnalisadaEm()).isEqualTo(primeira.getAnalisadaEm());
        assertThat(depois.getResultado()).isEqualTo(Resultado.EM_REVISAO);
    }

    @Test
    void oBancoRecusaAsegundaAnaliseDaMesmaTransacao() {
        UUID id = UUID.randomUUID();
        analises.saveAndFlush(new AnaliseEntity(id, "0001-123", TARDE, 0, Resultado.APROVADA, null, LocalDateTime.now()));

        assertThatThrownBy(() -> analises.saveAndFlush(
                new AnaliseEntity(id, "0001-123", TARDE, 100, Resultado.REPROVADA, null, LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(analises.findById(id).orElseThrow().getResultado()).isEqualTo(Resultado.APROVADA);
    }

    @Test
    void destinoNaListaNegraDoDataSqlEreprovado() {
        TransacaoRecebida mensagem = transacao(UUID.randomUUID().toString(), "6666-666", "10", TARDE);

        consumidor.receber(mensagem, false);

        AnaliseEntity analise = analises.findById(mensagem.transacaoId()).orElseThrow();
        assertThat(analise.getScore()).isEqualTo(100);
        assertThat(analise.getResultado()).isEqualTo(Resultado.REPROVADA);
    }

    @Test
    void aQuartaTransacaoDaContaEm10MinutosGanhaPontos() {
        String conta = UUID.randomUUID().toString();
        for (int minuto = 0; minuto < 3; minuto++) {
            consumidor.receber(transacao(conta, "0002-456", "10", TARDE.plusMinutes(minuto)), false);
        }
        TransacaoRecebida quarta = transacao(conta, "0002-456", "10", TARDE.plusMinutes(5));
        TransacaoRecebida forDaJanela = transacao(conta, "0002-456", "10", TARDE.plusMinutes(30));

        consumidor.receber(quarta, false);
        consumidor.receber(forDaJanela, false);

        assertThat(analises.findById(quarta.transacaoId()).orElseThrow().getScore()).isEqualTo(30);
        assertThat(analises.findById(forDaJanela.transacaoId()).orElseThrow().getScore()).isZero();
    }

    @Test
    void mensagemIncompletaVaiDiretoParaAdlq() {
        TransacaoRecebida semValor = new TransacaoRecebida(UUID.randomUUID(), "0001-123", "0002-456", null, TARDE);

        assertThatThrownBy(() -> consumidor.receber(semValor, false))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThat(analises.existsById(semValor.transacaoId())).isFalse();
    }
}
