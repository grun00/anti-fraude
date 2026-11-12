# ADR-001: dois serviços, cada um com o seu banco

## Contexto
O sistema tem duas responsabilidades com ritmos diferentes:

- **receber a transação**: precisa responder rápido ao cliente, sempre;
- **analisar a fraude**: é mais lenta, as regras mudam com frequência e a consulta do resultado
  é a leitura mais chamada.

Precisamos decidir como dividir o sistema e onde cada dado mora.

## Decisão
Dois serviços, cada um dono do seu dado, com **o seu próprio banco** (H2 em memória):

- **transacoes**: recebe, valida e grava a transação (tabela `transacao`). Responde `202`.
- **antifraude**: analisa e grava o resultado (tabela `analise`, com o id da transação como
  chave primária). Responde a consulta do resultado.

Nenhum serviço lê a tabela do outro. O antifraude recebe os dados da transação por uma mensagem
na fila (ADR-002), e não consultando o banco do transacoes.

## Alternativas consideradas
- **Um serviço só (monolito)**: mais simples de rodar e sem fila. Mas uma análise lenta ou com
  defeito atrasaria ou derrubaria o recebimento de transações, e não daria para escalar a
  consulta do resultado separado do recebimento. **Rejeitada.**
- **Dois serviços com banco compartilhado**: separa o código, mas acopla pelos dados: uma mudança
  na tabela quebra os dois, e um serviço pode ler dado que não é dele. **Rejeitada.**
- **Dois serviços, um banco cada**: mais peças, e o dado passa de um para o outro por mensagem
  (consistência eventual). Em troca, cada um evolui, sobe e cai sozinho. **Escolhida.**

## Consequências
- Se o antifraude cair, o transacoes continua recebendo transações: elas esperam na fila.
- **Consistência eventual**: logo depois do `202`, a análise ainda não existe e o
  `GET /analises/{id}` responde `404` por alguns instantes. O cliente precisa consultar de novo,
  e o cache não pode guardar esse `404` (ADR-003).
- A transação existe em dois bancos (completa no transacoes, só o necessário no antifraude):
  a mensagem da fila é o contrato entre os dois.

## Status
Aceita
