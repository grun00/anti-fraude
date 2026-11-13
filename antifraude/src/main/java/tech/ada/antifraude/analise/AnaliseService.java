package tech.ada.antifraude.analise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ada.antifraude.contracts.TransacaoRecebida;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Analisa a transação e grava o resultado, uma vez só por transação (ADR-002).
 * Responde a consulta do resultado, com cache, e a revisão do analista (ADR-003).
 */
@Service
public class AnaliseService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseService.class);

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

    /**
     * Cache-aside: procura no Redis primeiro; se não acha, lê do banco e guarda a cópia.
     * Vazio (ainda não analisada) não vai para o cache: daqui a pouco a análise existe, e quem
     * consultar de novo precisa achar.
     */
    @Cacheable(value = CacheConfig.ANALISES, unless = "#result == null")
    public Optional<AnaliseView> buscar(UUID transacaoId) {
        log.info("Indo ao banco buscar a análise {}", transacaoId);
        return analises.findById(transacaoId).map(AnaliseView::de);
    }

    /**
     * O analista decide uma análise EM_REVISAO. O dado mudou: a cópia do cache é apagada.
     *
     * Sem @Transactional aqui de propósito: o save() faz o commit sozinho, e o @CacheEvict roda
     * depois que o método termina, ou seja, DEPOIS do commit. Assim, uma consulta no meio do
     * caminho não guarda de novo no cache o resultado antigo.
     *
     * @return vazio se a transação ainda não tem análise
     */
    @CacheEvict(value = CacheConfig.ANALISES, key = "#transacaoId")
    public Optional<AnaliseView> revisar(UUID transacaoId, RevisaoRequest revisao) {
        return analises.findById(transacaoId).map(analise -> {
            analise.revisar(revisao.resultado(), revisao.analista(), LocalDateTime.now());
            AnaliseView revisada = AnaliseView.de(analises.save(analise));
            log.info("{} revisou a análise {}: {}. Cópia do cache apagada", revisao.analista(),
                    transacaoId, revisao.resultado());
            return revisada;
        });
    }
}
