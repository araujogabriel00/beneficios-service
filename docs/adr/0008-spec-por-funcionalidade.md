# ADR-0008: Uma spec por funcionalidade nova

Status: aceita · 25/10/2026

## Contexto
A constituição (princípio 3) manda escrever a spec antes do código, mas todas as funcionalidades viviam em um único `spec.md`, que tende a crescer a cada incremento e a misturar assuntos.

## Alternativas
- **Continuar acrescentando tudo em `spec.md`.**
- **Uma spec por funcionalidade em `docs/specs/`** — escolhida.

## Decisão
Cada funcionalidade nova ganha `docs/specs/<funcionalidade>.md`, com história, regras e critérios de aceite, aprovada antes do código. O `spec.md` da raiz permanece como spec do serviço e do incremento 1, e passa a apenas linkar as novas specs. Cada spec traz:
- **Contexto e objetivo:** o problema e o que está fora do escopo.
- **Histórias e critérios de aceite:** identificadores únicos em todo o projeto (CA-xx.y); cada critério vira ao menos um teste com o identificador no nome.
- **Regras de negócio e termos novos** do glossário, quando houver.
- **Contrato da API** e eventos de domínio, quando mudarem.
- **Tarefas** na ordem sugerida de implementação.

Entra como princípio 8 da constituição e no `CLAUDE.md`.

## Consequências
- Cada mudança é revisada isoladamente e os critérios de aceite ficam próximos do assunto.
- Há mais de um arquivo para consultar.
