package tech.ada.antifraude.analise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * O cache não pode derrubar a consulta: se o Redis não responder, o antifraude segue sem cache
 * e lê direto do banco. Fica mais lento, mas não erra: o cache é só uma cópia.
 */
@Configuration
public class CacheTolerante implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheTolerante.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException erro, Cache cache, Object chave) {
                log.warn("Cache fora do ar. Vou direto ao banco (análise {})", chave);
            }

            @Override
            public void handleCachePutError(RuntimeException erro, Cache cache, Object chave, Object valor) {
                log.warn("Cache fora do ar. Não deu para guardar a análise {}", chave);
            }

            @Override
            public void handleCacheEvictError(RuntimeException erro, Cache cache, Object chave) {
                log.warn("Cache fora do ar. Não deu para apagar a análise {}: a cópia velha expira pelo TTL", chave);
            }
        };
    }
}
