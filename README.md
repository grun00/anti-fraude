# Antifraude

Projeto final do módulo de Arquitetura de Software.

**Perfil de execução: A** (roda com Docker).

## O problema

Toda transação de pagamento precisa passar por uma análise de fraude, mas o cliente não pode
ficar esperando a análise terminar para ter a resposta do pedido.

- O serviço **transacoes** recebe a transação, grava no banco dele e responde `202 Accepted` na hora.
- O serviço **antifraude** recebe a transação por uma **fila**, calcula a pontuação de risco,
  grava o resultado no banco dele e responde a consulta do resultado, com **cache**.

### As regras de pontuação

| Regra | Pontos |
|---|:---:|
| Valor acima de R$ 5.000 | +50 |
| Entre 0h e 6h | +20 |
| Conta de destino na lista negra | +100 |
| Mais de 3 transações da mesma conta em 10 minutos | +30 |

Resultado: menos de 40 é **APROVADA**, de 40 a 69 é **EM_REVISAO**, 70 ou mais é **REPROVADA**.
Um analista pode revisar uma transação `EM_REVISAO` e trocar o resultado.

## O desenho

```
cliente ── POST /transacoes ──▶ [transacoes] ──grava──▶ H2 "transacoes"
                                     │
                                     │ publica a transação          (parte 2)
                                     ▼
                              RabbitMQ: fila
                                     │
                                     │ consome, analisa
                                     ▼
cliente ── GET /analises/{id} ─▶ [antifraude] ──grava──▶ H2 "antifraude"
                                     │
                                     └── cache-aside ──▶ Redis      (parte 3)
```

- Cada serviço tem **o seu banco** e ninguém lê a tabela do outro ([ADR-001](docs/adr/ADR-001-servicos-e-bancos.md)).
- A **fila** (RabbitMQ) leva a transação do transacoes para o antifraude. Se a mesma mensagem
  chegar duas vezes, o antifraude grava uma vez só (parte 2).
- O **cache** (Redis) guarda o resultado da análise (parte 3).

| Serviço | Porta | Endpoints | Banco |
|---|---|---|---|
| transacoes | 8081 | `POST /transacoes`, `GET /transacoes/{id}` | H2 `transacoes` (tabela `transacao`) |
| antifraude | 8082 | `GET /analises/{transacaoId}` | H2 `antifraude` (tabela `analise`) |

## Como rodar

Precisa só do Git e do **Docker Desktop aberto**. Não precisa instalar Java nem Maven.

```bash
git clone https://github.com/grun00/anti-fraude.git
cd anti-fraude
docker compose up -d --build
```

A primeira vez demora uns minutos (o Docker baixa as imagens e compila). Para ver se subiu:

```bash
docker compose logs | grep Started    # deve aparecer TransacoesApplication e AntifraudeApplication
```

### Mandar uma transação

```bash
curl -s -X POST localhost:8081/transacoes -H "Content-Type: application/json" \
  -d '{"conta_origem":"0001-123","conta_destino":"0002-456","valor":150.00,"data_hora":"2026-11-12T14:30:00"}'
```

A resposta é `202` com o `transacao_id`. Para consultar:

```bash
curl -s localhost:8081/transacoes/<transacao_id>   # a transação, no transacoes
curl -s localhost:8082/analises/<transacao_id>     # o resultado, no antifraude (404 até a parte 2)
```

### Olhar os bancos

| Serviço | Console | JDBC URL |
|---|---|---|
| transacoes | http://localhost:8081/h2-console | `jdbc:h2:mem:transacoes` |
| antifraude | http://localhost:8082/h2-console | `jdbc:h2:mem:antifraude` |

Usuário `sa`, senha em branco. Os bancos ficam na memória: reiniciar o serviço apaga o banco dele.

### Testes e desligar

```bash
docker compose run --rm testes   # roda os testes dos dois serviços
docker compose down              # desliga tudo
```

## Decisões (ADRs)

- [ADR-001: dois serviços, cada um com o seu banco](docs/adr/ADR-001-servicos-e-bancos.md)
