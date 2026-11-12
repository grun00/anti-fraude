package tech.ada.antifraude.transacoes;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recebe a transação, grava no banco do transacoes e responde 202: a análise acontece depois,
 * no antifraude. O resultado se consulta em GET /analises/{id}, no antifraude.
 *
 * TODO (parte 2): publicar a transação na fila do antifraude.
 */
@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    private static final Logger log = LoggerFactory.getLogger(TransacaoController.class);

    private final TransacaoRepository repositorio;

    public TransacaoController(TransacaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @PostMapping
    public ResponseEntity<TransacaoView> receber(@Valid @RequestBody TransacaoRequest pedido) {
        TransacaoEntity transacao = repositorio.save(new TransacaoEntity(UUID.randomUUID(),
                pedido.contaOrigem(), pedido.contaDestino(), pedido.valor(), pedido.dataHora(),
                LocalDateTime.now()));
        log.info("Recebi a transação {} de R$ {}", transacao.getId(), transacao.getValor());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(TransacaoView.de(transacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoView> buscar(@PathVariable UUID id) {
        return repositorio.findById(id)
                .map(TransacaoView::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
