package tech.ada.antifraude.analise;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A análise não pode ser revisada (não está EM_REVISAO, ou a revisão não decidiu). Vira 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class RevisaoNaoPermitidaException extends RuntimeException {

    public RevisaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
