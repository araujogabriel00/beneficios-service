# ADR-0005: Eventos de domínio já no incremento 1, publicados em log (D-05)

Status: aceita · 25/10/2026

## Contexto
O extrato (incremento 4) e o Kafka (incremento 3) precisam de eventos de mudança de saldo.

## Alternativas
- **Criar eventos só quando houver Kafka.**
- **Eventos desde já, publicados em log** — escolhida.

## Decisão
`Wallet` registra `BenefitCredited`, `PurchaseDebited` e `PurchaseReversed` a cada mudança de saldo. A porta `DomainEventPublisher` tem, por ora, um adaptador que escreve em log.

## Consequências
- O modelo nasce pronto; trocar o adaptador por Kafka não mexe no domínio.
- Os eventos ainda não são persistidos, então se perdem se o processo cair antes de publicar (resolvido pelo outbox, incremento 3).
