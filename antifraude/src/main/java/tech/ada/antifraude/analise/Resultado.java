package tech.ada.antifraude.analise;

/** O veredito da análise, pela pontuação de risco: < 40, 40 a 69, 70 ou mais. */
public enum Resultado {
    APROVADA,
    EM_REVISAO,
    REPROVADA
}
