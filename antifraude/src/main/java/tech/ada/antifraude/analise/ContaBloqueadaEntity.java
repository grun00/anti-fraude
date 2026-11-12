package tech.ada.antifraude.analise;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma conta na lista negra. As contas da demo são carregadas pelo data.sql. */
@Entity
@Table(name = "lista_negra")
public class ContaBloqueadaEntity {

    @Id
    private String conta;

    private String motivo;

    protected ContaBloqueadaEntity() {
        // para o JPA
    }

    public String getConta() { return conta; }
    public String getMotivo() { return motivo; }
}
