package tech.ada.antifraude.transacoes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** A transação como chegou do cliente. Só o transacoes lê e escreve esta tabela (ADR-001). */
@Entity
@Table(name = "transacao")
public class TransacaoEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String contaOrigem;

    @Column(nullable = false)
    private String contaDestino;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private LocalDateTime recebidaEm;

    protected TransacaoEntity() {
        // para o JPA
    }

    public TransacaoEntity(UUID id, String contaOrigem, String contaDestino, BigDecimal valor,
                           LocalDateTime dataHora, LocalDateTime recebidaEm) {
        this.id = id;
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.dataHora = dataHora;
        this.recebidaEm = recebidaEm;
    }

    public UUID getId() { return id; }
    public String getContaOrigem() { return contaOrigem; }
    public String getContaDestino() { return contaDestino; }
    public BigDecimal getValor() { return valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public LocalDateTime getRecebidaEm() { return recebidaEm; }
}
