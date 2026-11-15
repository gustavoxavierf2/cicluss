import type { AgregadoPeriodo } from '../intervalo'
import type { Nivel, Planejamento, Ponto, SoloEstimado, TipoSolo } from '../tipos'
import { NOME_FASE, NOME_NIVEL, NOME_RISCO, NOME_SOLO, acaoDaJanela, iconeDaJanela, terraEmPalavras } from '../tipos'
import { hectaresEmTexto, peso, reais } from '../formato'
import logo from '../assets/logo-ciclus.png'
import tatuPlantando from '../assets/tatu-plantando.png'
import tatuNotebook from '../assets/tatu-notebook.png'

interface Props {
  plano: Planejamento
  feitoEm: string
  ponto: Ponto
  solo: TipoSolo
  soloEstimado: SoloEstimado | null
  /** nasa, http ou mock; null quando o servidor não informou. */
  fonteClima: string | null
  /**
   * Anos já observados no terreno, calculados pelo site com a simulação carregada.
   * `temCultura` é false quando a cultura do plano não foi simulada: aí só o clima vale.
   */
  historico: (AgregadoPeriodo & { temCultura: boolean }) | null
}

type Origem = 'observado' | 'tendencia' | 'estimativa' | 'regra'

const ORIGEM: Record<Origem, [string, string]> = {
  observado: ['Observado', 'clima que já aconteceu, medido pela NASA'],
  tendencia: ['Tendência', 'projeção do histórico para os próximos anos; não é previsão do tempo'],
  estimativa: ['Estimativa', 'conta de dinheiro com preços e custos de exemplo'],
  regra: ['Regra do Ciclus', 'regra simples do sistema, a confirmar no campo'],
}

const FONTE_CLIMA: Record<string, string> = {
  nasa: 'NASA POWER (temperatura, chuva e umidade da zona de raiz, por coordenada)',
  http: 'serviço de clima configurado no servidor',
  mock: 'dados de demonstração (não são reais)',
}

const MOTIVOS: [string, string, string][] = [
  ['🌱', 'Terra mais forte', 'Cada planta tira e devolve coisas diferentes. O feijão, por exemplo, deixa adubo natural (nitrogênio) para a próxima.'],
  ['🐛', 'Menos praga e doença', 'A praga de uma planta não acha comida quando entra outra no lugar. O ciclo dela quebra sem gastar com veneno.'],
  ['💧', 'Terra coberta guarda água', 'Planta de cobertura e palha fazem sombra no chão: a umidade fica e a chuva forte não leva a terra embora (erosão).'],
  ['💰', 'Renda mais segura', 'Com mais de uma colheita, se o clima castigar uma, a outra segura o faturamento do ano.'],
]

const SIMBOLO: Record<Nivel, string> = { BOM: '✓', ATENCAO: '!', RUIM: '✕' }

function Selo({ origem }: { origem: Origem }) {
  return <span className={`relatorio__origem relatorio__origem--${origem}`}>{ORIGEM[origem][0]}</span>
}

const mm = (v: number | null) => (v === null ? '—' : `${Math.round(v)} mm`)
const graus = (v: number | null) => (v === null ? '—' : `${v.toFixed(1).replace('.', ',')} °C`)
const umidade = (v: number | null) => (v === null ? '—' : `${Math.round(v * 100)}% (${terraEmPalavras(v)})`)

/**
 * Relatório de Rotação de Culturas: só aparece na impressão (ou "Salvar como PDF").
 * Não busca nem calcula valor agronômico novo: só organiza o que o plano e a simulação já trouxeram,
 * dizendo de onde vem cada parte (observado, tendência, estimativa ou regra).
 */
export function RelatorioRotacao({ plano, feitoEm, ponto, solo, soloEstimado, fonteClima, historico }: Props) {
  const { janelas, anos } = plano
  const informado = solo !== 'NAO_SEI'
  const tipoSolo = informado ? solo : soloEstimado?.tipo
  const nomeSolo = tipoSolo ? NOME_SOLO[tipoSolo].split(' (')[0] : 'Não informado'
  const origemSolo = informado ? 'informado por você' : soloEstimado ? `estimado pelo ${soloEstimado.fonte}` : 'sem informação'
  const periodo = `${janelas[0].inicio} a ${janelas[janelas.length - 1].fim}`

  const umidades = janelas.map((j) => j.umidadeSolo)
  const secas = janelas.filter((j) => j.fase === 'ENTRESSAFRA')
  const epocaSeca = secas.length > 0 ? `${secas[0].inicio.slice(0, 3)}–${secas[0].fim.slice(0, 3)}`.toLowerCase() : null
  const coberturas = [...new Set(janelas.filter((j) => !j.culturaId).map((j) => j.plantio.split(' (')[0]))]
  const anosDeRisco = anos.filter((a) => a.resultado.nivel !== 'BOM')
  const temDinheiro = plano.saldoTotal !== null

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

      <ul className="relatorio__legenda" aria-label="De onde vem cada informação">
        {(Object.keys(ORIGEM) as Origem[]).map((o) => (
          <li key={o}><Selo origem={o} /> {ORIGEM[o][1]}</li>
        ))}
      </ul>

      <section>
        <h2>1. Identificação do terreno</h2>
        <dl className="relatorio__dados">
          <div><dt>Localização (latitude, longitude)</dt><dd>{ponto.latitude.toFixed(4)}, {ponto.longitude.toFixed(4)}</dd></div>
          <div><dt>Área</dt><dd>{hectaresEmTexto(plano.hectares)}</dd></div>
          <div><dt>Tipo de solo</dt><dd>{nomeSolo} <small>{origemSolo}</small></dd></div>
        </dl>
      </section>

      <section>
        <h2>2. Resumo da simulação</h2>
        <dl className="relatorio__dados">
          <div><dt>Cultura principal</dt><dd>{plano.principal.nome}</dd></div>
          <div><dt>Período do plano</dt><dd>{periodo}</dd></div>
          <div><dt>Horizonte</dt><dd>{anos.length} {anos.length === 1 ? 'ano' : 'anos'}</dd></div>
        </dl>
        <p className="relatorio__fontes">
          <b>Fontes:</b> clima: {fonteClima ? FONTE_CLIMA[fonteClima] ?? fonteClima : 'não informada'}; solo: SoilGrids
          (ISRIC), quando estimado; culturas, ciclos, preços e custos: tabela de exemplo do Ciclus.
        </p>
      </section>

      <section>
        <h2>3. Condições da terra</h2>
        <dl className="relatorio__dados">
          {soloEstimado && (
            <div>
              <dt>Textura estimada <Selo origem="regra" /></dt>
              <dd>
                {Math.round(soloEstimado.argila)}% argila · {Math.round(soloEstimado.areia)}% areia ·{' '}
                {Math.round(soloEstimado.silte)}% silte
              </dd>
            </div>
          )}
          {historico && (
            <div>
              <dt>Umidade média da terra, {historico.inicio}–{historico.fim} <Selo origem="observado" /></dt>
              <dd>{umidade(historico.umidadeMedia)}</dd>
            </div>
          )}
          <div>
            <dt>Umidade nas épocas do plano <Selo origem="tendencia" /></dt>
            <dd>de {Math.round(Math.min(...umidades) * 100)}% a {Math.round(Math.max(...umidades) * 100)}%</dd>
          </div>
        </dl>
        <p className="relatorio__nota">
          {soloEstimado
            ? `A textura vem do ${soloEstimado.fonte}: é uma estimativa de mapa, não mede o seu talhão. `
            : 'Não há estimativa de textura para este ponto. '}
          A umidade é a da zona de raiz no modelo da NASA, não uma medição no terreno. Uma análise de solo de
          laboratório continua necessária para adubação e correção.
        </p>
      </section>

      <section>
        <h2>4. Desempenho climático</h2>
        {historico && (
          <>
            <h3>O que já aconteceu ({historico.inicio}–{historico.fim}) <Selo origem="observado" /></h3>
            <dl className="relatorio__dados">
              {historico.temCultura && (
                <div>
                  <dt>{historico.plantada.nome}: anos bons · de atenção · ruins</dt>
                  <dd>{historico.plantada.bons} · {historico.plantada.atencao} · {historico.plantada.ruins} <small>de {historico.anos}</small></dd>
                </div>
              )}
              <div><dt>Chuva média por ano</dt><dd>{mm(historico.chuvaMedia)}</dd></div>
              <div>
                <dt>Menor e maior chuva</dt>
                <dd>
                  {historico.chuvaMinima ? `${mm(historico.chuvaMinima.valor)} (${historico.chuvaMinima.ano})` : '—'} ·{' '}
                  {historico.chuvaMaxima ? `${mm(historico.chuvaMaxima.valor)} (${historico.chuvaMaxima.ano})` : '—'}
                </dd>
              </div>
              <div><dt>Temperatura média</dt><dd>{graus(historico.temperaturaMedia)}</dd></div>
            </dl>
            {historico.temCultura && historico.plantada.principalRisco && (
              <p className="relatorio__nota">
                Problema que mais se repetiu para {historico.plantada.nome.toLowerCase()}:{' '}
                <b>{NOME_RISCO[historico.plantada.principalRisco.risco]}</b> ({historico.plantada.principalRisco.anos} de {historico.anos} anos).
              </p>
            )}
          </>
        )}
        <h3>Anos do plano ({anos.length} {anos.length === 1 ? 'ano projetado' : 'anos projetados'}) <Selo origem="tendencia" /></h3>
        <table className="relatorio__tabela">
          <thead>
            <tr><th>Ano</th><th>Cultura da safra</th><th>Leitura</th><th>Chuva</th><th>Temperatura</th><th>Risco</th></tr>
          </thead>
          <tbody>
            {anos.map((a) => (
              <tr key={a.ano}>
                <td>{a.ano}</td>
                <td>{a.cultura.nome}</td>
                <td>{SIMBOLO[a.resultado.nivel]} {NOME_NIVEL[a.resultado.nivel]}</td>
                <td>{mm(a.resultado.clima.precipitacao)}</td>
                <td>{graus(a.resultado.clima.temperaturaMedia)}</td>
                <td>{NOME_RISCO[a.resultado.risco]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2>5. Plano de rotação <Selo origem="regra" /></h2>
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
        <h2>6. Riscos e cuidados</h2>
        <ul className="relatorio__avisos">
          {anosDeRisco.map((a) => (
            <li key={a.ano}>
              <b>{a.ano} ({a.cultura.nome.toLowerCase()}, leitura {NOME_NIVEL[a.resultado.nivel].toLowerCase()}):</b> {a.cuidado}
              {a.planoB && ` Cultura reserva: ${a.planoB.nome.toLowerCase()}.`}
            </li>
          ))}
          {epocaSeca && (
            <li>
              <b>Época seca ({epocaSeca}):</b> é quando a terra mais perde umidade.
              {coberturas.length > 0 && ` Mantenha a cobertura indicada (${coberturas.join('; ').toLowerCase()}) em vez de deixar a terra pelada.`}
            </li>
          )}
          {plano.avisos.map((aviso) => (
            <li key={aviso}>{aviso}</li>
          ))}
          <li>
            <b>Validação local:</b> confirme a janela oficial de plantio (ZARC), a disponibilidade de semente das
            plantas de cobertura na região e a adubação com um agrônomo ou técnico.
          </li>
        </ul>
      </section>

      {temDinheiro && (
        <section>
          <h2>7. Resumo econômico <Selo origem="estimativa" /></h2>
          <table className="relatorio__tabela">
            <thead>
              <tr><th>Ano</th><th>Safra principal</th><th>Colheita</th><th>Venda</th><th>Custo</th><th>Sobra</th></tr>
            </thead>
            <tbody>
              {anos.map((a) => a.safra && (
                <tr key={a.ano}>
                  <td>{a.ano}</td>
                  <td>{a.cultura.nome}</td>
                  <td>{peso(a.safra.producaoKg)}</td>
                  <td>{reais(a.safra.receita)}</td>
                  <td>{reais(a.safra.custo)}</td>
                  <td>{reais(a.safra.saldo)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr><td colSpan={5}>Total do período</td><td>{reais(plano.saldoTotal ?? 0)}</td></tr>
            </tfoot>
          </table>
          <p className="relatorio__nota">
            Só a safra principal entra na conta; safrinha e cobertura não. Os preços, custos e produtividades são
            de exemplo: servem para comparar ordens de grandeza, não para fechar orçamento.
          </p>
        </section>
      )}

      <footer className="relatorio__rodape">
        Este guia apoia a decisão; não substitui análise de solo, a recomendação oficial de plantio (ZARC) nem a
        avaliação de um agrônomo. O clima futuro é uma tendência calculada do histórico, não uma previsão do tempo.
      </footer>
    </article>
  )
}
