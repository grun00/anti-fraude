package tech.ada.antifraude.analise;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.ada.antifraude.contracts.FilaDeAnalise;

/**
 * O caminho da mensagem no RabbitMQ (veja o desenho em {@link FilaDeAnalise}).
 *
 * Os dois serviços declaram AS MESMAS peças, com os mesmos argumentos: não importa quem sobe
 * primeiro, o caminho existe antes da primeira mensagem. Mudou aqui, muda também no
 * transacoes (FilasConfig.java), senão o RabbitMQ recusa (PRECONDITION_FAILED).
 */
@Configuration
public class FilasConfig {

    @Bean
    DirectExchange exchangeDeAnalise() {
        return new DirectExchange(FilaDeAnalise.EXCHANGE);
    }

    /** "durable": a fila sobrevive a um reinício do RabbitMQ. O que falhar de vez vai para a DLX. */
    @Bean
    Queue filaDeAnalise() {
        return QueueBuilder.durable(FilaDeAnalise.FILA)
                .deadLetterExchange(FilaDeAnalise.DLX)
                .build();
    }

    @Bean
    Binding ligacaoDaFila() {
        return BindingBuilder.bind(filaDeAnalise()).to(exchangeDeAnalise()).with(FilaDeAnalise.ROUTING_KEY);
    }

    @Bean
    DirectExchange exchangeDaDlq() {
        return new DirectExchange(FilaDeAnalise.DLX);
    }

    @Bean
    Queue dlq() {
        return QueueBuilder.durable(FilaDeAnalise.DLQ).build();
    }

    @Bean
    Binding ligacaoDaDlq() {
        return BindingBuilder.bind(dlq()).to(exchangeDaDlq()).with(FilaDeAnalise.ROUTING_KEY);
    }

    /** As mensagens vão em JSON, com o mesmo Jackson do HTTP (snake_case). */
    @Bean
    MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
