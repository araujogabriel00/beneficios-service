# ADR-0001: Gradle multi-módulo por camada (D-01)

Status: substituída pelo ADR-0007 · 25/10/2026

## Contexto
A arquitetura hexagonal exige que dependências apontem para dentro. Precisamos de um mecanismo que barre violações cedo.

## Alternativas
- **Um módulo só, com ArchUnit** — simples de configurar, mas a violação só aparece quando o teste roda.
- **Um módulo por camada (`domain`, `application`, `adapters`, `bootstrap`)** — escolhida.

## Decisão
Quatro subprojetos Gradle. `domain` e `application` não têm Spring no classpath; o compilador recusa o import indevido. O ArchUnit cobre o que o Gradle não vê (por exemplo, `adapter.in` não depender de `adapter.out`).

## Consequências
- O compilador barra dependência errada.
- Mais configuração de build e mais arquivos `build.gradle.kts`.
