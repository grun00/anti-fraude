package tech.ada.antifraude.analise;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A consulta do resultado da análise (com cache) e a revisão do analista.
 * 404 enquanto a transação ainda não foi analisada.
 */
@RestController
@RequestMapping("/analises")
public class AnaliseController {

    private static final Logger log = LoggerFactory.getLogger(AnaliseController.class);

    private final AnaliseService servico;

    public AnaliseController(AnaliseService servico) {
        this.servico = servico;
    }

    @GetMapping("/{transacaoId}")
    public ResponseEntity<AnaliseView> buscar(@PathVariable UUID transacaoId) {
        log.info("Pediram a análise {}", transacaoId);
        return servico.buscar(transacaoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{transacaoId}/revisao")
    public ResponseEntity<AnaliseView> revisar(@PathVariable UUID transacaoId,
                                               @Valid @RequestBody RevisaoRequest revisao) {
        return servico.revisar(transacaoId, revisao)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
