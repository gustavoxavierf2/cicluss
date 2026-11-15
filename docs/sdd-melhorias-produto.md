# SDD — Melhorias do painel agronômico e do relatório

**Status:** implementado na branch `feature/sdd-melhorias-produto` (2026-11-15)  
**Data:** 2026-11-15  
**Escopo:** frontend React/TypeScript, contratos já existentes da API e relatório de rotação

## 1. Objetivo

Tornar os resultados do Ciclus mais fáceis de interpretar e mais úteis para a decisão de um agricultor ou agrônomo, sem inventar dados que não existem no simulador. A experiência deve responder, nesta ordem:

1. Qual período estou analisando?
2. O que aconteceu nesse período e qual foi o impacto para a cultura?
3. Qual rotação devo considerar e por quê?
4. O que plantar primeiro, com quais riscos?
5. Quais informações do terreno e da simulação merecem atenção profissional?

## 2. Contexto atual

- A aba **Ano a ano** usa um único controle para selecionar um ano individual.
- O detalhe anual mistura veredito, safra, clima e épocas em uma sequência visual densa.
- **Rotação de culturas** e **Planejamento** são visões separadas, embora o planejamento seja a ação natural após escolher uma estratégia.
- **O que plantar** já possui uma lista ordenada, mas precisa assumir explicitamente o papel de ranking de decisão.
- O chat do Raul já existe, mas deve ficar fixo no canto inferior direito como ação persistente.
- O relatório impresso já recebe localização, solo, cultura, janelas, resumo financeiro e avisos.

Fontes de dados que podem ser usadas hoje:

- clima anual e trimestral da NASA POWER;
- tendência histórica para os anos futuros;
- tipo de solo estimado pelo SoilGrids ou informado pelo usuário;
- culturas, ciclos, grupos de rotação e valores econômicos de exemplo;
- notas de aptidão, safra estimada, riscos e sinais heurísticos de saúde do solo;
- planejamento de 1, 3 ou 5 anos e suas janelas de plantio.

## 3. Requisitos funcionais

### RF-01 — Seleção de intervalo de anos

Na aba **Ano a ano**, manter a barra temporal como interação principal, evoluindo-a para selecionar um intervalo inclusivo `[ano inicial, ano final]` com dois marcadores deslizantes. Não usar campos de texto para digitar o início e o fim.

- O usuário deve conseguir selecionar início e fim arrastando os marcadores na barra do histórico disponível.
- A barra deve mostrar visualmente a faixa selecionada entre os dois marcadores.
- O intervalo padrão deve preservar a experiência atual: começar no ano atual selecionado, com o fim igual ao mesmo ano.
- O sistema deve normalizar o intervalo quando o usuário arrastar os controles em ordem inversa.
- O rótulo deve mostrar claramente `De YYYY até YYYY` e a quantidade de anos incluídos.
- O usuário deve poder voltar rapidamente para `último ano completo` e para `todo o período disponível`.
- A marcação de passado, ano atual e tendência futura deve continuar visível.

### RF-02 — Agregação do intervalo

Quando o intervalo tiver mais de um ano, a tela deve calcular e exibir o agregado do período, sem eliminar o acesso ao detalhe de cada ano.

O agregado deve conter somente métricas derivadas dos dados existentes:

- quantidade e percentual de anos bons, de atenção e ruins;
- nota média da cultura principal e, quando existir, da alternativa;
- chuva média anual, temperatura média anual e umidade média do solo;
- chuva mínima e máxima do período;
- produção e saldo estimados acumulados, identificando que são estimativas;
- quantidade de anos projetados;
- principal risco repetido no intervalo, quando essa informação existir.

Regras:

- médias devem usar somente anos com valor disponível;
- dinheiro e produção devem ser somados, não promediados;
- o agregado deve indicar quando mistura histórico observado e tendência projetada;
- não apresentar correlação, causalidade ou recomendação agronômica que não esteja nos dados.

### RF-03 — Detalhe anual organizado por blocos

Com um intervalo selecionado, o usuário deve poder abrir um ano individual. O detalhe deve ser dividido em blocos independentes, com título, destaque visual e linguagem simples:

1. **Veredito da cultura principal** — nível, motivo e safra estimada.
2. **Comparação** — bloco separado para a cultura alternativa, somente quando existir.
3. **Condições do ano** — chuva, temperatura e umidade do solo.
4. **Distribuição ao longo do ano** — trimestres/épocas e período mais crítico.
5. **Limitações da leitura** — observação quando o ano for projetado ou quando os valores econômicos forem de exemplo.

Nenhum bloco deve depender visualmente de outro para ser entendido. O destaque do bloco deve corresponder ao tipo de informação: resultado, condição ambiental, comparação ou limitação.

### RF-04 — Unificar rotação e planejamento

Substituir as duas experiências independentes por uma visão **Rotação e planejamento**:

- apresentar a estratégia recomendada no topo;
- mostrar a sequência de culturas por ano/janela;
- explicar em uma frase por que a rotação foi escolhida;
- permitir comparar com a estratégia atual;
- permitir escolher horizonte de 1, 3 ou 5 anos;
- oferecer uma ação principal para abrir/atualizar o plano;
- manter os sinais de solo e os avisos como apoio secundário.

Ao mostrar recomendações, priorizar:

- **o que será plantado**;
- **em qual período**;
- **por que essa rotação**;
- **qual risco ou cuidado merece atenção**.

Informações detalhadas de heurística, saldo e sinais de solo podem ficar em detalhes expansíveis. A tela principal não deve exigir a leitura de todas elas para entender a recomendação.

### RF-05 — Ranking “O que plantar”

Transformar a aba **O que plantar** em um ranking visual de decisão, mantendo identidade própria em relação às outras telas.

Cada posição deve mostrar:

- posição e nome da cultura;
- indicação curta de compatibilidade com terreno/clima;
- anos bons e total de anos analisados;
- principal risco ou cuidado;
- ação **Comparar** e ação **Planejar**.

O ranking deve deixar explícito que ordena por aptidão climática e de solo, e não por maior preço ou lucro, enquanto a API não fornecer essa regra.

### RF-06 — Raul fixo no canto inferior direito

O botão para abrir o Raul deve ficar fixo no canto inferior direito da viewport quando o chat estiver fechado.

- Deve permanecer acessível durante a navegação entre abas.
- Deve respeitar áreas seguras em telas pequenas.
- Deve ter nome acessível, foco visível e estado aberto/fechado.
- Quando aberto, o painel deve permanecer ancorado no mesmo canto e não cobrir controles essenciais.
- O componente deve continuar funcionando sem voz e sem chave de IA, usando o modo de demonstração já existente.

### RF-07 — Relatório orientado à leitura agronômica

O relatório deve ser reorganizado para destacar o conhecimento da terra e das simulações, usando apenas dados disponíveis:

1. **Identificação do terreno:** localização, área e origem do tipo de solo (informado ou estimado).
2. **Resumo da simulação:** período, cultura principal, horizonte e fontes de dados.
3. **Condições da terra:** textura estimada, umidade disponível no modelo e limitações do SoilGrids, quando presentes.
4. **Desempenho climático:** anos bons/atenção/ruins, chuva, temperatura e anos projetados.
5. **Plano de rotação:** o que plantar em cada janela e motivo da escolha.
6. **Riscos e cuidados:** avisos do plano, época seca, cultura de cobertura e necessidade de validação local.
7. **Resumo econômico:** somente se disponível, sempre marcado como estimativa com preços de exemplo.

O relatório deve separar fato observado, tendência projetada, estimativa econômica e regra heurística. Deve manter o aviso de que não substitui análise de solo, recomendação oficial de plantio ou avaliação de agrônomo.

## 4. Requisitos não funcionais

- Preservar acessibilidade de teclado, foco visível, `aria-label` e leitura correta dos controles.
- Ser responsivo para desktop e telas estreitas.
- Não aumentar chamadas de API quando o agregado puder ser calculado no frontend a partir dos anos já carregados.
- Manter os contratos existentes sempre que o cálculo puder ser feito com os dados atuais.
- Não expor chave da Anthropic no frontend.
- Manter a distinção visual entre histórico observado e tendência futura.
- Todas as mensagens ao usuário devem permanecer em português do Brasil.

## 5. Modelo de interação proposto

### Aba Ano a ano

```text
Período analisado
1994 ───────●════════════════●────── 2024
            início           fim
De 1994 até 2024 · 31 anos · 5 projetados

[Resumo do período]
[Cultura principal] [Comparação] [Condições do clima] [Limitações]

Detalhar um ano: [ seletor/linha do tempo ]
```

O intervalo é o foco. O ano individual é uma segunda camada de investigação.

### Aba Rotação e planejamento

```text
Recomendação para o próximo ciclo
Milho → feijão → milho
Por quê: alterna grupos e mantém a terra coberta...

[1 ano] [3 anos] [5 anos]
Jan–Abr | cultura | motivo
Mai–Ago | cobertura | motivo
Set–Dez | safrinha | motivo

[Planejar esta rotação]
```

## 6. Contratos e cálculo

### Frontend

Adicionar estado de intervalo, mantendo `ano` para o detalhe:

```ts
interface IntervaloAnos {
  inicio: number
  fim: number
}
```

Criar funções puras para agregação, com testes para:

- intervalo de um ano;
- intervalo invertido;
- intervalo com anos projetados;
- ausência de safra/saldo;
- produção e saldo acumulados;
- médias e mínimos/máximos.

### Backend

Não é necessário alterar a API para a primeira entrega se `Simulacao.anos` já contiver todos os valores necessários. Uma evolução posterior poderá expor agregados no backend caso o volume de anos ou a regra de cálculo passe a ser compartilhada por outros clientes.

### Relatório

O componente de relatório deve receber dados já calculados pelo frontend ou um modelo derivado do `Planejamento` e das simulações. Não deve buscar dados diretamente nem criar valores agronômicos novos.

## 7. Critérios de aceite

- [x] É possível escolher um intervalo inclusivo e visualizar início, fim e quantidade de anos.
- [x] O intervalo de um ano apresenta resultado equivalente ao detalhe atual.
- [x] Intervalos de vários anos exibem contagens, médias, extremos e acumulados coerentes.
- [x] O usuário consegue acessar o detalhe de qualquer ano do intervalo.
- [x] O detalhe anual não apresenta informação aninhada de modo que impeça a leitura independente.
- [x] Rotação e planejamento estão na mesma jornada e a recomendação prioriza cultura, período e motivo.
- [x] O ranking mostra posição, aptidão, risco e ações sem sugerir que preço determina a ordem.
- [x] O Raul aparece no canto inferior direito, tem foco/rotulagem acessíveis e não bloqueia a tela em mobile.
- [x] O relatório identifica origem dos dados, separa observado/projetado/estimado e destaca cuidados relevantes para um agrônomo.
- [x] `npm run build`, `npm run lint` e `mvn test` passam (e `npm test`, novo, para as funções de intervalo).

## 8. Estratégia de implementação

1. Criar e testar funções puras de intervalo/agregação.
2. Evoluir `BarraAnos` para o controle de intervalo e preservar o detalhe individual.
3. Criar o componente de resumo do período e reorganizar `DetalheAno`.
4. Unificar navegação e componentes de rotação/planejamento.
5. Ajustar o ranking mantendo as ações existentes.
6. Reposicionar e validar o `ChatAgente` em viewport e teclado.
7. Reorganizar `RelatorioRotacao` e sua impressão.
8. Rodar build, lint e testes; revisar dados projetados e avisos no fluxo completo.

## 9. Fora de escopo

- Criar novos modelos climáticos ou transformar tendência em previsão.
- Substituir SoilGrids por análise de solo de laboratório.
- Calibrar produtividade, preço ou custo com dados externos.
- Integrar SMAP, MODIS, Landsat, ZARC, CONAB ou IBGE nesta entrega.
- Alterar a regra agronômica de aptidão sem uma decisão específica e dados de validação.

## 10. Decisões adotadas na implementação

Estavam pendentes; valem até decisão em contrário.

1. **Intervalo padrão:** só o último ano completo (como pede o RF-01), para a tela abrir igual a antes.
2. **Anos futuros no agregado:** entram só se o usuário arrastar o marcador até eles. O atalho "só o que já aconteceu" fica ao lado de "todo o período", e o resumo avisa quando mistura observado e previsão.
3. **Ordem do ranking:** continua a que a API devolve (aptidão no histórico). O frontend não reordena.
4. **Público do relatório:** o agricultor, em linguagem simples, mas na ordem e com os selos de origem que um agrônomo espera. O quadro do Raul sobre rotação foi mantido.

## 11. Notas de implementação

- Agregação em `frontend/src/intervalo.ts`, testada em `frontend/testes/intervalo.test.ts` com o executor de testes do próprio Node (`npm test`), sem dependência nova.
- A barra usa dois controles nativos sobrepostos: teclado e leitor de tela funcionam como em qualquer controle deslizante.
- Rotação e planejamento continuam com duas chamadas à API (`/api/estrategias` e `/api/planejamentos`); só a tela foi unida. "Planejar esta rotação" abre o plano com a primeira cultura da rotação recomendada, mas a sequência do plano segue a regra do `PlanejamentoService`, que pode diferir da estratégia.
- Com o Raul aberto no desktop, o menu encolhe e ganha rolagem para nada ficar por baixo do chat.
- O desempenho histórico do relatório só aparece por cultura quando a cultura do plano é uma das duas já simuladas; o clima aparece sempre.

