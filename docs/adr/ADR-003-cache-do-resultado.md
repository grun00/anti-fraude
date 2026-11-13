# ADR-003: cache do resultado da análise no Redis

## Contexto
A consulta do resultado (`GET /analises/{id}`) é a leitura mais chamada: o cliente, o app e o
atendimento abrem o resultado logo depois da transação, e mais de uma vez. Sem cache, toda
consulta vai ao banco do antifraude, o mesmo que grava as análises que chegam pela fila.

O resultado quase não muda: só muda quando um analista revisa uma análise `EM_REVISAO`.

## Decisão
1. **O que guarda:** o resultado da análise (`AnaliseView`, em JSON), na chave
   `analises::<id da transação>`. **Cache-aside** com `@Cacheable`: o antifraude procura no
   Redis; se não acha, lê do banco e guarda a cópia.
2. **Por quanto tempo:** TTL de **1 hora** (`antifraude.cache.ttl` no `application.yml`). Cobre
   a janela em que o resultado é mais consultado; depois, a chave sai do Redis sozinha e não
   ocupa memória com análises que ninguém mais abre.
3. **Quando apaga:** quando o analista revisa (`PUT /analises/{id}/revisao`), o `@CacheEvict`
   apaga a cópia. A próxima consulta vai ao banco e guarda o resultado novo. O evict roda depois
   do commit da revisão, para uma consulta no meio do caminho não guardar o resultado antigo.
4. **O que nunca guarda:** o "ainda não analisada" (`404`), com `unless = "#result == null"`.
   Logo depois do `202`, a análise ainda está na fila (ADR-002): guardar o `404` faria o cliente
   ver "não encontrada" por 1 hora, mesmo com a análise já gravada.
5. **Redis fora do ar:** o `CacheTolerante` registra um aviso e a consulta lê direto do banco.
   Fica mais lenta (até 1 s de espera por tentativa no Redis), mas não erra: o cache é só uma cópia.

## Alternativas consideradas
- **Sem cache**: mais simples, sempre atual. Mas toda leitura repetida vai ao banco, que também
  recebe as gravações da fila. **Rejeitada.**
- **Cache local (um `Map` dentro do antifraude, ou Caffeine)**: mais rápido e sem servidor a
  mais. Mas se perde a cada reinício, e com várias instâncias cada uma teria a sua cópia: o
  evict da revisão só apagaria a cópia de uma delas, e as outras mostrariam o resultado velho.
  **Rejeitada.**
- **Atualizar o cache na revisão (`@CachePut`) em vez de apagar**: evita o miss seguinte. Mas
  apagar é mais simples e mais seguro: a próxima leitura sempre vem do banco, a fonte da verdade.
  **Rejeitada.**
- **TTL curto (60 s) ou longo (1 dia)**: com o evict cuidando da revisão, o TTL só é uma rede de
  segurança. 60 s faria muitos miss; 1 dia guardaria muita análise que ninguém abre. **1 hora.**

## Consequências
- A segunda consulta do mesmo id vem do Redis em poucos milissegundos, sem ir ao banco.
- **Só a revisão pela API apaga o cache.** Uma mudança direto no banco (um `UPDATE` à mão) fica
  invisível por até 1 hora, até o TTL vencer, ou até alguém rodar `DEL` na chave.
- Se o evict falhar (Redis fora no momento da revisão), a cópia velha vale até o TTL: no
  máximo 1 hora mostrando o resultado de antes da revisão.
- Se o Redis cair, todas as consultas vão ao banco. No pico, isso aumenta a carga dele.

## Status
Aceita
