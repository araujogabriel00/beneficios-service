# Specs por funcionalidade

Toda funcionalidade nova tem a sua própria spec neste diretório, em `<funcionalidade>.md`, aprovada antes de qualquer código (ADR-0008).

Cada spec traz:
- **Contexto e objetivo:** o problema e o que está fora do escopo.
- **Histórias e critérios de aceite:** identificadores únicos em todo o projeto (CA-xx.y); cada critério vira ao menos um teste com o identificador no nome.
- **Regras de negócio e termos novos** do glossário, quando houver.
- **Contrato da API** e eventos de domínio, quando mudarem.
- **Tarefas** na ordem sugerida de implementação.

A `spec.md` da raiz continua sendo a spec do serviço e do incremento 1.
