# ADR-0007: Módulo único com pacotes hexagonais (substitui D-01)

Status: aceita · 25/10/2026

## Contexto
O ADR-0001 adotou um módulo Gradle por camada para que o compilador barrasse dependências erradas. Passamos a preferir a estrutura hexagonal clássica em um único módulo: `domain`, `ports/{inbound,outbound}`, `adapters/{inbound,outbound}` e `config`.

## Alternativas
- **Manter quatro módulos Gradle** (ADR-0001).
- **Módulo único, com a regra de dependência verificada pelo ArchUnit** — escolhida.

## Decisão
Um único projeto Gradle, pacote raiz `dev.beneficios`:
- `domain` (`model`, `exception`): sem dependências além do JDK.
- `ports.inbound` (casos de uso) e `ports.outbound` (repositórios, antifraude, publicador de eventos): dependem só de `domain`.
- `application` (`service`, `exception`): implementa `ports.inbound`; depende de `domain` e `ports`, sem Spring.
- `adapters.inbound.rest` e `adapters.outbound.{persistence,messaging,client}`: não dependem um do outro nem de `config`.
- `config`: liga os beans.

Os repositórios (`WalletRepository`, `AuthorizationRepository`) passam de `domain` para `ports.outbound`, ajustando o ADR-0003. O agregado não os usa, então `domain` segue sem dependências.

## Consequências
- Um `build.gradle.kts` só; menos configuração.
- O compilador não barra mais import indevido: o `ArchitectureTest` passa a ser o único guardião e precisa cobrir todas as camadas.
