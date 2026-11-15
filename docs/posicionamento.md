# Posicionamento do Ciclus

Escrito em 14/11/2026. O clima e a tabela de culturas do protótipo ainda são de demonstração (`MockClimaProvider` e valores de exemplo no `culturas.json`). O que está dito aqui sobre o produto vale para o desenho dele, não para resultados reais.

## Em uma frase

Para o pequeno produtor do Sudoeste da Bahia que se pergunta se vale a pena mudar o que planta, o Ciclus é um simulador de clique no mapa que mostra, ano a ano, como cada cultura teria se saído naquele terreno nos últimos 15 anos e como tende a se sair nos próximos 15, em linguagem de gente. Os apps de zoneamento dizem quando plantar uma cultura que o produtor já escolheu. Os modelos de pesquisa estimam produtividade para quem sabe calibrá-los. O Ciclus responde à pergunta que vem antes das duas: qual cultura, e se esta terra vem piorando para ela.

## Quem usa e o que pergunta

Pequeno agricultor, leigo, que não lê gráfico técnico e nunca ouviu "evapotranspiração". Ele chega com uma dúvida: "vale a pena mudar o que eu planto? E se eu tivesse plantado outra coisa?". A tela devolve bom, atenção ou ruim e uma frase com o número que explica, por exemplo "Choveu 540 mm, abaixo do ideal para milho (600 a 1200 mm)".

## O que já existe

| Ferramenta | Quem usa | Pergunta que responde | Frente ao Ciclus |
|---|---|---|---|
| **ZARC / Zarc Plantio Certo** (MAPA e Embrapa) | Produtor e técnico | "Posso semear esta cultura agora com risco aceitável?" Janela de semeadura por município e cultura, em decêndios, com risco de 20%, 30% ou 40%. Cerca de 43 culturas | Complementar. O ZARC entra depois que a cultura foi escolhida. É a referência oficial e o que Proagro e seguro rural exigem |
| **Simulador de plantio do Broto** (Banco do Brasil) | Produtor cliente do BB | A mesma pergunta do ZARC, que ele usa por baixo | Mesma camada do ZARC. O Broto é antes de tudo um canal de crédito, seguro, insumo e barter |
| **Inmet e FieldPRO no Broto** | Cliente do BB | Previsão e balanço hídrico do Inmet; estação de 14 sensores com histórico de 5 anos de chuva e temperatura | Olham a safra em andamento. O Ciclus olha 15 anos para trás e 15 para frente, sem hardware |
| **AgriMaps e Agrolly** | Produtor e consultor | "Que cultura combina com este local?" | Concorrentes diretos da recomendação. Não verifiquei versão atual nem método de nenhum dos dois |
| **FAO ABC-Map** | Governos e técnicos | Adequação de cerca de 30 culturas por local até 2100, em dois cenários de emissão | Mesma família da nossa projeção, em escala global. Pelo que li sobre ela (não abri a página da FAO), não mostra tipo de solo do produtor nem compara culturas |
| **FAO EcoCrop** | Pesquisadores | Faixas ideais de temperatura e chuva por planta | Não é concorrente. É a fonte da nossa tabela de culturas |
| **AgroClimate** (Universidade da Flórida) | Técnicos e extensionistas do sudeste dos EUA | Planejador de data de plantio, risco por fase do El Niño, geada, doenças | Inspirou o visual. Para técnico, em inglês, para outra região |
| **DSSAT** | Pesquisadores | Simula crescimento e produtividade de mais de 42 culturas | Mais preciso e muito mais difícil de usar: pede clima diário, perfil de solo, manejo e calibração de variedade |
| **SIMAGRI** | n/d | n/d | Está no `resumo-projeto.md`, mas não achei fonte que diga o que é. Conferir antes de citar |

## O que é nosso

A volta no tempo. Para cada ano passado, o Ciclus mostra como a cultura teria ido com o clima que realmente aconteceu naquele ponto. Nenhuma das ferramentas que consegui consultar faz essa pergunta de "e se eu tivesse plantado outra coisa". ZARC e Broto olham risco para a próxima semeadura, e o ABC-Map olha o futuro.

A comparação lado a lado entre o que o produtor plantou e a alternativa, com a barra de anos bons, de atenção e ruins para cada uma.

A linguagem do leigo, do primeiro ao último pixel. O passado é sólido e o futuro é tracejado e apagando, para a incerteza aparecer sem explicação.

Um chat que só traduz. O modelo de linguagem recebe os números prontos (os 30 anos, os resumos, o ranking) e explica. Ele não calcula nada, então a resposta não inventa nota.

## Onde estamos iguais ou atrás

**Escala anual.** A nota usa temperatura média e chuva total do ano. Um ano com 900 mm bem distribuídos e um ano com 900 mm todos em março recebem a mesma nota. O ZARC trabalha por decêndio e por ciclo da cultura, e é por isso que é mais fiel à lavoura. Esta é a fraqueza mais séria do protótipo.

**Sem peso oficial.** O ZARC é exigido para acessar Proagro e subvenção do seguro rural. Se o Ciclus recomendar algo que o ZARC não zoneia para o município, o produtor perde crédito e proteção. O produto precisa deixar isso dito na tela, não só no pitch.

**Cobertura.** São 5 culturas na tabela (milho, feijão, café arábica, mandioca, sorgo) contra cerca de 43 do ZARC.

**Sem produtividade, preço, cultivar ou manejo.** Produtividade em kg/ha exigiria um modelo calibrado como o DSSAT e está fora de propósito de projeto. A recomendação olha só clima e solo.

**Regra de nota ainda não validada.** É deliberadamente simples: vale a pior entre temperatura e chuva, estresse hídrico tira 15% e solo inadequado tira 25%. Ninguém comparou isso com produtividade real nem com o ZARC.

**Projeção.** O método dos 15 anos futuros é do time de dados e ainda não está documentado. No mock é uma tendência linear (mais calor e menos chuva por ano). Quando os dados da NASA entrarem, a projeção vai precisar de um método explicado, ou a palavra "previsão" sai do produto.

## Como falar disso

A frase curta: os apps dizem quando plantar, o Ciclus diz se vale a pena mudar o que você planta.

Frente ao ZARC e ao Broto, o discurso é de sequência e não de rivalidade. O Ciclus ajuda a escolher a cultura, o ZARC confirma a data e o crédito e o seguro vêm depois. Essa ordem também abre uma porta de parceria, já que o Broto vive de conectar produtor a crédito e seguro.

Frente ao DSSAT e ao AgroClimate, a diferença é o público. Eles servem a quem tem formação técnica e dados de manejo. O Ciclus serve a quem só sabe onde fica o próprio terreno.

Evite "previsão" para os anos futuros e use "projeção" ou "tendência". Evite "dados da NASA" no pitch enquanto o mock estiver ligado.

## A conferir antes do pitch

- Rodar o ranking do Ciclus para Vitória da Conquista e comparar com as culturas zoneadas pelo ZARC no município. Se baterem, vira argumento. Se não, é preciso saber o porquê antes de alguém perguntar.
- Abrir o app do Broto e confirmar se o simulador de plantio e a previsão do Inmet seguem ativos. As fontes que achei são de 2020 a 2023.
- Descobrir o que é o SIMAGRI ou tirá-lo da lista de semelhantes.
- Abrir AgriMaps e Agrolly e ver o que entregam hoje.
- Ler a página do ABC-Map na FAO para confirmar o que a ferramenta mostra e a resolução espacial.

## Fontes

- [Zarc Plantio Certo ajuda produtor a saber a melhor época para plantar](https://revistacultivar.com.br/noticias/aplicativo-zarc-plantio-certo-ajuda-produtor-a-saber-qual-a-melhor-epoca-do-ano-para-plantar)
- [Zarc Plantio Certo na App Store](https://apps.apple.com/br/app/plantio-certo/id1518252333)
- [Broto na App Store](https://apps.apple.com/br/app/broto/id1619769755)
- [BB acerta criação de empresa para gerir o Broto (Forbes)](https://forbes.com.br/forbes-money/2022/06/bb-acerta-criacao-de-empresa-para-gerir-plataforma-ao-agronegocio-broto/)
- [Inmet disponibiliza previsão do tempo e balanço hídrico no Broto](https://portal.inmet.gov.br/noticias/inmet-disponibiliza-previsão-do-tempo-e-balanço-hídrico-na-plataforma-broto-do-banco-do-brasil)
- [BB firma parceria com a agtech FieldPRO](https://revistacultivar.com.br/noticias/banco-do-brasil-firma-parceria-com-agtech-para-levar-informacoes-climaticas-e-de-gestao-aos-produtores)
- [FAO: dados de adequação de culturas até 2100 (ABC-Map)](https://www.fao.org/newsroom/detail/new-data-shows-that-crops-like-wheat--coffee--beans-and-cassava-could-lose-half-of-the-best-land-for-growing-them-by-2100/en)
- [FAO EcoCrop](https://ecocrop.apps.fao.org/ecocrop/srv/en/home)
- [AgroClimate no USDA Climate Hubs](https://www.climatehubs.usda.gov/hubs/southeast/tools/agroclimate)
- [DSSAT, visão geral das ferramentas](https://dssat.net/tools-data-overview/)
- [AgriMaps na App Store](https://apps.apple.com/br/app/agrimaps/id1397860620)
- [Agrolly lança aplicativo para agricultores brasileiros](https://revistacultivar.com.br/noticias/agrolly-lanca-aplicativo-para-ajudar-os-agricultores-brasileiros-a-melhorar-o-cultivo)
