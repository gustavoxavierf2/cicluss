import type { Planejamento, Ponto, SoloEstimado, TipoSolo } from '../tipos'
import { NOME_FASE, NOME_SOLO, acaoDaJanela, iconeDaJanela } from '../tipos'
import { hectaresEmTexto, reais } from '../formato'
import logo from '../assets/logo-ciclus.png'
import tatuPlantando from '../assets/tatu-plantando.png'
import tatuNotebook from '../assets/tatu-notebook.png'

interface Props {
  plano: Planejamento
  feitoEm: string
  ponto: Ponto
  solo: TipoSolo
  soloEstimado: SoloEstimado | null
}

const MOTIVOS: [string, string, string][] = [
  ['🌱', 'Terra mais forte', 'Cada planta tira e devolve coisas diferentes. O feijão, por exemplo, deixa adubo natural (nitrogênio) para a próxima.'],
  ['🐛', 'Menos praga e doença', 'A praga de uma planta não acha comida quando entra outra no lugar. O ciclo dela quebra sem gastar com veneno.'],
  ['💧', 'Terra coberta guarda água', 'Planta de cobertura e palha fazem sombra no chão: a umidade fica e a chuva forte não leva a terra embora (erosão).'],
  ['💰', 'Renda mais segura', 'Com mais de uma colheita, se o clima castigar uma, a outra segura o faturamento do ano.'],
]

/** Relatório de Rotação de Culturas: só aparece na impressão (ou "Salvar como PDF"). */
export function RelatorioRotacao({ plano, feitoEm, ponto, solo, soloEstimado }: Props) {
  const { janelas, anos } = plano
  const nomeSolo = solo === 'NAO_SEI'
    ? soloEstimado
      ? `${NOME_SOLO[soloEstimado.tipo].split(' (')[0]} (estimado pelo mapa de solos)`
      : 'Não informado'
    : NOME_SOLO[solo].split(' (')[0]
  const periodo = `${janelas[0].inicio} a ${janelas[janelas.length - 1].fim}`

  return (
    <article className="relatorio">
      <header className="relatorio__topo">
        <img src={logo} alt="Ciclus" width="150" height="45" />
        <div>
          <h1>Relatório de Rotação de Culturas</h1>
          <p>Guia prático para {anos.length === 1 ? 'o próximo ano' : `os próximos ${anos.length} anos`} · feito em {feitoEm}</p>
        </div>
        <img src={tatuPlantando} alt="Tatu Raul, mascote do Ciclus" width="110" height="108" />
      </header>

      <section>
        <h2>O seu terreno</h2>
        <dl className="relatorio__dados">
          <div><dt>Área</dt><dd>{hectaresEmTexto(plano.hectares)}</dd></div>
          <div><dt>Tipo de solo</dt><dd>{nomeSolo}</dd></div>
          <div><dt>Localização</dt><dd>{ponto.latitude.toFixed(4)}, {ponto.longitude.toFixed(4)}</dd></div>
          <div><dt>Cultura principal</dt><dd>{plano.principal.nome}</dd></div>
          <div><dt>Período</dt><dd>{periodo}</dd></div>
        </dl>
      </section>

      <section>
        <h2>A rotação ao longo do tempo</h2>
        <div className="relatorio__linha">
          {anos.map((a) => (
            <div key={a.ano} className="relatorio__ano">
              <b>{a.ano}</b>
              {janelas.filter((j) => j.ano === a.ano).map((j) => (
                <span key={j.inicio} className={`fase--${j.fase.toLowerCase()}`}>
                  <span aria-hidden="true">{iconeDaJanela(j)}</span> {j.plantio.split(' (')[0]}
                  <small>{j.inicio.slice(0, 3)}–{j.fim.slice(0, 3)} · {NOME_FASE[j.fase]}</small>
                </span>
              ))}
            </div>
          ))}
        </div>
      </section>

      <section>
        <h2>O que fazer em cada época</h2>
        <ol className="relatorio__epocas">
          {janelas.map((j, i) => (
            <li key={j.inicio}>
              <span className="relatorio__quando">{j.inicio} a {j.fim}</span>
              <span>
                <strong>{iconeDaJanela(j)} {acaoDaJanela(janelas, i)}</strong>
                <span><b>Por quê?</b> {j.porque}</span>
              </span>
            </li>
          ))}
        </ol>
      </section>

      <section className="relatorio__motivos">
        <img src={tatuNotebook} alt="" width="120" height="112" />
        <div>
          <h2>Por que não plantar sempre a mesma coisa?</h2>
          <ul>
            {MOTIVOS.map(([icone, titulo, texto]) => (
              <li key={titulo}>
                <span aria-hidden="true">{icone}</span>
                <span><b>{titulo}.</b> {texto}</span>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section>
        <h2>Resumo do período</h2>
        <dl className="relatorio__dados">
          {plano.saldoTotal !== null && (
            <div>
              <dt>{plano.saldoTotal < 0 ? 'Prejuízo estimado' : 'Sobra estimada'} (safra principal)</dt>
              <dd>{reais(Math.abs(plano.saldoTotal))}</dd>
            </div>
          )}
          <div><dt>Anos com risco de clima</dt><dd>{plano.anosDeRisco} de {anos.length}</dd></div>
        </dl>
        <ul className="relatorio__avisos">
          {plano.avisos.map((aviso) => (
            <li key={aviso}>{aviso}</li>
          ))}
        </ul>
      </section>

      <footer className="relatorio__rodape">
        Este guia apoia a sua decisão; não substitui um agrônomo nem a análise de solo. O clima é uma tendência
        calculada com dados da NASA, não uma previsão do tempo, e os valores em dinheiro usam preços de exemplo.
        Antes de plantar, confira a janela oficial de plantio (ZARC).
      </footer>
    </article>
  )
}
