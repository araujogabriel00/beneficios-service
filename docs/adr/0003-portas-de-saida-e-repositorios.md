# ADR-0003: Repositórios no domínio; demais portas de saída na aplicação (D-03)

Status: aceita, ajustada pelo ADR-0007 (todas as portas de saída agora ficam em `ports.outbound`) · 25/10/2026

## Contexto
É preciso decidir onde ficam as interfaces implementadas pelos adaptadores de saída.

## Alternativas
- **Todas as portas de saída em `application`.**
- **Repositórios no domínio, demais portas em `application/port/out`** — escolhida.

## Decisão
`WalletRepository` e `AuthorizationRepository` ficam no `domain`, junto do agregado. `AntifraudPort` e `DomainEventPublisher` ficam em `application/port/out`.

## Consequências
- O repositório faz parte da linguagem do agregado (DDD).
- Antifraude e eventos são necessidades da aplicação, não do modelo.
