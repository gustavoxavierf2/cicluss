# Ciclus

**E se você tivesse plantado outra coisa?** Simulador para o pequeno produtor do Sudoeste da Bahia: ele clica no terreno no mapa e arrasta uma barra de anos. Para trás, vê como cada cultura teria se saído com o clima que realmente aconteceu ali; para frente, vê a previsão.

Projeto da equipe para o desafio *Field Shift: Adapting Farms with NASA Data* (NASA Space Apps 2026).

> ⚠️ **Protótipo com dados de demonstração.** O clima ainda é gerado por `MockClimaProvider` e a tabela de culturas tem valores de exemplo. Veja [docs/contrato-dados.md](docs/contrato-dados.md) para ligar os dados reais da NASA.

## Estrutura

```
backend/   API em Java 21 + Spring Boot 3.5 (porta 8080)
frontend/  Site em React + TypeScript + Vite + Leaflet (porta 5173)
docs/      Contrato de dados para o pessoal dos dados
```

## Como rodar

Pré-requisitos: Java 21, Maven 3.9+, Node 20+.

```bash
# terminal 1 — backend
cd backend
mvn spring-boot:run

# terminal 2 — frontend
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173. Em desenvolvimento, o Vite repassa `/api` para o backend.

Testes do backend: `cd backend && mvn test`.

## API

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/api/culturas` | Lista de culturas da tabela |
| POST | `/api/simulacoes` | Linha do tempo de uma cultura no terreno (15 anos para trás e 15 para frente) |
| POST | `/api/recomendacoes` | Ranking das culturas mais aptas para o terreno |
| POST | `/api/agente` | Pergunta ao agente sobre os resultados |

Exemplo:

```bash
curl -X POST localhost:8080/api/simulacoes -H 'Content-Type: application/json' \
  -d '{"latitude":-14.866,"longitude":-40.839,"solo":"MISTO","culturaId":"milho"}'
```

## Como o backend está organizado

- `core/` — Java puro, sem Spring. É onde está a regra do simulador:
  - `servico/AptidaoCalculadora` — compara o clima do ano com a faixa da cultura e dá nota 0–100.
  - `servico/SimuladorService` — monta a linha do tempo.
  - `servico/RecomendacaoService` — ranking das culturas.
  - `clima/ClimaProvider` — de onde vem o clima. `MockClimaProvider` (falso) ou `HttpClimaProvider` (serviço do pessoal dos dados).
  - `agente/AgenteDemonstracao` — responde perguntas por regras fixas. Trocar por um LLM depois, mantendo a interface `Agente`.
- `api/` — controllers REST.
- `config/` — liga as peças. Trocar a fonte de clima: `fieldshift.clima.fonte` em `application.properties`.

## Próximos passos

- [ ] Dados reais da NASA (ver contrato).
- [ ] Tabela de culturas com valores do EcoCrop (FAO) / ZARC (Embrapa).
- [ ] Agente com LLM (chave da API só no backend).
- [ ] Deploy para a demo.
