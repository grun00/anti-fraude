package tech.ada.antifraude.contracts;

/**
 * Os nomes do caminho da mensagem no RabbitMQ. Os dois serviços usam os mesmos nomes.
 *
 * <pre>
 *   analise.ex ──"analise"──► analise.q ──► antifraude
 *                                 │ falhou de vez
 *                                 ▼
 *   analise.dlx ──"analise"──► analise.dlq   (alguém olha)
 * </pre>
 */
public final class FilaDeAnalise {

    /** A exchange onde o transacoes publica. */
    public static final String EXCHANGE = "analise.ex";

    /** A routing key de cada mensagem. */
    public static final String ROUTING_KEY = "analise";

    /** A fila onde a transação espera o antifraude. */
    public static final String FILA = "analise.q";

    /** A exchange que recebe as mensagens que falharam de vez. */
    public static final String DLX = "analise.dlx";

    /** A DLQ: as mensagens que falharam de vez ficam aqui, para alguém olhar. */
    public static final String DLQ = "analise.dlq";

    private FilaDeAnalise() {
        // só constantes
    }
}
