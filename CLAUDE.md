# beneficios-service

Serviço de benefícios (vale-refeição e vale-alimentação). A fonte da verdade é `spec.md`; leia-a antes de mudar comportamento. Domínio e dados são inventados.

## Comandos

- `./gradlew build` — compila e roda todos os testes (precisa de JDK 21; a toolchain está configurada).
- `./gradlew bootRun` — sobe a API; Swagger em `/swagger-ui`.

## Regras (Constituição da spec)

1. **Dependências apontam para dentro.** Módulo único (ADR-0007). `domain` não conhece nada; `ports` só conhece `domain`; `application` conhece `domain` e `ports`; `adapters` conhecem os três; `config` liga tudo. Sem Spring ou Resilience4j em `domain`/`ports`/`application`. `adapters.inbound` não depende de `adapters.outbound`. O `ArchitectureTest` verifica.
2. **Linguagem ubíqua.** Código em inglês, mas os termos do glossário da spec (`Wallet`, `BenefitCategory`, `REFEICAO`, `Authorization`, `DenialReason`...) ficam iguais no código, nos testes e na API.
3. **Spec antes do código.** Cada critério de aceite (CA-xx.y) vira ao menos um teste com o identificador no nome (`ca_03_2_...`); escreva o teste antes da implementação. Mudou comportamento? Atualize a spec primeiro.
4. **Dinheiro é `Money`** (centavos, não negativo). Nunca `double` ou `float`.
5. **Toda alteração de saldo é idempotente e rastreável:** gera evento de domínio (`BenefitCredited`, `PurchaseDebited`, `PurchaseReversed`) e respeita a chave de idempotência (RN-04).
6. **Decisão de arquitetura vira ADR** em `docs/adr/`.
7. **Nada inventado.** README, ADRs e currículo só trazem números medidos.

## Convenções

- Ordem de verificação na autorização: estabelecimento → saldo → antifraude (RN-05).
- Antifraude: Retry (mais externo, ignora `CallNotPermittedException`) → CircuitBreaker → chamada com timeout; o fallback da RN-06 fica fora do Retry.
- Erros da API em Problem Details (RFC 9457): 400 validação, 404 não encontrado, 409 conflito de estado ou chave em processamento, 422 chave reutilizada com outro conteúdo.
- O projeto compila com `-parameters`; mantenha, pois os controllers dependem dele.
