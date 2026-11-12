package tech.ada.antifraude.transacoes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tech.ada.antifraude.contracts.TransacaoRecebida;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransacaoControllerTest {

    private static final String TRANSACAO = """
            {"conta_origem":"0001-123","conta_destino":"0002-456","valor":150.00,
             "data_hora":"2026-11-12T14:30:00"}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    TransacaoRepository repositorio;

    @MockBean
    PublicadorDeTransacao publicador;

    @Test
    void recebeAtransacaoPublicaNaFilaResponde202EdepoisAchaPeloId() throws Exception {
        String resposta = mvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(TRANSACAO))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        JsonNode corpo = json.readTree(resposta);
        UUID id = UUID.fromString(corpo.get("transacao_id").asText());

        ArgumentCaptor<TransacaoRecebida> mensagem = ArgumentCaptor.forClass(TransacaoRecebida.class);
        verify(publicador).publicar(mensagem.capture());
        assertThat(mensagem.getValue().transacaoId()).isEqualTo(id);
        assertThat(mensagem.getValue().contaDestino()).isEqualTo("0002-456");
        assertThat(mensagem.getValue().valor()).isEqualByComparingTo(new BigDecimal("150.00"));

        mvc.perform(get("/transacoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conta_destino").value("0002-456"))
                .andExpect(jsonPath("$.valor").value(150.00));
    }

    @Test
    void filaForaDoArResponde503EnaoGravaAtransacao() throws Exception {
        long antes = repositorio.count();
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(publicador).publicar(any());

        mvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(TRANSACAO))
                .andExpect(status().isServiceUnavailable());

        assertThat(repositorio.count()).isEqualTo(antes);
    }

    @Test
    void recusaTransacaoComValorNegativo() throws Exception {
        String invalida = TRANSACAO.replace("150.00", "-1");
        mvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transacaoQueNaoExisteDa404() throws Exception {
        mvc.perform(get("/transacoes/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
