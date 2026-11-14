# Ciclus

**E se você tivesse plantado outra coisa?** Simulador para o pequeno produtor do Sudoeste da Bahia: ele clica no terreno no mapa e arrasta uma barra de anos. Para trás, vê como cada cultura teria se saído com o clima que realmente aconteceu ali; para frente, vê a previsão.

Projeto da equipe para o desafio *Field Shift: Adapting Farms with NASA Data* (NASA Space Apps 2026).

> ⚠️ **Protótipo com dados de demonstração.** O clima ainda é gerado por `MockClimaProvider` e a tabela de culturas tem valores de exemplo. Veja [docs/contrato-dados.md](docs/contrato-dados.md) para ligar os dados reais da NASA.

## Estrutura

```
backend/   API em Java 21 + Spring Boot 3.5 (porta 8080)
rodar.ps1 / rodar.sh   sobem tudo com um comando
frontend/  Site em React + TypeScript + Vite + Leaflet (porta 5173)
docs/      Contrato de dados para o pessoal dos dados
```

## Como rodar

Pré-requisitos: Java 21 ou mais novo, Maven 3.9+, Node 20+.

Um comando só, na raiz do projeto (sobe backend e frontend juntos e mostra a URL):

```powershell
# Windows (PowerShell)
powershell -ExecutionPolicy Bypass -File .
odar.ps1
```

```bash
# Linux / macOS
./rodar.sh
```

Abra http://localhost:5173. `Ctrl+C` encerra os dois. Em desenvolvimento, o Vite repassa `/api` para o backend (porta 8080).

Se preferir subir separado: `cd backend && mvn spring-boot:run` em um terminal e `cd frontend && npm install && npm run dev` em outro.

Testes do backend: `cd backend && mvn test`. Build do front: `cd frontend && npm run build`.

### Como ligar a IA do chat

O chat "Tire dúvidas" usa a API da Anthropic quando a variável de ambiente `ANTHROPIC_API_KEY` existe **no terminal em que você roda o backend**. A chave fica só no backend, nunca vai para o navegador.

```powershell
# Windows (PowerShell)
$env:ANTHROPIC_API_KEY = "sua-chave"
powershell -ExecutionPolicy Bypass -File .
odar.ps1
```

```bash
# Linux / macOS
export ANTHROPIC_API_KEY="sua-chave"
./rodar.sh
```

- Sem a variável, ou se a chamada falhar ou passar de 20 s, quem responde é o agente de demonstração (regras fixas) e o chat mostra o selo **modo demonstração**. O motivo da falha aparece no log do backend.
- O modelo é configurável em `backend/src/main/resources/application.properties`: `fieldshift.agente.modelo` (padrão `claude-sonnet-5-5`).
- A IA não calcula nada: recebe os números da simulação (os 30 anos, os resumos e o ranking) e só explica em linguagem simples.

## Como publicar na internet

O `Dockerfile` da raiz monta um site único: compila o frontend e coloca dentro do backend, que serve a página e a API na mesma porta (variável `PORT`, padrão 8080).

No [Render](https://render.com) (plano gratuito):

1. Crie a conta e conecte o GitHub.
2. **New > Blueprint**, escolha este repositório. O `render.yaml` já diz o que criar.
3. Quando pedir `ANTHROPIC_API_KEY`, cole a chave para o chat usar IA ou deixe vazio (modo demonstração).
4. Espere o primeiro deploy (uns 5 a 10 minutos). O endereço fica no formato `https://ciclus-xxxx.onrender.com`.

No plano gratuito o site dorme depois de 15 minutos sem uso; a primeira visita depois disso demora cerca de 1 minuto para acordar. Cada push na `main` publica de novo.

Para testar a imagem na sua máquina: `docker build -t ciclus . && docker run -p 8080:8080 ciclus`.

## API

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/api/culturas` | Lista de culturas da tabela |
| POST | `/api/simulacoes` | Linha do tempo de uma cultura no terreno (15 anos para trás e 15 para frente) |
| POST | `/api/recomendacoes` | Ranking das culturas mais aptas para o terreno |
| POST | `/api/cenarios` | "E se o clima fosse diferente?": mesma regra da nota com temperatura, chuva e umidade ajustadas |
| POST | `/api/planejamentos` | Plano de plantio com rotação para os próximos 1, 3 ou 5 anos |
| POST | `/api/agente` | Pergunta ao agente sobre os resultados |

Exemplo:

```bash
curl -X POST localhost:8080/api/simulacoes -H 'Content-Type: application/json' \
  -d '{"latitude":-14.866,"longitude":-40.839,"solo":"MISTO","culturaId":"milho"}'
```

## Safra, valores e planejamento

- **Hectares e safra:** com a área informada (`hectares`), cada ano da simulação traz safra, venda, custo e sobra estimados. A conta é simples: colheita de um ano bom × nota do ano (0 a 100) × hectares. Serve para dar ideia de grandeza; **não é** produtividade calibrada (tipo DSSAT).
- **Valores de exemplo:** produtividade, preço e custo ficam em `culturas.json` (campo `economia`) e hoje são **valores de exemplo**. Trocar por CONAB/IBGE antes de mostrar como real.
- **Cenário:** o agricultor ajusta temperatura, chuva e umidade da terra de um ano e vê a nota e a safra com esse clima. A regra da nota é a mesma (`AptidaoCalculadora`).
- **Planejamento:** a partir da cultura que o agricultor decidiu plantar, monta 1, 3 ou 5 anos. Regras em `core/servico/PlanejamentoService`: a escolhida entra no 1º ano e volta sempre que a rotação deixa; não se repete o mesmo grupo (`grupoRotacao`) em anos seguidos; no ano de rotação entra, entre as culturas que aguentam o clima previsto, a de maior sobra estimada; ano com previsão ruim ganha plano B e o cuidado contra o risco.

## Como o backend está organizado

- `core/` — Java puro, sem Spring. É onde está a regra do simulador:
  - `servico/AptidaoCalculadora` — compara o clima do ano com a faixa da cultura e dá nota 0–100.
  - `servico/SimuladorService` — monta a linha do tempo.
  - `servico/RecomendacaoService` — ranking das culturas.
  - `servico/SafraCalculadora` — estimativa de safra e dinheiro por hectare.
  - `servico/PlanejamentoService` — plano de plantio com rotação.
  - `clima/ClimaProvider` — de onde vem o clima. `MockClimaProvider` (falso) ou `HttpClimaProvider` (serviço do pessoal dos dados).
  - `agente/AgenteAnthropic` — agente com IA (API da Anthropic). `agente/AgenteDemonstracao` — responde por regras fixas. `agente/AgenteComReserva` — usa a IA e cai na demonstração se não houver chave, der erro ou demorar.
- `api/` — controllers REST.
- `config/` — liga as peças. Trocar a fonte de clima: `fieldshift.clima.fonte` em `application.properties`.

## Próximos passos

- [ ] Dados reais da NASA (ver contrato).
- [ ] Tabela de culturas com valores do EcoCrop (FAO) / ZARC (Embrapa).
- [ ] Produtividade, preço e custo reais (CONAB/IBGE) no lugar dos valores de exemplo.
- [x] Agente com LLM (chave da API só no backend).
- [ ] Deploy para a demo.
