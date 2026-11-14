# Resumo do projeto — Ciclus (NASA Space Apps 2026, desafio "Field Shift: Adapting Farms with NASA Data")

## A ideia em uma frase
Um simulador em que o pequeno produtor do Sudoeste da Bahia clica no próprio terreno no mapa e vê, com
dados de satélite da NASA, como cada cultura teria se saído ali nos últimos 15 anos e como deve se sair
nos próximos 15.

## Usuário
- Pequeno agricultor do Sudoeste da Bahia (base de testes: Vitória da Conquista).
- Leigo: não lê gráfico técnico nem conhece termos como "evapotranspiração". Tudo na tela vira
  bom / atenção / ruim e uma frase simples.
- Pergunta que ele traz: "Vale a pena mudar o que eu planto? E se eu tivesse plantado outra coisa?"

## Dados de entrada
Da NASA (por coordenada, série histórica + projeção): temperatura (média, mín., máx.), precipitação,
umidade do solo, evapotranspiração. Fonte sugerida: NASA POWER (API por ponto) e SMAP para umidade do
solo — conferir com os datasets listados no desafio; a solução TEM que usar os dados indicados lá.
Do agricultor: localização (clique no mapa), tipo de solo (arenoso/argiloso/misto/não sei),
o que plantou por último e a cultura que quer testar.
Tabela de culturas (coração do simulador): faixas ideais e limites de temperatura e chuva, ciclo e solos,
a partir das fichas oficiais do EcoCrop (FAO) e do ZARC (Embrapa). Os cálculos usam sempre os dados da NASA.

## Funcionalidades
1. Simulador na linha do tempo: uma barra única de anos que começa no ano atual (no meio). Para trás:
   clima que aconteceu; para frente: previsão. O passado tem uma cor sólida e o futuro outra cor,
   tracejada e apagando, para mostrar que a previsão é menos certa.
2. Recomendação de plantio: ranking das culturas mais aptas para o terreno, com o motivo em uma linha.
3. Agente de IA: explica os resultados em linguagem simples e responde dúvidas. Ele NÃO calcula nada,
   só traduz os números que vieram da tabela + dados da NASA.

Acrescentadas depois (14/11/2026), a pedido da equipe:
4. Hectares e safra: o agricultor informa a área e vê, em cada ano, a safra estimada e uma ideia de
   venda, custo e sobra em reais.
5. Clima ajustável: no resultado, ele pode mexer na temperatura, na chuva e na umidade da terra para
   testar um cenário que ele imagina.
6. Planejamento com rotação: depois de decidir a cultura, ele pede um plano para 1, 3 ou 5 anos. O plano
   alterna culturas, aponta plano B e cuidados nos anos de risco e avisa quando há previsão de prejuízo.

## Saídas
Nota de aptidão por ano (bom/atenção/ruim), resumo "X anos bons, Y de risco", risco principal do ano
(seca, calor, frio, chuva demais, solo pouco indicado) e o clima do ano. A safra e os valores em reais
são uma estimativa simples (colheita de um ano bom × nota do ano × hectares), com preços e custos de
exemplo; produtividade calibrada (tipo DSSAT) continua fora de propósito.

## Diferencial
AgroClimate, SIMAGRI e DSSAT fazem algo parecido para técnicos e pesquisadores. O Ciclus é clique no
mapa, poucas perguntas e resposta em linguagem simples, com foco no pequeno produtor brasileiro.
Visual inspirado no painel do AgroClimate.

## Divisão
Outras pessoas da equipe cuidam dos dados reais da NASA e vão entregar no formato de
docs/contrato-dados.md. Até lá o site usa dados de demonstração.
