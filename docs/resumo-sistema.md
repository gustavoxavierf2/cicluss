# Ciclus — resumo do sistema

## Em uma frase

O Ciclus é um simulador de decisão para pequenos produtores rurais: o usuário marca seu terreno no mapa, informa o solo e a cultura, e recebe uma visão simples sobre **o que plantar, como o clima afeta a produção e qual rotação pode proteger melhor a terra e a renda**.

## Que problema o sistema resolve

O produtor normalmente precisa decidir com pouca informação:

- se vale a pena continuar plantando a mesma cultura;
- qual cultura é mais adequada para seu terreno;
- como o clima dos últimos anos afetou a produção;
- quais riscos podem aparecer nos próximos anos;
- como alternar culturas sem olhar apenas para a produtividade de um ano;
- quando uma mudança pode melhorar a renda sem degradar o solo.

O Ciclus transforma dados técnicos de clima, solo e culturas em uma resposta visual e direta, com os níveis **bom**, **atenção** e **ruim**. A proposta é apoiar a conversa e o planejamento — não substituir um agrônomo, uma análise de solo ou uma decisão financeira.

## Para quem é

O público principal é o pequeno produtor do Sudoeste da Bahia, especialmente quem:

- não tem acesso fácil a ferramentas técnicas de planejamento;
- não quer interpretar gráficos complexos;
- conhece o próprio terreno, mas pode não saber classificar o solo;
- precisa comparar alternativas de plantio considerando clima, rotação e resultado econômico.

## Como funciona

A tela tem o mapa ao fundo, o resultado e a conversa com o assistente à esquerda e as perguntas (planta, solo, área) à direita. O usuário pode preencher as perguntas direto ou deixar o assistente conduzir (item 7).

1. O usuário clica no local do terreno no mapa.
2. O sistema sugere um tipo de solo para aquele ponto e permite confirmar ou ajustar a informação.
3. O usuário informa a cultura plantada, uma cultura para comparação e a área em hectares.
4. O backend consulta ou calcula a série climática do local.
5. Cada cultura é avaliada ano a ano com base em temperatura, chuva, umidade do solo, evapotranspiração e tipo de solo.
6. O sistema apresenta os resultados em linguagem simples e permite explorar alternativas.

## O que o usuário consegue fazer

### 1. Ver a cultura ano a ano

O sistema mostra uma linha do tempo com até 15 anos observados e 15 anos projetados. Para cada ano, apresenta:

- nível de aptidão: bom, atenção ou ruim;
- nota e motivo principal;
- risco predominante, como seca, calor, frio, chuva excessiva ou solo pouco indicado;
- resumo de anos bons e anos de risco;
- chuva e temperatura por trimestre, quando disponíveis.

### 2. Comparar culturas

O produtor pode comparar a cultura atual com outra opção e entender se a troca tende a melhorar o resultado no terreno. Também recebe um ranking das culturas mais aptas, ordenado pelo desempenho observado no histórico e acompanhado de uma justificativa curta.

### 3. Comparar estratégias de rotação

Na aba de rotação, o sistema compara estratégias como:

- repetir a cultura atual;
- alternar a cultura atual com outra escolhida;
- fazer rotação com uma leguminosa;
- alternar três grupos de plantas: gramínea, leguminosa e raiz.

A comparação considera tanto o desempenho climático quanto sinais simples de saúde do solo:

- reposição de nitrogênio;
- redução de pragas e doenças;
- tempo de cobertura da terra;
- presença de palha e matéria orgânica.

O sistema sempre inclui como referência o que o produtor faz hoje.

### 4. Simular cenários

O usuário pode alterar temperatura, chuva e umidade do solo para responder perguntas do tipo:

> “O que aconteceria se este ano fosse mais quente e chovesse menos?”

A simulação usa a mesma regra de avaliação da simulação principal, permitindo visualizar o impacto de uma hipótese sem alterar os dados originais.

### 5. Estimar safra e resultado financeiro

Quando a área é informada, o sistema estima, para cada ano:

- produção em quilogramas;
- receita de venda;
- custo por hectare;
- sobra ou prejuízo estimado.

Esses valores são orientativos e dependem de preços, custos e produtividades cadastrados na tabela de culturas.

### 6. Simular a rotação em uma linha do tempo

Na aba de planejamento, o usuário escolhe 1, 3 ou 5 anos e desliza uma linha do tempo que começa em janeiro do próximo ano. Cada ano tem três épocas de quatro meses (jan–abr, mai–ago, set–dez). Para a época escolhida, o sistema mostra:

- a planta indicada e o papel da época: **safra** (colheita principal, na época mais chuvosa), **safrinha** (segunda colheita, de ciclo curto) ou **entressafra** (época seca, com planta de cobertura);
- a temperatura média e a umidade média do solo previstas para aqueles meses;
- um bloco curto de “por quê”, em linguagem de leigo: renda, adubo natural, proteção contra erosão, umidade guardada, quebra do ciclo de pragas.

O plano ainda alterna grupos de culturas entre os anos, aponta uma cultura reserva e o cuidado quando a previsão do ano não é boa, e resume a sobra ou o prejuízo estimado do período.

### 7. Conversar com o Raul, o assistente

O Raul (o tatu mascote) aparece em dois lugares: no botão **Assistente**, ao lado da logo, e no botão **Tire dúvidas com o Raul**. Ele tem dois papéis:

- **Guiar:** faz, uma por vez, as mesmas perguntas do menu (o que planta, comparar com, onde fica o terreno, como é a terra, hectares, anos do plano) e termina oferecendo o relatório. O usuário responde tocando nas opções, escrevendo ou falando; pelo botão Assistente o Raul também lê as perguntas em voz alta (depende do navegador).
- **Explicar:** depois do resultado, responde dúvidas em linguagem simples, por exemplo:

- “Por que esta cultura foi melhor?”
- “Vale a pena trocar?”
- “Qual foi o maior risco?”
- “O que acontece no futuro?”

O agente não calcula uma nova recomendação por conta própria: ele recebe os resultados produzidos pelo simulador e os traduz para o usuário.

### 8. Imprimir o Relatório de Rotação de Culturas

Tanto a conversa com o Raul quanto a aba de planejamento terminam no botão **Imprimir Relatório de Rotação de Culturas**. É uma folha de impressão (ou PDF, pelo “salvar como PDF” do navegador) com as cores do Ciclus e o Raul, pensada como guia prático:

- os dados informados: área, tipo de solo, localização, cultura principal e período;
- a rotação ao longo do tempo, ano a ano e época a época, com o porquê de cada plantio;
- uma explicação curta de por que não plantar sempre a mesma coisa (terra mais forte, menos praga, terra coberta, renda mais segura);
- o resumo de sobra estimada e anos com risco, com os avisos do plano.

## Dados e regras principais

- **Clima:** usa NASA POWER por coordenada, com temperatura, precipitação, umidade da zona de raiz e evapotranspiração, por ano e por trimestre. O sistema também mantém um modo de demonstração para desenvolvimento.
- **Solo:** SoilGrids, usado como sugestão de textura para o ponto selecionado.
- **Culturas:** tabela local com faixas ideais, limites absolutos, ciclo, grupo de rotação e dados econômicos.
- **Histórico e futuro:** o sistema usa dados observados no passado; os anos futuros são uma projeção ou tendência calculada a partir do histórico, não uma previsão meteorológica detalhada.
- **Aptidão:** a nota combina principalmente temperatura e chuva, com ajustes por umidade, relação entre chuva e evapotranspiração e compatibilidade do solo.
- **Classificação:** `70+` é bom, `40–69` é atenção e abaixo de `40` é ruim.
- **Épocas da rotação:** em cada ano, a época mais seca vira entressafra; das outras duas, a mais chuvosa é a safra e a outra é a safrinha. A safrinha só entra se chover o bastante e nunca repete o grupo da colheita anterior; senão a terra fica com cobertura.

## O que o sistema entrega na prática

Ao final, o produtor consegue responder rapidamente:

1. **O que plantei funcionou bem neste terreno?**
2. **Qual cultura parece mais adequada?**
3. **Quais anos ou condições representam maior risco?**
4. **Trocar ou alternar culturas pode proteger melhor a terra?**
5. **O que plantar em cada época dos próximos anos, e por quê?**
6. **Qual é o impacto aproximado na produção e na sobra?**

## Limites atuais

O Ciclus é um protótipo de apoio à decisão. Portanto:

- a tabela de culturas e os valores econômicos ainda podem conter dados de exemplo;
- as plantas de cobertura (braquiária, crotalária, milheto) são uma recomendação fixa, não avaliada pelo clima, e a safrinha não entra na conta do dinheiro;
- as épocas de quatro meses são uma simplificação: a data certa de plantio vem do ZARC;
- a temperatura e a umidade por época nos anos futuros repetem o padrão sazonal do histórico sobre a tendência do ano;
- a conversa por voz depende do navegador (funciona no Chrome e no Edge);
- a projeção futura representa uma tendência climática, não previsão de seca ou chuva de um ano específico;
- os sinais de saúde do solo são regras simplificadas, não uma análise laboratorial;
- a produtividade estimada não substitui um modelo agronômico calibrado para a propriedade;
- a umidade do solo e os critérios de risco ainda precisam de calibração regional;
- SMAP para umidade observada e MODIS/Landsat para indicadores como NDVI são evoluções previstas, não a base atual do sistema.

## Resumo executivo

O Ciclus transforma a pergunta **“o que devo plantar neste terreno?”** em uma decisão mais completa:

> **“Qual cultura ou rotação faz mais sentido para este lugar, considerando o clima que ocorreu, o risco dos próximos anos, a saúde da terra e uma estimativa de renda?”**

Seu diferencial é combinar mapa, dados climáticos, comparação de culturas, rotação e explicação em linguagem simples em uma única experiência voltada ao pequeno produtor.

## Trabalhos relacionados e diferencial

O Ciclus se relaciona com ferramentas que já apoiam decisões agrícolas, mas que normalmente respondem apenas a uma parte da decisão:

- **ZARC / Zarc Plantio Certo:** informa a janela oficial de plantio por cultura e município. É complementar ao Ciclus e importante para crédito, seguro e Proagro.
- **Broto, Inmet e FieldPRO:** oferecem previsão, balanço hídrico, sensores e serviços ligados à safra em andamento.
- **AgriMaps e Agrolly:** fazem recomendações de culturas e localização, sendo referências mais próximas na escolha do que plantar.
- **DSSAT:** simula crescimento e produtividade com mais detalhe, mas exige dados de manejo, solo, clima e calibração técnica.
- **AgroClimate:** trabalha com risco climático e planejamento de plantio para um público mais técnico.
- **FAO ABC-Map:** projeta a adequação de culturas em escala global e cenários de longo prazo.
- **NASA POWER, SoilGrids e EcoCrop:** são fontes de dados ambientais e agronômicos; não são concorrentes diretos.

### Diferencial do Ciclus

O Ciclus não tenta substituir essas ferramentas. Ele reúne parte dessas informações em uma experiência mais simples e orientada à decisão do pequeno produtor:

- começa pelo terreno no mapa, sem exigir conhecimento técnico;
- responde primeiro se vale a pena mudar a cultura, antes de definir a data de plantio;
- transforma dados complexos em respostas “bom”, “atenção” e “ruim”;
- compara a cultura atual com alternativas;
- avalia rotação, saúde do solo e renda em conjunto;
- mostra passado, tendência futura e rotação por época do ano em uma única jornada;
- conduz o produtor por conversa (texto ou voz) e explica os resultados em linguagem simples;
- entrega um relatório impresso que o produtor leva para o campo ou para o técnico.

Em resumo: **os aplicativos de zoneamento dizem quando plantar; o Ciclus ajuda a decidir se vale a pena mudar o que plantar**. Depois dessa escolha, o ZARC pode confirmar a janela oficial e as condições relacionadas a crédito e seguro.
