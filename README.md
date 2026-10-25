# beneficios-service

Serviço de estudo que mantém a carteira de vale-refeição e vale-alimentação de cada beneficiário, autoriza compras no cartão e estorna compras canceladas. Domínio e dados são inventados.

A especificação completa está em [`spec.md`](spec.md). Este repositório cobre o **incremento 1**: domínio, casos de uso, persistência em memória, API REST, Resilience4j e eventos em log.

## Arquitetura

DDD com arquitetura hexagonal (ports and adapters), em um único módulo Gradle (ADR-0007):

| Pacote | Papel |
| --- | --- |
| `domain` | `Wallet`, `Authorization`, `Money`, eventos e exceções de negócio. Só JDK. |
| `ports` | `inbound` (casos de uso) e `outbound` (repositórios, antifraude, publicador de eventos). |
| `application` | Implementação dos casos de uso. Sem Spring. |
| `adapters` | `inbound/rest` (API REST); `outbound` com repositórios em memória, antifraude simulado com Resilience4j e publicador de eventos em log. |
| `config` | Fiação dos beans. |

O `ArchitectureTest` impede que as dependências apontem para fora.

## Como rodar

Requer JDK 21.

```bash
./gradlew build                 # compila e roda os testes
./gradlew bootRun                # sobe a API em http://localhost:8080
```

Documentação interativa em `http://localhost:8080/swagger-ui`.

## Exemplo

```bash
# abrir carteira
curl -X POST localhost:8080/wallets -H 'Content-Type: application/json' -d '{"beneficiaryName":"Maria"}'

# creditar R$ 500,00 em refeição
curl -X POST localhost:8080/wallets/{id}/credits -H 'Idempotency-Key: c1' \
  -H 'Content-Type: application/json' -d '{"category":"REFEICAO","amountCents":50000}'

# autorizar compra de R$ 30,00 em restaurante
curl -X POST localhost:8080/authorizations -H 'Idempotency-Key: a1' \
  -H 'Content-Type: application/json' \
  -d '{"walletId":"{id}","merchantType":"RESTAURANTE","amountCents":3000}'

# estornar
curl -X POST localhost:8080/authorizations/{authorizationId}/reversal
```

## Decisões

As decisões de arquitetura estão em [`docs/adr/`](docs/adr). O uso de IA no desenvolvimento está em [`docs/diario-ia.md`](docs/diario-ia.md).

## Limites do incremento 1

Os dados ficam em memória e se perdem ao reiniciar. O antifraude é simulado, e os parâmetros de circuit breaker, retry e timeout são valores de estudo, ainda sem medição. Persistência em PostgreSQL, Docker e Kubernetes vêm no incremento 2.
