# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# beneficios-service

Serviço de benefícios (vale-refeição e vale-alimentação). A fonte da verdade é `docs/spec.md`; leia-a antes de mudar comportamento. Domínio e dados são inventados.

## Comandos

- `./gradlew build` — compila e roda todos os testes (precisa de JDK 21; a toolchain está configurada).
- `./gradlew bootRun` — sobe a API em `localhost:8080`; Swagger em `/swagger-ui`.
- `./gradlew test --tests '*PurchaseServiceTest'` — uma classe de teste; `--tests '*PurchaseServiceTest.ca_03_1*'` filtra por método/critério.
- Não há linter nem formatter configurado.

## Arquitetura (visão geral)

Módulo Gradle único, pacote raiz `dev.beneficios`: `domain` → `ports` (inbound = casos de uso, outbound = repositórios, antifraude, publicador de eventos) → `application/service` (implementa os `ports.inbound`) → `adapters` (REST e saída) → `config`. Os services são POJOs sem Spring; `config/BeansConfig` instancia tudo na mão e é o único lugar que conhece adaptadores e services juntos.

Pontos que exigem ler vários arquivos:
- **Concorrência (in-memory, D-04):** `WalletLocks` dá um monitor por `WalletId`; `WalletService` e `PurchaseService` compartilham a mesma instância e fazem `synchronized` nela ao alterar saldo. Autorizar altera `Wallet` e cria `Authorization` sob o mesmo lock.
- **Idempotência:** crédito guarda a chave dentro da `Wallet`; autorização usa `AuthorizationRepository.findByIdempotencyKey` mais o conjunto `inFlight` do `PurchaseService` (chave em processamento → 409; mesma chave com outro conteúdo → 422).
- **Eventos:** o agregado acumula eventos; o service chama `wallet.pullEvents()` e publica via `DomainEventPublisher` depois de salvar.
- **Antifraude:** `ResilientAntifraudAdapter` (Retry → CircuitBreaker → timeout) envolve um `AntifraudClient`; o fallback da RN-06 (aprova até R$ 100,00) fica fora do Retry. Parâmetros em `BeansConfig`.
- **Erros → HTTP:** exceções de `domain.exception` e `application.exception` são mapeadas para Problem Details em `ApiExceptionHandler`.

## Onde está o quê

`docs/spec.md` (serviço e incremento 1: regras RN-xx, critérios CA-xx.y, tarefas) · `docs/specs/` (uma spec por funcionalidade nova) · `docs/adr/` (decisões) · `docs/diario-ia.md` (uso de IA).

## Regras (Constituição da spec)

1. **Dependências apontam para dentro.** Módulo único (ADR-0007). `domain` não conhece nada; `ports` só conhece `domain`; `application` conhece `domain` e `ports`; `adapters` conhecem os três; `config` liga tudo. Sem Spring ou Resilience4j em `domain`/`ports`/`application`. `adapters.inbound` não depende de `adapters.outbound`. O `ArchitectureTest` verifica.
2. **Linguagem ubíqua.** Código em inglês, mas os termos do glossário da spec (`Wallet`, `BenefitCategory`, `REFEICAO`, `Authorization`, `DenialReason`...) ficam iguais no código, nos testes e na API.
3. **Spec antes do código.** Cada critério de aceite (CA-xx.y) vira ao menos um teste com o identificador no nome (`ca_03_2_...`); escreva o teste antes da implementação. Mudou comportamento? Atualize a spec primeiro.
4. **Dinheiro é `Money`** (centavos, não negativo). Nunca `double` ou `float`.
5. **Toda alteração de saldo é idempotente e rastreável:** gera evento de domínio (`BenefitCredited`, `PurchaseDebited`, `PurchaseReversed`) e respeita a chave de idempotência (RN-04).
6. **Decisão de arquitetura vira ADR** em `docs/adr/`.
7. **Nada inventado.** README, ADRs e currículo só trazem números medidos.
8. **Funcionalidade nova tem spec própria** em `docs/specs/<funcionalidade>.md` (história, regras, critérios CA-xx.y), escrita e aprovada antes do código. Não acrescente funcionalidade nova a `docs/spec.md`; ela só ganha um link para a nova spec.

## Convenções

- Ordem de verificação na autorização: estabelecimento → saldo → antifraude (RN-05).
- Antifraude: Retry (mais externo, ignora `CallNotPermittedException`) → CircuitBreaker → chamada com timeout; o fallback da RN-06 fica fora do Retry.
- Erros da API em Problem Details (RFC 9457): 400 validação, 404 não encontrado, 409 conflito de estado ou chave em processamento, 422 chave reutilizada com outro conteúdo.
- Lombok só para construtores (`@RequiredArgsConstructor`) em services, controllers e adaptadores; o `domain` fica sem Lombok. MapStruct só em `adapters` (`adapters/inbound/rest/mapper`), com `componentModel = "spring"`; os agregados não têm getters `getX`, então os mappers usam `expression`/métodos `default`.
- O projeto compila com `-parameters`; mantenha, pois os controllers dependem dele.
