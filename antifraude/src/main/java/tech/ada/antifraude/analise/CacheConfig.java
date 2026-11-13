package tech.ada.antifraude.analise;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/**
 * Como o resultado da análise fica no Redis (ADR-003).
 *
 * - A chave é "analises::" + o id da transação.
 * - O valor vai em JSON, para dar para ler no redis-cli.
 * - Cada análise fica no cache por antifraude.cache.ttl (o TTL). Depois, o Redis apaga sozinho.
 */
@Configuration
public class CacheConfig {

    public static final String ANALISES = "analises";

    @Bean
    public RedisCacheConfiguration comoGuardarNoRedis(ObjectMapper jackson,
                                                      @Value("${antifraude.cache.ttl}") Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(jackson, AnaliseView.class)));
    }
}
