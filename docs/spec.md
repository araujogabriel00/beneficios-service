# Spec: beneficios-service

25/10/2026 · Gabriel de Araújo Santos

Esta spec define o que o serviço faz e como ele será construído, um incremento por vez. O código de cada incremento só começa quando as seções dele estiverem aprovadas; até lá, refinamos por comentários aqui.

## Constituição

Princípios que valem para todos os incrementos. Mudar um deles exige um ADR.

1. **Dependências apontam para dentro.** O domínio não conhece aplicação, adaptadores nem frameworks. O `ArchitectureTest` (ArchUnit) verifica isso (ADR-0007).
2. **Linguagem ubíqua.** Os termos do glossário aparecem iguais no código, nos testes, na API e nesta spec.
3. **Spec antes do código.** Cada critério de aceite vira ao menos um teste, escrito antes da implementação.
4. **Dinheiro é tipo próprio.** Valores em centavos dentro de `Money`; nunca `double` ou `float`.
5. **Toda alteração de saldo é idempotente e rastreável.** Gera um evento de domínio e pode ser repetida sem efeito duplicado.
6. **Decisão de arquitetura vira ADR** em `docs/adr/`, com contexto, alternativas e consequências.
7. **Nada inventado.** README, ADRs e currículo só trazem números medidos.
8. **Cada funcionalidade nova tem a sua própria spec** em `docs/specs/`, com história, regras e critérios de aceite, aprovada antes do código (ADR-0008). Esta `docs/spec.md` segue como spec do serviço e do incremento 1.

## Contexto, escopo e linguagem ubíqua

Uma empresa concede vale-refeição e vale-alimentação aos colaboradores. O serviço mantém a carteira de cada beneficiário, decide se uma compra no cartão pode ser paga e com qual saldo, e desfaz compras estornadas. O domínio e os dados são inventados; nada vem de código ou clientes reais.

**Dentro do escopo (incremento 1):** abrir carteira, creditar benefício, autorizar compra, estornar compra e consultar saldos.

**Fora do escopo por enquanto:** emissão de cartão, cadastro de empresas e RH, repasse ao estabelecimento, autenticação, limites por compra ou diários, estorno parcial e extrato (o extrato entra no incremento 4).

| Termo | No código | Definição |
| --- | --- | --- |
| Beneficiário | `Beneficiary` | Pessoa titular de uma carteira. |
| Carteira | `Wallet` | Conjunto de saldos de um beneficiário, um por categoria. |
| Categoria de benefício | `BenefitCategory` | Tipo de saldo: `REFEICAO` ou `ALIMENTACAO`. |
| Crédito de benefício | `credit` | Valor que a empresa deposita em uma categoria da carteira. |
| Estabelecimento | `Merchant` | Quem vende. Seu tipo decide qual categoria paga a compra. |
| Tipo de estabelecimento | `MerchantType` | `RESTAURANTE`, `SUPERMERCADO` ou `OUTROS`. |
| Autorização | `Authorization` | Decisão sobre um pedido de compra: aprovada, negada ou estornada. |
| Motivo de negação | `DenialReason` | Por que a compra foi negada. |
| Estorno | `reverse` | Desfaz uma autorização aprovada e devolve o valor à categoria. |
| Chave de idempotência | `IdempotencyKey` | Identificador enviado pelo cliente; repetir a chave não repete o efeito. |
| Antifraude | `AntifraudPort` | Serviço externo que aprova ou nega a compra por risco. |

## Modelo de domínio

Um único bounded context, **Benefícios**, com dois agregados que se referenciam só por id.

| Elemento | Tipo DDD | Responsabilidade |
| --- | --- | --- |
| `Wallet` | Agregado raiz | Guarda os saldos por categoria; credita, debita e devolve; nunca deixa saldo negativo. |
| `Authorization` | Agregado raiz | Registra a decisão de uma compra e controla o ciclo aprovada → estornada. |
| `Money` | Value object (shared kernel) | Valor em centavos, não negativo, com soma e subtração. |
| `WalletId`, `AuthorizationId`, `IdempotencyKey` | Value objects | Identidades tipadas, para não trocar um UUID por outro. |
| `BenefitCategory`, `MerchantType`, `DenialReason` | Enums de domínio | Vocabulário fechado do negócio. |
| `BenefitCredited`, `PurchaseDebited`, `PurchaseReversed` | Eventos de domínio | Emitidos pela `Wallet` a cada mudança de saldo. Base do extrato e do Kafka nos incrementos 3 e 4. |

**Regras de negócio**

- **RN-01** Saldo de uma categoria nunca fica negativo.
- **RN-02** O tipo de estabelecimento decide a categoria: `RESTAURANTE` → `REFEICAO`; `SUPERMERCADO` → `ALIMENTACAO`; `OUTROS` → compra negada (`MERCHANT_NOT_ACCEPTED`).
- **RN-03** Valores de crédito, compra e estorno são maiores que zero.
- **RN-04** A mesma chave de idempotência devolve a mesma autorização, sem novo débito nem nova chamada ao antifraude. Vale também para crédito de benefício. A chave é gravada com a operação e não expira.
- **RN-05** Ordem de verificação: estabelecimento → saldo → antifraude. O antifraude só é chamado se as anteriores passarem.
- **RN-06** Com o antifraude indisponível, aprova até R$ 100,00 e nega acima disso (`FRAUD_SUSPECTED`). O limite é de estudo e está no ADR do fallback.
- **RN-07** Só autorização aprovada pode ser estornada, uma única vez e pelo valor total.
- **RN-08** Compras negadas também são registradas, com motivo, e respeitam a RN-04.

## Histórias e critérios de aceite (incremento 1)

Cada critério (CA) vira um teste com o mesmo identificador no nome.

**US-01 Abrir carteira.** Como empresa, quero abrir a carteira de um beneficiário para depois creditar benefícios.

- **CA-01.1** Dado um nome de beneficiário, quando abro a carteira, então ela existe com saldo zero em todas as categorias.
- **CA-01.2** Dado um nome vazio, quando tento abrir, então recebo erro de validação e nada é criado.

**US-02 Creditar benefício.** Como empresa, quero creditar valor em uma categoria para o beneficiário poder comprar.

- **CA-02.1** Dada uma carteira com R$ 0,00 em refeição, quando credito R$ 500,00 em refeição, então o saldo de refeição é R$ 500,00 e o de alimentação não muda.
- **CA-02.2** Quando credito, então é emitido `BenefitCredited` com carteira, categoria e valor.
- **CA-02.3** Dado valor zero, quando credito, então recebo erro (RN-03).
- **CA-02.4** Dada a mesma chave de idempotência, quando credito duas vezes, então o saldo sobe uma vez só.

**US-03 Autorizar compra.** Como rede de cartões, quero saber se a compra pode ser paga para concluir a venda.

- **CA-03.1** Dado R$ 100,00 em refeição, quando um restaurante pede R$ 30,00, então a compra é aprovada, o saldo vai a R$ 70,00 e é emitido `PurchaseDebited`.
- **CA-03.2** Dado R$ 100,00 em refeição, quando um restaurante pede R$ 100,01, então a compra é negada com `INSUFFICIENT_BALANCE` e o antifraude não é chamado.
- **CA-03.3** Quando o tipo de estabelecimento é `OUTROS`, então a compra é negada com `MERCHANT_NOT_ACCEPTED`.
- **CA-03.4** Quando o antifraude nega, então a compra é negada com `FRAUD_SUSPECTED` e o saldo não muda.
- **CA-03.5** Dada uma autorização já feita com a chave K, quando repito o pedido com K, então recebo a mesma autorização, sem novo débito e sem nova chamada ao antifraude.
- **CA-03.6** Dada a chave K já usada, quando repito K com outro valor ou outra carteira, então recebo 422 e nada muda. Se a primeira requisição com K ainda estiver em processamento, recebo 409.
- **CA-03.7** Dado o antifraude fora do ar, quando peço R$ 80,00, então aprova; quando peço R$ 150,00, então nega com `FRAUD_SUSPECTED` (RN-06).
- **CA-03.8** Dado o antifraude falhando seguidamente, então o circuito abre e as chamadas seguintes vão direto ao fallback, sem esperar o antifraude.

**US-04 Estornar compra.** Como rede de cartões, quero desfazer uma compra cancelada para devolver o valor ao beneficiário.

- **CA-04.1** Dada uma compra aprovada de R$ 30,00 em refeição, quando estorno, então o status fica `REVERSED`, o saldo volta R$ 30,00 e é emitido `PurchaseReversed`.
- **CA-04.2** Dada uma compra já estornada, quando estorno de novo, então recebo erro de conflito e o saldo não muda.
- **CA-04.3** Dada uma compra negada, quando tento estornar, então recebo erro de conflito.
- **CA-04.4** Dado um id inexistente, quando tento estornar, então recebo "não encontrado".

**US-05 Consultar saldos.** Como beneficiário, quero ver meus saldos por categoria.

- **CA-05.1** Dada uma carteira, quando consulto, então vejo o saldo de cada categoria.
- **CA-05.2** Dado um id inexistente, quando consulto, então recebo "não encontrado".

## Plano técnico

**Stack do projeto.** Versões exatas ficam no `gradle/libs.versions.toml`, sempre na última estável da linha indicada.

| Área | Tecnologia | Incremento |
| --- | --- | --- |
| Linguagem | Java 21 (toolchain do Gradle) | 1 |
| Framework | Spring Boot 3.5.x (Web, Actuator, AOP) | 1 |
| Build | Gradle com Kotlin DSL, módulo único e version catalog | 1 |
| Arquitetura | DDD + hexagonal (ports and adapters), verificada por ArchUnit | 1 |
| Resiliência | Resilience4j (circuit breaker, retry) | 1 |
| Documentação da API | springdoc-openapi (OpenAPI 3, Swagger UI) | 1 |
| Testes | JUnit 5, AssertJ, ArchUnit, MockMvc | 1 |
| Testes de integração | Testcontainers | 2 |
| Banco relacional (ledger) | PostgreSQL, com Spring Data JPA e Flyway | 2 |
| Contêiner | Docker | 2 |
| Orquestração local | Kubernetes com kind e Helm | 2 |
| CI/CD | GitHub Actions | 2 |
| Mensageria | Apache Kafka | 3 |
| Nuvem | AWS EKS | 3 |
| Infra como código | Terraform | 3 |
| Banco NoSQL (extrato) | MongoDB | 4 |
| Cache e idempotência | Redis | 4 |
| Observabilidade | OpenTelemetry, Prometheus, Grafana | 4 |
| IA no desenvolvimento | Claude Code ou Cursor, com regras em `CLAUDE.md` | 1 em diante |

**Pacotes e regra de dependência.** Um único módulo Gradle; a regra da arquitetura hexagonal é verificada pelo `ArchitectureTest` (ADR-0007).

| Pacote | Pode depender de | Não pode depender de |
| --- | --- | --- |
| `domain` | Nada além do JDK | Spring, Resilience4j, `ports`, `application`, `adapters`, `config` |
| `ports` | `domain` | Spring, `application`, `adapters`, `config` |
| `application` | `domain`, `ports` | Spring, Resilience4j, `adapters`, `config` |
| `adapters.inbound` | `domain`, `ports`, Spring Web | `adapters.outbound`, `config` |
| `adapters.outbound` | `domain`, `ports`, Spring, Resilience4j | `adapters.inbound`, `config` |
| `config` | Todos | — |

**Estrutura de pastas**

```text
beneficios-service/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
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
│   │   │                        ReversePurchase, GetWallet (+ comandos)
│   │   └── outbound/            WalletRepository, AuthorizationRepository,
│   │                            AntifraudPort, DomainEventPublisher
│   ├── application/
│   │   ├── service/             implementação dos casos de uso, sem Spring
│   │   └── exception/           NotFoundException, RequestInProgressException
│   ├── adapters/
│   │   ├── inbound/rest/        controllers, tratamento de erros, response/
│   │   └── outbound/
│   │       ├── persistence/     repositórios em memória
│   │       ├── messaging/       publicador em log (Kafka no incremento 3)
│   │       └── client/          antifraude simulado + Resilience4j
│   └── config/                  fiação dos beans
├── src/main/resources/          application.yml
├── src/test/java/dev/beneficios/
└── docs/                        adr/, specs/, diario-ia.md
```

Os testes espelham os pacotes: domínio e casos de uso sem Spring; API e Resilience4j com contexto Spring. O `ArchitectureTest` cobre toda a regra de dependência da tabela acima.

**Contrato da API (rascunho)**

| Método e rota | Corpo | Respostas |
| --- | --- | --- |
| `POST /wallets` | `beneficiaryName` | 201 carteira; 400 validação |
| `POST /wallets/{id}/credits` | `category`, `amountCents`; header `Idempotency-Key` | 200 carteira; 400; 404; 409; 422 |
| `GET /wallets/{id}` | — | 200 saldos; 404 |
| `POST /authorizations` | `walletId`, `merchantType`, `amountCents`; header `Idempotency-Key` | 200 aprovada ou negada; 404 carteira; 422 chave reutilizada com outro conteúdo; 409 mesma chave em processamento |
| `POST /authorizations/{id}/reversal` | — | 200 estornada; 404; 409 |

Erros no formato Problem Details (RFC 9457) e contrato publicado pelo springdoc em `/swagger-ui`.

## Decisões e questões resolvidas

Decisões confirmadas em 25/10/2026. Cada uma vira um ADR em docs/adr/ na tarefa T12.

| ID | Decisão | Alternativa descartada | Motivo |
| --- | --- | --- | --- |
| D-01 | Módulo único com pacotes hexagonais, verificado por ArchUnit (ADR-0007; substitui o multi-módulo) | Gradle multi-módulo por camada | Estrutura hexagonal clássica com menos configuração de build; o compilador deixa de barrar dependência errada, então o ArchUnit cobre tudo. |
| D-02 | Código em inglês, glossário em português | Código em português | Padrão das vagas e do mercado; o glossário mantém a linguagem ubíqua. |
| D-03 | Todas as portas de saída (repositórios, antifraude, eventos) em `ports.outbound` | Repositórios no domínio | Segue a estrutura hexagonal adotada (ADR-0007); o agregado não usa o repositório, então o domínio segue isolado. |
| D-04 | Autorizar altera `Wallet` e cria `Authorization` na mesma transação | Um agregado por transação, com consistência eventual | Os dois ficam no mesmo banco e é dinheiro: consistência forte vale mais. A publicação de eventos sem perda fica com o outbox, no incremento 3. |
| D-05 | Eventos de domínio já no incremento 1, publicados em log | Criar eventos só quando houver Kafka | O modelo nasce pronto para extrato e Kafka; o adaptador muda depois, o domínio não. |
| D-06 | Fallback do antifraude: aprova até R$ 100,00 e nega acima | Aprovar tudo ou negar tudo | Equilíbrio entre bloquear o cliente e expor fraude durante a queda. Valor de estudo. |

**Questões resolvidas**

- **Q-01** Mesma chave com outro conteúdo: 422. Mesma chave com a primeira requisição ainda em processamento: 409. Segue o rascunho do IETF para o header `Idempotency-Key`.
- **Q-02** O crédito de benefício também exige chave de idempotência.
- **Q-03** No banco, a chave não expira (restrição de unicidade). No Redis, a partir do incremento 4, é só cache, com TTL de 24 horas.
- **Q-04** Limites por compra ou diários ficam fora do incremento 1.
- **Q-05** Metas de latência e disponibilidade são definidas no incremento 4, depois de medir.
- **Q-06** O pacote de adaptadores se chama `adapters`, com `inbound` e `outbound`.

## Incrementos e tarefas

Só o incremento 1 está detalhado aqui. Cada funcionalidade dos incrementos seguintes ganha a sua própria spec em `docs/specs/`, com histórias, critérios e tarefas, antes de começar (princípio 8).

| Incremento | Semanas | Escopo |
| --- | --- | --- |
| 1 | 1 | Domínio, casos de uso, memória, API REST, Resilience4j enxuto (circuit breaker, retry, timeout, fallback), eventos em log |
| 2 | 2 e 3 | Ledger em PostgreSQL com Testcontainers e concorrência otimista, Docker multi-stage, Kubernetes no kind com Helm, GitHub Actions |
| 3 | 4 a 6 | Kafka com padrão outbox e consumidor idempotente (semana 4); deploy no AWS EKS e Terraform (semanas 5 e 6) |
| 4 | 7 e 8 | Extrato em MongoDB e idempotência e cache em Redis (semana 7); OpenTelemetry, Prometheus e Grafana (semana 8) |

**Tarefas do incremento 1** (ordem sugerida; cada uma fecha com testes verdes)

- [x] **T01** Build Gradle: projeto único, version catalog, toolchain Java 21. Pronto quando `./gradlew build` passa.
- [x] **T02** `Money` e ids tipados. Cobre RN-01 e RN-03.
- [x] **T03** Agregado `Wallet` com eventos. Cobre CA-01.1, CA-01.2, CA-02.1 a CA-02.3.
- [x] **T04** Agregado `Authorization` e estados. Cobre CA-04.2 e CA-04.3 no nível do domínio.
- [x] **T05** Casos de uso de carteira (abrir, creditar, consultar) com repositório fake. Cobre US-01, US-02, US-05.
- [x] **T06** Caso de uso autorizar compra, com antifraude fake. Cobre CA-03.1 a CA-03.6.
- [x] **T07** Caso de uso estornar. Cobre US-04.
- [x] **T08** Adaptadores em memória e publicador de eventos em log.
- [x] **T09** Antifraude simulado com Resilience4j e teste de circuito. Fallback na camada mais externa (Retry), e o Retry ignora `CallNotPermittedException`. Cobre CA-03.7 e CA-03.8.
- [x] **T10** API REST, Problem Details e OpenAPI. Testes de contrato com MockMvc.
- [x] **T11** `ArchitectureTest` e `CLAUDE.md` com as regras desta spec.
- [x] **T12** README, ADRs das decisões confirmadas e entrada no diário de IA.

**Pronto do incremento 1:** todos os CAs com teste verde, `./gradlew build` limpo, API documentada no Swagger e os ADRs escritos.
