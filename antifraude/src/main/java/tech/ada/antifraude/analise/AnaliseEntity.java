package tech.ada.antifraude.analise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O resultado da análise de uma transação. Só o antifraude lê e escreve esta tabela (ADR-001).
 *
 * O id da transação é a chave primária: uma transação tem no máximo uma análise. É isso que
 * impede a mensagem repetida de gravar duas vezes (ADR-002).
 *
 * Implementa Persistable para o save() sempre fazer INSERT. Sem isso, como o id vem de fora,
 * o JPA faria um merge: a análise repetida SOBRESCREVERIA a primeira, em vez de o banco recusar.
 */
@Entity
@Table(name = "analise", indexes = @Index(columnList = "contaOrigem, dataHora"))
public class AnaliseEntity implements Persistable<UUID> {

    @Id
    private UUID transacaoId;

    /** A conta e a hora da transação: a regra de frequência conta as transações recentes da conta. */
    @Column(nullable = false)
    private String contaOrigem;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resultado resultado;

    private String motivos;

    @Column(nullable = false)
    private LocalDateTime analisadaEm;

    /** Preenchidos quando um analista revisa uma análise EM_REVISAO. */
    private String revisadaPor;

    private LocalDateTime revisadaEm;

    @Transient
    private boolean nova = true;

    protected AnaliseEntity() {
        // para o JPA
    }

    public AnaliseEntity(UUID transacaoId, String contaOrigem, LocalDateTime dataHora, int score,
                         Resultado resultado, String motivos, LocalDateTime analisadaEm) {
        this.transacaoId = transacaoId;
        this.contaOrigem = contaOrigem;
        this.dataHora = dataHora;
        this.score = score;
        this.resultado = resultado;
        this.motivos = motivos;
        this.analisadaEm = analisadaEm;
    }

    @Override
    public UUID getId() { return transacaoId; }

    @Override
    public boolean isNew() { return nova; }

    @PostLoad
    @PostPersist
    void jaEstaNoBanco() { nova = false; }

    /** O analista troca o resultado de uma análise EM_REVISAO por APROVADA ou REPROVADA. */
    public void revisar(Resultado novoResultado, String analista, LocalDateTime quando) {
        if (resultado != Resultado.EM_REVISAO) {
            throw new RevisaoNaoPermitidaException("a análise " + transacaoId + " está " + resultado
                    + ": só uma análise EM_REVISAO pode ser revisada");
        }
        if (novoResultado == Resultado.EM_REVISAO) {
            throw new RevisaoNaoPermitidaException("a revisão precisa decidir: APROVADA ou REPROVADA");
        }
        this.resultado = novoResultado;
        this.revisadaPor = analista;
        this.revisadaEm = quando;
    }

    public UUID getTransacaoId() { return transacaoId; }
    public String getContaOrigem() { return contaOrigem; }
    public LocalDateTime getDataHora() { return dataHora; }
    public int getScore() { return score; }
    public Resultado getResultado() { return resultado; }
    public String getMotivos() { return motivos; }
    public LocalDateTime getAnalisadaEm() { return analisadaEm; }
    public String getRevisadaPor() { return revisadaPor; }
    public LocalDateTime getRevisadaEm() { return revisadaEm; }
}
