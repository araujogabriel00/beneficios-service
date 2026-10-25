# ADR-0004: Autorizar altera `Wallet` e cria `Authorization` na mesma transação (D-04)

Status: aceita · 25/10/2026

## Contexto
Aprovar uma compra debita a carteira e registra a autorização. Se um dos dois falhar sozinho, o saldo e o registro divergem.

## Alternativas
- **Um agregado por transação, com consistência eventual.**
- **Os dois na mesma transação** — escolhida.

## Decisão
O caso de uso `PurchaseService` altera os dois agregados na mesma operação, serializada por carteira (`WalletLocks`). Os dois ficarão no mesmo banco (incremento 2), então uma transação local resolve.

## Consequências
- Consistência forte, o que importa por ser dinheiro.
- No incremento 1 a atomicidade é só por exclusão mútua em memória; a transação de banco e a concorrência otimista chegam no incremento 2.
- Publicar eventos sem perda fica com o padrão outbox, no incremento 3.
