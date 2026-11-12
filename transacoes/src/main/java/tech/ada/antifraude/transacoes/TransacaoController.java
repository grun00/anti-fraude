package tech.ada.antifraude.transacoes;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Recebe a transação, grava, publica na fila do antifraude e responde 202: a análise acontece
 * depois, no antifraude. O resultado se consulta em GET /analises/{id}, no antifraude.
 */
@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    private static final Logger log = LoggerFactory.getLogger(TransacaoController.class);

    private final TransacaoService servico;

    public TransacaoController(TransacaoService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<TransacaoView> receber(@Valid @RequestBody TransacaoRequest pedido) {
        TransacaoEntity transacao = servico.receber(pedido);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(TransacaoView.de(transacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoView> buscar(@PathVariable UUID id) {
        return servico.buscar(id)
                .map(TransacaoView::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** A fila não confirmou a mensagem: a gravação foi desfeita e o cliente pode tentar de novo. */
    @ExceptionHandler(AmqpException.class)
    public ProblemDetail filaForaDoAr(AmqpException erro) {
        log.warn("Não consegui publicar na fila do antifraude: {}. Transação não gravada.", erro.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "A fila do antifraude não respondeu. A transação não foi gravada: tente de novo.");
    }
}
