# ADR-0002: Código em inglês, glossário em português (D-02)

Status: aceita · 25/10/2026

## Contexto
A linguagem ubíqua precisa aparecer igual no código, nos testes, na API e na spec.

## Alternativas
- **Código em português** — alinha com o negócio, mas foge do padrão do mercado e das vagas.
- **Código em inglês, glossário em português** — escolhida.

## Decisão
Identificadores em inglês (`Wallet`, `Authorization`). Os valores do vocabulário de negócio ficam como no glossário (`REFEICAO`, `ALIMENTACAO`, `RESTAURANTE`, `SUPERMERCADO`, `OUTROS`). A tabela do glossário na spec mapeia cada termo ao nome no código.

## Consequências
- Padrão familiar a quem lê o código de fora.
- Quem lê a spec precisa do glossário para traduzir; os enums em português misturam idiomas de propósito.
