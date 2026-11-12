package tech.ada.antifraude.analise;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
class AnaliseControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AnaliseRepository repositorio;

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
