# beneficios-service

Serviço de estudo que mantém a carteira de vale-refeição e vale-alimentação de cada beneficiário, autoriza compras no cartão e estorna compras canceladas. Domínio e dados são inventados.

A especificação completa está em [`spec.md`](spec.md); ela é a fonte da verdade. Este repositório cobre o **incremento 1**: domínio, casos de uso, persistência em memória, API REST, Resilience4j e eventos em log.

**Stack:** Java 21, Spring Boot 3.5, Gradle (Kotlin DSL), Resilience4j, springdoc-openapi, JUnit 5, AssertJ e ArchUnit.

## O que o serviço faz

- Abre a carteira de um beneficiário, com saldo zero em `REFEICAO` e `ALIMENTACAO`.
- Credita benefício em uma categoria.
- Autoriza compras: o tipo de estabelecimento decide a categoria (`RESTAURANTE` → `REFEICAO`, `SUPERMERCADO` → `ALIMENTACAO`, `OUTROS` → negada).
- Estorna compras aprovadas, devolvendo o valor à categoria.
- Consulta os saldos.

Regras que valem a pena conhecer:

- Saldo nunca fica negativo, e dinheiro é sempre `Money` em centavos.
- Crédito e autorização exigem o header `Idempotency-Key`: repetir a chave devolve o mesmo resultado, sem novo débito.
- A autorização verifica estabelecimento, depois saldo, depois antifraude.
- Com o antifraude fora do ar, o fallback aprova até R$ 100,00 e nega acima disso.
- Cada mudança de saldo gera um evento de domínio (`BenefitCredited`, `PurchaseDebited`, `PurchaseReversed`), publicado em log.

## API

| Método e rota | Descrição | Respostas |
| --- | --- | --- |
| `POST /wallets` | Abre a carteira (`beneficiaryName`) | 201; 400 |
| `POST /wallets/{id}/credits` | Credita benefício (`category`, `amountCents`; `Idempotency-Key`) | 200; 400; 404; 409; 422 |
| `GET /wallets/{id}` | Consulta saldos | 200; 404 |
| `POST /authorizations` | Autoriza compra (`walletId`, `merchantType`, `amountCents`; `Idempotency-Key`) | 200 aprovada ou negada; 404; 409; 422 |
| `POST /authorizations/{id}/reversal` | Estorna compra aprovada | 200; 404; 409 |

Erros seguem Problem Details (RFC 9457): 400 validação, 404 não encontrado, 409 conflito de estado ou chave em processamento, 422 chave reutilizada com outro conteúdo. O contrato interativo fica em `/swagger-ui`.

## Arquitetura

DDD com arquitetura hexagonal (ports and adapters), em um único módulo Gradle (ADR-0007):

| Pacote | Papel |
| --- | --- |
| `domain` | `Wallet`, `Authorization`, `Money`, eventos e exceções de negócio. Só JDK. |
| `ports` | `inbound` (casos de uso) e `outbound` (repositórios, antifraude, publicador de eventos). |
| `application` | Implementação dos casos de uso. Sem Spring. |
| `adapters` | `inbound/rest` (API REST); `outbound` com repositórios em memória, antifraude simulado com Resilience4j e publicador de eventos em log. |
| `config` | Fiação dos beans. |

As dependências apontam para dentro, e o `ArchitectureTest` falha se alguma apontar para fora.

**Resiliência do antifraude:** Retry (mais externo) → CircuitBreaker → chamada com timeout. O fallback da regra de negócio fica fora do Retry.

### Estrutura de pastas

```text
beneficios-service/
├── build.gradle.kts, settings.gradle.kts, gradle/libs.versions.toml
├── src/main/java/dev/beneficios/
│   ├── BeneficiosApplication.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── shared/          Money, DomainEvent, AggregateRoot
│   │   │   ├── wallet/          Wallet, WalletId, BenefitCategory
│   │   │   ├── authorization/   Authorization, AuthorizationId, IdempotencyKey,
│   │   │   │                    MerchantType, DenialReason
│   │   │   └── event/           BenefitCredited, PurchaseDebited, PurchaseReversed
│   │   └── exception/           InvalidStateException, IdempotencyConflictException,
│   │                            InsufficientBalanceException
│   ├── ports/
│   │   ├── inbound/             OpenWallet, CreditBenefit, AuthorizePurchase,
│   │   │                        ReversePurchase, GetWallet
│   │   └── outbound/            WalletRepository, AuthorizationRepository,
│   │                            AntifraudPort, DomainEventPublisher
│   ├── application/
│   │   ├── service/             WalletService, PurchaseService, WalletLocks
│   │   └── exception/           NotFoundException, RequestInProgressException
│   ├── adapters/
│   │   ├── inbound/rest/        controllers, ApiExceptionHandler, response/
│   │   └── outbound/
│   │       ├── persistence/     repositórios em memória
│   │       ├── messaging/       publicador de eventos em log
│   │       └── client/          antifraude simulado + Resilience4j
│   └── config/                  BeansConfig
├── src/main/resources/          application.yml
├── src/test/java/dev/beneficios/  testes espelhando os pacotes + ArchitectureTest
└── docs/                        adr/, diario-ia.md
```

## Como rodar

Requer JDK 21.

```bash
./gradlew build      # compila e roda os testes
./gradlew bootRun    # sobe a API em http://localhost:8080
```

Documentação interativa em `http://localhost:8080/swagger-ui`.

## Exemplo

Troque `{id}` pelo `id` devolvido ao abrir a carteira.

```bash
# abrir carteira
curl -X POST localhost:8080/wallets -H 'Content-Type: application/json' -d '{"beneficiaryName":"Maria"}'

# creditar R$ 500,00 em refeição
curl -X POST localhost:8080/wallets/{id}/credits -H 'Idempotency-Key: c1'   -H 'Content-Type: application/json' -d '{"category":"REFEICAO","amountCents":50000}'

# autorizar compra de R$ 30,00 em restaurante
curl -X POST localhost:8080/authorizations -H 'Idempotency-Key: a1'   -H 'Content-Type: application/json'   -d '{"walletId":"{id}","merchantType":"RESTAURANTE","amountCents":3000}'

# estornar (use o id da autorização)
curl -X POST localhost:8080/authorizations/{authorizationId}/reversal

# consultar saldos
curl localhost:8080/wallets/{id}
```

## Testes

`./gradlew test` roda 62 testes. Cada critério de aceite da spec (CA-xx.y) tem ao menos um teste com o identificador no nome, como `ca_03_2_...`. Domínio e casos de uso rodam sem Spring; a API e o Resilience4j usam contexto Spring.

## Documentação

- [`spec.md`](spec.md): escopo, regras de negócio, critérios de aceite e tarefas.
- [`docs/adr/`](docs/adr): decisões de arquitetura.
- [`docs/diario-ia.md`](docs/diario-ia.md): uso de IA no desenvolvimento.

## Limites do incremento 1

Os dados ficam em memória e se perdem ao reiniciar. O antifraude é simulado, e os parâmetros de circuit breaker, retry e timeout são valores de estudo, ainda sem medição. Persistência em PostgreSQL, Docker e Kubernetes vêm no incremento 2.
