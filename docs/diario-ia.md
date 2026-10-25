# Diário de uso de IA

## 25/10/2026 — Incremento 1

**Ferramenta:** Claude Code, seguindo `docs/spec.md` e as regras do `CLAUDE.md`.

**O que foi feito com IA:** executei as tarefas T01 a T12 da spec, na ordem, com testes nomeados pelo identificador do critério de aceite.

**Pontos que exigiram correção:**
- O plugin do Spring Boot sozinho não gerencia versões; foi preciso importar o BOM do Spring Boot nos módulos.
- Sem `-parameters` no compilador, `@PathVariable` sem nome explícito falhava e o handler de `IllegalArgumentException` devolvia 400, escondendo a causa. Foi corrigido no `build.gradle.kts` raiz.
- Os testes da API compartilham um contexto Spring, então chaves de idempotência repetidas entre testes geravam 422. As chaves passaram a ser prefixadas pelo id da carteira.

**Decisões minhas na implementação, não ditadas pela spec:**
- A idempotência do crédito mora dentro da `Wallet` (mapa chave → categoria e valor).
- A idempotência da autorização usa o repositório mais um conjunto de chaves em processamento (409).
- O antifraude simulado nega acima de R$ 1.000,00; é regra de simulação, não de negócio.

**Revisão humana pendente:** conferir os valores do circuit breaker (janela, taxa de falha, tempo aberto) e do timeout em `BeansConfig`, que são valores de estudo sem medição.

- Reorganizado de quatro módulos Gradle para um módulo único com pacotes `domain`, `ports`, `application`, `adapters` e `config` (ADR-0007). Sem mudança de comportamento: a suíte passou de 60 para 62 testes, só pelas duas regras novas do `ArchitectureTest`.
