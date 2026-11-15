import type { AgregadoPeriodo, ResumoCultura } from '../intervalo'
import type { Simulacao } from '../tipos'
import { NOME_RISCO, terraEmPalavras } from '../tipos'
import { peso, reais } from '../formato'

interface Props {
  agregado: AgregadoPeriodo
  plantada: Simulacao
  ano: number
  aoEscolherAno: (ano: number) => void
}

const porcento = (parte: number, total: number) => `${Math.round((100 * parte) / Math.max(total, 1))}%`

/** Como uma cultura foi no período: contagem de anos, nota média, risco repetido e somas estimadas. */
function Cultura({ r, papel, tipo }: { r: ResumoCultura; papel: string; tipo: 'resultado' | 'comparacao' }) {
  const total = r.bons + r.atencao + r.ruins
  return (
    <section className={`bloco bloco--${tipo}`}>
      <h3 className="bloco__titulo">{r.nome} <small>{papel}</small></h3>
      <div className="barra" role="img" aria-label={`${r.bons} anos bons, ${r.atencao} de atenção e ${r.ruins} ruins`}>
        <span className="barra__bom" style={{ flexGrow: r.bons }} />
        <span className="barra__atencao" style={{ flexGrow: r.atencao }} />
        <span className="barra__ruim" style={{ flexGrow: r.ruins }} />
      </div>
      <ul className="periodo__contagem">
        <li><b className="cor-bom">{r.bons}</b> {r.bons === 1 ? 'bom' : 'bons'} ({porcento(r.bons, total)})</li>
        <li><b className="cor-atencao">{r.atencao}</b> de atenção ({porcento(r.atencao, total)})</li>
        <li><b className="cor-ruim">{r.ruins}</b> {r.ruins === 1 ? 'ruim' : 'ruins'} ({porcento(r.ruins, total)})</li>
      </ul>
      <dl className="quadros quadros--tres">
        <div>
          <dt>Nota média</dt>
          <dd>{r.notaMedia} <small>de 100</small></dd>
        </div>
        {r.producaoKg !== null && (
          <div>
            <dt>Colheita somada</dt>
            <dd>{peso(r.producaoKg)}</dd>
          </div>
        )}
        {r.saldo !== null && (
          <div className={r.saldo < 0 ? 'quadros__negativo' : undefined}>
            <dt>{r.saldo < 0 ? 'Prejuízo somado' : 'Sobra somada'}</dt>
            <dd>{reais(Math.abs(r.saldo))}</dd>
          </div>
        )}
      </dl>
      <p>
        {r.principalRisco
          ? <>Problema que mais se repetiu: <b>{NOME_RISCO[r.principalRisco.risco]}</b> ({r.principalRisco.anos} de {total} anos).</>
          : 'Nenhum problema de clima ou de terra se repetiu no período.'}
      </p>
    </section>
  )
}

/** Resumo de um intervalo de vários anos, com atalho para abrir cada ano. */
export function ResumoPeriodo({ agregado: g, plantada, ano, aoEscolherAno }: Props) {
  const anos = plantada.anos.filter((a) => a.ano >= g.inicio && a.ano <= g.fim)
  const temDinheiro = g.plantada.saldo !== null || (g.alternativa?.saldo ?? null) !== null

  return (
    <section className="periodo">
      <div className="detalhe__cabecalho">
        <h2>{g.inicio}–{g.fim}</h2>
        <span className={`etiqueta ${g.projetados > 0 ? 'etiqueta--previsao' : ''}`}>
          {g.anos} anos{g.projetados > 0 && ` · ${g.projetados} de previsão`}
        </span>
      </div>

      <Cultura r={g.plantada} papel="o que você planta" tipo="resultado" />
      {g.alternativa && <Cultura r={g.alternativa} papel="para comparar" tipo="comparacao" />}

      <section className="bloco bloco--ambiente">
        <h3 className="bloco__titulo">Clima do período</h3>
        <dl className="clima-simples">
          <div>
            <span className="clima-simples__icone" aria-hidden="true">🌧️</span>
            <dt>Chuva por ano</dt>
            <dd>{g.chuvaMedia === null ? '—' : `${Math.round(g.chuvaMedia)} mm`}</dd>
          </div>
          <div>
            <span className="clima-simples__icone" aria-hidden="true">🌡️</span>
            <dt>Temperatura</dt>
            <dd>{g.temperaturaMedia === null ? '—' : `${g.temperaturaMedia.toFixed(1)} °C`}</dd>
          </div>
          <div>
            <span className="clima-simples__icone" aria-hidden="true">🌱</span>
            <dt>Umidade da terra</dt>
            <dd>
              {g.umidadeMedia === null ? '—' : <>{Math.round(g.umidadeMedia * 100)}% <small>{terraEmPalavras(g.umidadeMedia)}</small></>}
            </dd>
          </div>
        </dl>
        {g.chuvaMinima && g.chuvaMaxima && (
          <p>
            Ano de menos chuva: <b>{g.chuvaMinima.ano}</b> ({Math.round(g.chuvaMinima.valor)} mm). Ano de mais chuva:{' '}
            <b>{g.chuvaMaxima.ano}</b> ({Math.round(g.chuvaMaxima.valor)} mm).
          </p>
        )}
      </section>

      {(g.projetados > 0 || temDinheiro) && (
        <section className="bloco bloco--limite">
          <h3 className="bloco__titulo">Limitações da leitura</h3>
          <ul>
            {g.misturaProjecao && (
              <li>Este período junta {g.anos - g.projetados} anos que já aconteceram com {g.projetados} de previsão.</li>
            )}
            {g.projetados > 0 && <li>Os anos de previsão são uma tendência do histórico, não uma previsão do tempo.</li>}
            {temDinheiro && <li>Colheita e dinheiro são somas de estimativas, com preços de exemplo.</li>}
          </ul>
        </section>
      )}

      <section className="periodo__anos">
        <h3 className="detalhe__subtitulo">Detalhar um ano</h3>
        <div className="anos-chips" role="group" aria-label="Escolher um ano do período">
          {anos.map((a) => (
            <button key={a.ano} type="button" aria-pressed={a.ano === ano}
              className={`ano-chip ano-chip--${a.nivel.toLowerCase()} ${a.projetado ? 'ano-chip--previsao' : ''} ${a.ano === ano ? 'ano-chip--atual' : ''}`}
              onClick={() => aoEscolherAno(a.ano)}>
              {a.ano}
            </button>
          ))}
        </div>
      </section>
    </section>
  )
}
