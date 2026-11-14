import type { CSSProperties } from 'react'

interface Props {
  primeiro: number
  ultimo: number
  anoAtual: number
  ano: number
  aoMudar: (ano: number) => void
}

/**
 * Barra única de anos (como um controle de volume). Começa no ano atual, no meio.
 * O passado é sólido (clima que aconteceu); o futuro é mais claro e tracejado,
 * e vai apagando, para mostrar que a previsão fica menos certa quanto mais longe.
 */
export function BarraAnos({ primeiro, ultimo, anoAtual, ano, aoMudar }: Props) {
  const fracao = (v: number) => (v - primeiro) / (ultimo - primeiro)
  const estilo = {
    '--hoje': fracao(anoAtual),
    '--pos': fracao(ano),
  } as CSSProperties

  const marcas: number[] = []
  for (let a = Math.ceil(primeiro / 5) * 5; a <= ultimo; a += 5) marcas.push(a)

  const futuro = ano > anoAtual
  const distancia = ano - anoAtual

  return (
    <div className="barra-anos" style={estilo}>
      <div className="barra-anos__lados">
        <span>◀ passado: o clima que aconteceu</span>
        <span>futuro: previsão ▶</span>
      </div>

      <div className="barra-anos__area">
        <output className={`barra-anos__balao ${futuro ? 'barra-anos__balao--futuro' : ''}`} aria-hidden="true">
          {ano}
        </output>
        <div className="barra-anos__trilho" aria-hidden="true">
          <span className="barra-anos__passado" />
          <span className="barra-anos__futuro" />
          <span className="barra-anos__hoje" />
        </div>
        <input
          type="range"
          className="barra-anos__input"
          min={primeiro}
          max={ultimo}
          step={1}
          value={ano}
          onChange={(e) => aoMudar(Number(e.target.value))}
          aria-label="Ano"
          aria-valuetext={futuro ? `${ano}, previsão` : `${ano}`}
        />
      </div>

      <div className="barra-anos__marcas" aria-hidden="true">
        {marcas.map((m) => (
          <span key={m} style={{ left: `calc(var(--polegar) / 2 + (100% - var(--polegar)) * ${fracao(m)})` }}>
            {m}
          </span>
        ))}
      </div>

      <div className="barra-anos__rodape">
        <button type="button" className="link" onClick={() => aoMudar(anoAtual)} disabled={ano === anoAtual}>
          voltar para hoje ({anoAtual})
        </button>
        {futuro && (
          <span className="barra-anos__incerteza">
            {distancia <= 5 ? 'Previsão próxima: mais confiável.' : distancia <= 10 ? 'Previsão média: use como tendência.' : 'Previsão distante: só tendência, pouca certeza.'}
          </span>
        )}
      </div>
    </div>
  )
}
