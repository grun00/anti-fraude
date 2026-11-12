package tech.ada.antifraude.analise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O resultado da análise de uma transação. Só o antifraude lê e escreve esta tabela (ADR-001).
 *
 * O id da transação é a chave primária: uma transação tem no máximo uma análise. É isso que vai
 * impedir a mensagem repetida de gravar duas vezes (parte 2).
 */
@Entity
@Table(name = "analise")
public class AnaliseEntity {

    @Id
    private UUID transacaoId;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resultado resultado;

    private String motivos;

    @Column(nullable = false)
    private LocalDateTime analisadaEm;

    protected AnaliseEntity() {
        // para o JPA
    }

    public AnaliseEntity(UUID transacaoId, int score, Resultado resultado, String motivos,
                         LocalDateTime analisadaEm) {
        this.transacaoId = transacaoId;
        this.score = score;
        this.resultado = resultado;
        this.motivos = motivos;
        this.analisadaEm = analisadaEm;
    }

    public UUID getTransacaoId() { return transacaoId; }
    public int getScore() { return score; }
    public Resultado getResultado() { return resultado; }
    public String getMotivos() { return motivos; }
    public LocalDateTime getAnalisadaEm() { return analisadaEm; }
}
