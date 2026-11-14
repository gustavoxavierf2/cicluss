# Contrato de dados — o que o site espera receber

Este documento é para quem está cuidando dos dados da NASA. O site já funciona com **dados falsos**; quando vocês entregarem os dados neste formato, é só trocar uma configuração e o site passa a usar os reais.

São duas entregas:

1. **Clima por ano** de um ponto do mapa (série histórica + projeção).
2. **Tabela de culturas** (faixas ideais e limites de cada cultura).

Regra do desafio: os números de clima têm que vir dos **dados da NASA indicados no desafio**. EcoCrop/ZARC entram só na tabela de culturas.

---

## 1. Clima por ano

### Como o site pede

O backend faz uma chamada HTTP para o serviço de vocês (pode ser Python/Flask/FastAPI, o que preferirem):

```
GET {url}/clima?lat=-14.8660&lon=-40.8390&inicio=2011&fim=2040&anoAtual=2025
```

| Parâmetro | Significado |
|---|---|
| `lat`, `lon` | Ponto que o agricultor clicou no mapa (graus decimais, 4 casas) |
| `inicio`, `fim` | Primeiro e último ano, inclusive. Hoje: 15 anos para trás e 15 para frente |
| `anoAtual` | Último ano completo com dado observado. Anos **maiores** que ele são previsão |

### O que o serviço devolve

Uma lista JSON com **exatamente um item por ano**, de `inicio` até `fim`, em ordem:

```json
[
  {
    "ano": 2011,
    "projetado": false,
    "temperaturaMedia": 20.4,
    "temperaturaMaxima": 27.6,
    "temperaturaMinima": 14.5,
    "precipitacao": 812,
    "umidadeSolo": 0.34,
    "evapotranspiracao": 1003
  },
  {
    "ano": 2026,
    "projetado": true,
    "temperaturaMedia": 21.1,
    "temperaturaMaxima": 28.2,
    "temperaturaMinima": 15.0,
    "precipitacao": 760,
    "umidadeSolo": 0.31,
    "evapotranspiracao": 1040
  }
]
```

| Campo | Unidade | Como agregar no ano |
|---|---|---|
| `ano` | ano | — |
| `projetado` | `true`/`false` | `false` para dado observado, `true` para previsão |
| `temperaturaMedia` | °C | média das médias diárias |
| `temperaturaMaxima` | °C | média das máximas diárias |
| `temperaturaMinima` | °C | média das mínimas diárias |
| `precipitacao` | mm | **soma** da chuva do ano |
| `umidadeSolo` | 0 a 1 | média do ano da umidade da zona de raiz (0 = seco, 1 = saturado) |
| `evapotranspiracao` | mm | **soma** do ano |

Campo opcional `trimestres`: lista com 4 itens (`jan–mar`, `abr–jun`, `jul–set`, `out–dez`), cada um com `nome`, `chuva` (soma em mm) e `temperaturaMedia` (°C). Com ele o site mostra a época do ano que pesou; sem ele, mostra só o total do ano.

Observações:

- Se a fonte vier em mm/dia, multiplique pela quantidade de dias (ou some os diários).
- Se a fonte de umidade do solo vier em outra escala (ex.: m³/m³), avisem que a gente ajusta o limite de "solo seco" no cálculo.
- Se faltar dado em algum ano, **não pule o ano**: mande o item com o melhor valor disponível e avisem. O backend recusa séries com anos faltando.
- Respondam com status 200 e `Content-Type: application/json`. Em erro, qualquer status diferente de 200 com uma mensagem no corpo.

### Como ligar no site

Em `backend/src/main/resources/application.properties`:

```properties
fieldshift.clima.fonte=http
fieldshift.clima.url=http://localhost:8000
```

### Plano B (sem servidor)

Se não der tempo de subir um serviço, mandem um arquivo JSON por ponto no mesmo formato, e a gente lê do disco.

---

## 2. Tabela de culturas

Arquivo: `backend/src/main/resources/culturas.json`. Hoje ele tem **valores de exemplo** (campo `fonte` avisa). Cada cultura:

```json
{
  "id": "milho",
  "nome": "Milho",
  "temperatura": { "idealMin": 18, "idealMax": 33, "absMin": 10, "absMax": 47 },
  "chuva": { "idealMin": 600, "idealMax": 1200, "absMin": 400, "absMax": 1800 },
  "solos": ["ARGILOSO", "MISTO"],
  "cicloDias": 120,
  "fonte": "EcoCrop (FAO), ficha de Zea mays, consultada em 14/11/2026",
  "grupoRotacao": "GRAMINEA",
  "economia": { "produtividadeKgHa": 5000, "precoPorKg": 1.00, "custoPorHa": 3500, "fonte": "CONAB, safra 2025/26, Bahia" }
}
```

| Campo | De onde tirar (EcoCrop) |
|---|---|
| `temperatura.idealMin` / `idealMax` | Optimal temperature, min / max (°C) |
| `temperatura.absMin` / `absMax` | Absolute temperature, min / max (°C) |
| `chuva.idealMin` / `idealMax` | Optimal annual rainfall, min / max (mm) |
| `chuva.absMin` / `absMax` | Absolute annual rainfall, min / max (mm) |
| `solos` | `ARENOSO`, `ARGILOSO` e/ou `MISTO`, a partir da textura de solo da ficha |
| `cicloDias` | Crop cycle (dias); para perene, use 365 |
| `fonte` | Ficha e data consultada (para citar no pitch) |
| `grupoRotacao` | `GRAMINEA`, `LEGUMINOSA`, `RAIZ` ou `PERENE`. Usado na rotação: o plano não repete o mesmo grupo em anos seguidos; `PERENE` não entra em rotação |
| `economia.produtividadeKgHa` | Colheita de um **ano bom** na região, em kg por hectare (CONAB/IBGE) |
| `economia.precoPorKg` | Preço de venda ao produtor, em R$ por kg |
| `economia.custoPorHa` | Custo para plantar e colher um hectare, em R$ |
| `economia.fonte` | De onde vieram os três valores e a data |

Os valores de `economia` hoje são **de exemplo**. O site estima a safra como `produtividadeKgHa × nota do ano / 100 × hectares` e mostra venda, custo e sobra a partir daí.

A ordem precisa ser `absMin ≤ idealMin ≤ idealMax ≤ absMax`, senão o backend não sobe.

---

## Como o site usa esses dados (para conferirem se faz sentido)

Para cada ano e cultura:

1. Temperatura média e chuva do ano recebem nota de 0 a 1: 1 dentro da faixa ideal, caindo em linha reta até 0 no limite absoluto.
2. Vale a **pior** das duas.
3. Solo seco (`umidadeSolo < 0,25`) ou chuva menor que 60% da evapotranspiração tira 15%.
4. Solo não indicado para a cultura tira 25%.
5. Nota final de 0 a 100: **70+ bom**, **40–69 atenção**, **abaixo de 40 ruim**.

O código está em `backend/src/main/java/br/com/fieldshift/core/servico/AptidaoCalculadora.java`.
