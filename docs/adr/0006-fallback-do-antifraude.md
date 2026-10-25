# ADR-0006: Fallback do antifraude: aprova até R$ 100,00 e nega acima (D-06)

Status: aceita · 25/10/2026

## Contexto
O antifraude é um serviço externo e pode ficar indisponível. A autorização precisa responder mesmo assim.

## Alternativas
- **Aprovar tudo** — não bloqueia o cliente, mas expõe a fraude durante a queda.
- **Negar tudo** — seguro, mas bloqueia todas as compras durante a queda.
- **Aprovar até um limite e negar acima** — escolhida.

## Decisão
Com o antifraude indisponível, aprova compras até R$ 100,00 e nega acima, com `FRAUD_SUSPECTED` (RN-06). O limite é um valor de estudo, sem base em dados reais.

A chamada passa por Retry (o mais externo, que ignora `CallNotPermittedException`), CircuitBreaker e um timeout. O fallback fica fora do Retry e vale para falha, timeout e circuito aberto.

## Consequências
- Equilíbrio entre bloquear o cliente e expor fraude durante a queda.
- Com o circuito aberto, as chamadas seguem direto ao fallback sem esperar o antifraude.
- O limite é uma constante no adaptador; mudá-lo exige novo código.
