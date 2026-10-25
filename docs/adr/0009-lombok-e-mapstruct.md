# ADR-0009: Lombok para construtores e MapStruct para mapear objetos

Status: aceita · 25/10/2026

## Contexto
Services, controllers e adaptadores repetem construtores que só atribuem campos, e os controllers montam à mão as respostas e os comandos a partir dos objetos de domínio.

## Alternativas
- **Manter construtores e mapeamento manuais.**
- **Lombok (construtores) e MapStruct (mapeamento)** — escolhida.

## Decisão
- **Lombok** apenas para construtores (`@RequiredArgsConstructor`) em `application.service`, `adapters` e controllers. Gera código em tempo de compilação e não deixa dependência no bytecode. O `domain` não usa Lombok: seus construtores validam invariantes e são privados.
- **MapStruct** apenas em `adapters.inbound.rest.mapper`, com `componentModel = "spring"`, para converter agregado → response e request → comando. `domain`, `ports` e `application` não conhecem MapStruct; o `ArchitectureTest` passa a proibir `org.mapstruct..` nessas camadas.
- Os agregados expõem acessores no estilo `id()`, e não `getId()`, que o MapStruct não reconhece como propriedade. Os mappers usam `expression` e métodos `default` onde for preciso.

## Consequências
- Menos código repetitivo nos adaptadores e nos services.
- Processadores de anotação no build (Lombok, MapStruct e o `lombok-mapstruct-binding`, na ordem certa).
- Em mapeamentos com value objects, o ganho do MapStruct é pequeno e exige conversões manuais.
