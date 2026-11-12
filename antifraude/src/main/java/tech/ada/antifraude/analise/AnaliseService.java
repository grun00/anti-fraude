package tech.ada.antifraude.analise;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ada.antifraude.contracts.TransacaoRecebida;

import java.time.Duration;
import java.time.LocalDateTime;

/** Analisa a transação e grava o resultado, uma vez só por transação (ADR-002). */
@Service
public class AnaliseService {

    static final Duration JANELA_DE_FREQUENCIA = Duration.ofMinutes(10);

    private final AnaliseRepository analises;
    private final ListaNegraRepository listaNegra;

    public AnaliseService(AnaliseRepository analises, ListaNegraRepository listaNegra) {
        this.analises = analises;
        this.listaNegra = listaNegra;
    }

    /**
     * @return true se gravou; false se essa transação já tinha análise (mensagem repetida).
     * @throws org.springframework.dao.DataIntegrityViolationException se duas cópias da mesma
     *         mensagem chegarem ao mesmo tempo: as duas passam pelo existsById, mas o banco
     *         recusa a segunda, porque o transacaoId é a chave primária.
     */
    @Transactional
    public boolean analisar(TransacaoRecebida t) {
        if (analises.existsById(t.transacaoId())) {
            return false;
        }
        long recentes = analises.countByContaOrigemAndDataHoraBetween(
                t.contaOrigem(), t.dataHora().minus(JANELA_DE_FREQUENCIA), t.dataHora());
        RegrasDeRisco.Avaliacao avaliacao = RegrasDeRisco.avaliar(t, listaNegra.existsById(t.contaDestino()), recentes);

        analises.saveAndFlush(new AnaliseEntity(t.transacaoId(), t.contaOrigem(), t.dataHora(),
                avaliacao.score(), avaliacao.resultado(), avaliacao.motivos(), LocalDateTime.now()));
        return true;
    }
}
