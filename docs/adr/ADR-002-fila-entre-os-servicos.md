# ADR-002: fila (RabbitMQ) entre os serviços, com consumidor idempotente

## Contexto
O transacoes precisa entregar cada transação ao antifraude (ADR-001). O cliente não pode
esperar a análise, e o transacoes não pode parar de receber transações se o antifraude estiver
lento ou fora do ar. Também não podemos perder transação nenhuma: uma transação que nunca foi
analisada é uma fraude que passou sem ninguém olhar.

## Decisão
1. **Fila no RabbitMQ.** O transacoes publica a mensagem `TransacaoRecebida` na exchange
   `analise.ex`, e o antifraude consome da fila `analise.q` (durável). O contrato (a mensagem e os
   nomes) fica no módulo `shared-contracts`.
2. **Garantia "pelo menos uma vez".** O antifraude só manda o ack depois de gravar a análise.
   Se cair antes, o RabbitMQ entrega a mesma mensagem de novo: ela pode chegar repetida, mas não
   se perde.
3. **Consumidor idempotente.** O id da transação é a chave primária da tabela `analise`. Antes
   de gravar, o antifraude confere se o id já existe; se existe, ignora a mensagem e manda o ack.
   Se duas cópias chegarem ao mesmo tempo e as duas passarem pela conferência, o banco recusa a
   segunda (chave duplicada) e o consumidor trata como repetida.
4. **Gravar e publicar na mesma transação do banco.** O transacoes grava, publica e espera o
   RabbitMQ confirmar (publisher confirm). Se a publicação falhar, a gravação é desfeita e o
   cliente recebe `503`: não fica transação gravada que o antifraude nunca vai ver.
5. **Retry e DLQ.** Se a análise der erro, o antifraude tenta 3 vezes (espera 1 s e depois 2 s).
   Se falhar de vez, ou se a mensagem vier incompleta, ela vai para a `analise.dlq`, para alguém olhar.

## Alternativas consideradas
- **HTTP síncrono (o transacoes chama o antifraude)**: mais simples, sem broker. Mas o cliente
  esperaria a análise, e com o antifraude fora do ar o transacoes teria que recusar transações
  ou guardá-las por conta própria. **Rejeitada.**
- **Kafka**: guarda o histórico e permite reprocessar tudo, bom para vários consumidores. Aqui é
  um consumidor só, um comando de cada vez: o RabbitMQ resolve com menos peças, e foi o que vimos
  na Aula 5. **Rejeitada**, pode voltar se outros serviços precisarem das transações.
- **Idempotência com uma tabela de mensagens processadas**: funciona para qualquer mensagem, mas é
  uma tabela a mais. Como cada transação gera exatamente uma análise, a chave primária da própria
  `analise` já resolve. **Rejeitada.**
- **Outbox (gravar a mensagem numa tabela e publicar depois)**: a solução completa para o
  "gravou mas não publicou". Precisa de uma tabela e de um processo que publique. **Rejeitada por
  ora**: com a gravação e a publicação na mesma transação, o pior caso que sobra é o contrário
  (publicou e o commit falhou), raro, e o antifraude só analisaria uma transação a mais.

## Consequências
- O transacoes responde `202` sem esperar a análise; se o antifraude cair, as mensagens esperam na fila.
- **Se o RabbitMQ cair, o transacoes recusa transações (`503`)**: troca disponibilidade por não
  perder análise. Com outbox, ele continuaria aceitando.
- Logo depois do `202`, o `GET /analises/{id}` pode responder `404` por instantes (consistência eventual).
- A mensagem repetida não duplica nada, e isso tem teste (`ConsumidorDeAnaliseTest`).
- Mudar a fila exige mudar o `FilasConfig` dos dois serviços igual, senão o RabbitMQ recusa a declaração.

## Status
Aceita
