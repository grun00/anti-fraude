package tech.ada.antifraude.analise;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O cache aqui é o da memória (spring.cache.type=simple), no lugar do Redis: as anotações são as mesmas. */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.cache.type=simple"})
@AutoConfigureMockMvc
class AnaliseControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AnaliseRepository repositorio;

    @Autowired
    CacheManager caches;

    private Cache cache() {
        return caches.getCache(CacheConfig.ANALISES);
    }

    private UUID gravar(int score, Resultado resultado) {
        UUID id = UUID.randomUUID();
        repositorio.save(new AnaliseEntity(id, "0001-123", LocalDateTime.now(), score, resultado, null,
                LocalDateTime.now()));
        return id;
    }

    private static String revisao(String resultado) {
        return "{\"resultado\":\"" + resultado + "\",\"analista\":\"ana\"}";
    }

    @Test
    void aSegundaLeituraVemDoCacheSemIrAoBanco() throws Exception {
        UUID id = gravar(0, Resultado.APROVADA);
        mvc.perform(get("/analises/{id}", id)).andExpect(status().isOk());
        assertThat(cache().get(id)).isNotNull();

        repositorio.deleteById(id);   // se a leitura fosse ao banco, daria 404

        mvc.perform(get("/analises/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("APROVADA"));
    }

    @Test
    void oNaoAcheiNaoFicaNoCache() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/analises/{id}", id)).andExpect(status().isNotFound());
        assertThat(cache().get(id)).isNull();

        repositorio.save(new AnaliseEntity(id, "0001-123", LocalDateTime.now(), 0, Resultado.APROVADA, null,
                LocalDateTime.now()));   // a análise chegou pela fila logo depois

        mvc.perform(get("/analises/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void aRevisaoApagaAcopiaDoCache() throws Exception {
        UUID id = gravar(50, Resultado.EM_REVISAO);
        mvc.perform(get("/analises/{id}", id)).andExpect(jsonPath("$.resultado").value("EM_REVISAO"));
        assertThat(cache().get(id)).isNotNull();

        mvc.perform(put("/analises/{id}/revisao", id).contentType(MediaType.APPLICATION_JSON).content(revisao("APROVADA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("APROVADA"))
                .andExpect(jsonPath("$.revisada_por").value("ana"));
        assertThat(cache().get(id)).isNull();

        mvc.perform(get("/analises/{id}", id))
                .andExpect(jsonPath("$.resultado").value("APROVADA"))
                .andExpect(jsonPath("$.score").value(50));
    }

    @Test
    void soAnaliseEmRevisaoPodeSerRevisada() throws Exception {
        UUID aprovada = gravar(0, Resultado.APROVADA);
        mvc.perform(put("/analises/{id}/revisao", aprovada).contentType(MediaType.APPLICATION_JSON).content(revisao("REPROVADA")))
                .andExpect(status().isConflict());

        UUID emRevisao = gravar(50, Resultado.EM_REVISAO);
        mvc.perform(put("/analises/{id}/revisao", emRevisao).contentType(MediaType.APPLICATION_JSON).content(revisao("EM_REVISAO")))
                .andExpect(status().isConflict());
        assertThat(repositorio.findById(aprovada).orElseThrow().getResultado()).isEqualTo(Resultado.APROVADA);
    }

    @Test
    void revisarAnaliseQueNaoExisteDa404() throws Exception {
        mvc.perform(put("/analises/{id}/revisao", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content(revisao("APROVADA")))
                .andExpect(status().isNotFound());
    }

    @Test
    void devolveAanaliseGravada() throws Exception {
        UUID id = UUID.randomUUID();
        repositorio.save(new AnaliseEntity(id, "0001-123", LocalDateTime.now(), 70, Resultado.REPROVADA,
                "valor acima de R$ 5.000; de madrugada", LocalDateTime.now()));

        mvc.perform(get("/analises/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(70))
                .andExpect(jsonPath("$.resultado").value("REPROVADA"));
    }

    @Test
    void transacaoAindaNaoAnalisadaDa404() throws Exception {
        mvc.perform(get("/analises/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
