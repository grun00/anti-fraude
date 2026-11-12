package tech.ada.antifraude.analise;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A consulta do resultado da análise. 404 enquanto a transação ainda não foi analisada.
 *
 * TODO (parte 3): pôr o cache na leitura.
 */
@RestController
@RequestMapping("/analises")
public class AnaliseController {

    private final AnaliseRepository repositorio;

    public AnaliseController(AnaliseRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping("/{transacaoId}")
    public ResponseEntity<AnaliseView> buscar(@PathVariable UUID transacaoId) {
        return repositorio.findById(transacaoId)
                .map(AnaliseView::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
