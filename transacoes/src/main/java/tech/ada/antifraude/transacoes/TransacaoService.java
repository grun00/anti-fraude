package tech.ada.antifraude.transacoes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ada.antifraude.contracts.TransacaoRecebida;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Grava a transação e publica na fila, dentro da mesma transação do banco (ADR-002).
 *
 * Se a publicação falhar, a exceção desfaz a gravação: não fica transação gravada que o
 * antifraude nunca vai ver. O cliente recebe 503 e pode mandar de novo.
 */
@Service
public class TransacaoService {

    private static final Logger log = LoggerFactory.getLogger(TransacaoService.class);

    private final TransacaoRepository repositorio;
    private final PublicadorDeTransacao publicador;

    public TransacaoService(TransacaoRepository repositorio, PublicadorDeTransacao publicador) {
        this.repositorio = repositorio;
        this.publicador = publicador;
    }

    @Transactional
    public TransacaoEntity receber(TransacaoRequest pedido) {
        TransacaoEntity transacao = repositorio.save(new TransacaoEntity(UUID.randomUUID(),
                pedido.contaOrigem(), pedido.contaDestino(), pedido.valor(), pedido.dataHora(),
                LocalDateTime.now()));
        publicador.publicar(new TransacaoRecebida(transacao.getId(), transacao.getContaOrigem(),
                transacao.getContaDestino(), transacao.getValor(), transacao.getDataHora()));
        log.info("Recebi a transação {} de R$ {} e publiquei na fila do antifraude",
                transacao.getId(), transacao.getValor());
        return transacao;
    }

    public Optional<TransacaoEntity> buscar(UUID id) {
        return repositorio.findById(id);
    }
}
