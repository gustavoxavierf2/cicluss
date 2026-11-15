import { useEffect, useState } from 'react'
import type { CSSProperties } from 'react'
import type { IntervaloAnos } from '../intervalo'
import { normalizarIntervalo } from '../intervalo'

interface Props {
  primeiro: number
  ultimo: number
  anoAtual: number
  intervalo: IntervaloAnos
  aoMudar: (intervalo: IntervaloAnos) => void
}

/**
 * Barra do tempo com dois marcadores: escolhe um intervalo de anos (início e fim).
 * O passado é sólido (clima que aconteceu); o futuro é mais claro e tracejado,
 * e vai apagando, para mostrar que a previsão fica menos certa quanto mais longe.
 */
export function BarraAnos({ primeiro, ultimo, anoAtual, intervalo, aoMudar }: Props) {
  // As duas pontas como o agricultor as arrasta: uma pode passar pela outra, e aí elas trocam de papel.
  const [pontas, setPontas] = useState<[number, number]>([intervalo.inicio, intervalo.fim])
  const inicio = Math.min(...pontas)
  const fim = Math.max(...pontas)

  // O intervalo também muda por fora (atalhos, novo resultado): as pontas acompanham.
  useEffect(() => {
    setPontas((p) =>
      Math.min(...p) === intervalo.inicio && Math.max(...p) === intervalo.fim ? p : [intervalo.inicio, intervalo.fim])
  }, [intervalo.inicio, intervalo.fim])

  function arrastar(qual: 0 | 1, ano: number) {
    const novas: [number, number] = qual === 0 ? [ano, pontas[1]] : [pontas[0], ano]
    setPontas(novas)
    aoMudar(normalizarIntervalo({ inicio: novas[0], fim: novas[1] }, primeiro, ultimo))
  }

  function escolher(i: IntervaloAnos) {
    setPontas([i.inicio, i.fim])
    aoMudar(i)
  }

  const fracao = (v: number) => (v - primeiro) / (ultimo - primeiro)
  const estilo = {
    '--hoje': fracao(anoAtual),
    '--ini': fracao(inicio),
    '--fim': fracao(fim),
  } as CSSProperties

  const marcas: number[] = []
  for (let a = Math.ceil(primeiro / 5) * 5; a <= ultimo; a += 5) marcas.push(a)

  const anos = fim - inicio + 1
  const projetados = Math.max(0, fim - Math.max(anoAtual, inicio - 1))
  const distancia = fim - anoAtual
  const igual = (a: number, b: number) => inicio === a && fim === b

  return (
    <div className="barra-anos" style={estilo}>
      <div className="barra-anos__lados">
        <span>◀ passado: o clima que aconteceu</span>
        <span>futuro: previsão ▶</span>
      </div>

      <div className="barra-anos__area" role="group" aria-label="Período analisado">
        <output className={`barra-anos__balao barra-anos__balao--ini ${inicio > anoAtual ? 'barra-anos__balao--futuro' : ''}`}
          aria-hidden="true">
          {inicio}
        </output>
        {fim !== inicio && (
          <output className={`barra-anos__balao barra-anos__balao--fim ${fim > anoAtual ? 'barra-anos__balao--futuro' : ''}`}
            aria-hidden="true">
            {fim}
          </output>
        )}
        <div className="barra-anos__trilho" aria-hidden="true">
          <span className="barra-anos__passado" />
          <span className="barra-anos__futuro" />
          <span className="barra-anos__faixa" />
          <span className="barra-anos__hoje" />
        </div>
        {([0, 1] as const).map((qual) => (
          <input
            key={qual}
            type="range"
            className="barra-anos__input"
            min={primeiro}
            max={ultimo}
            step={1}
            value={pontas[qual]}
            onChange={(e) => arrastar(qual, Number(e.target.value))}
            aria-label={pontas[qual] < pontas[1 - qual] || (pontas[0] === pontas[1] && qual === 0) ? 'Ano inicial' : 'Ano final'}
            aria-valuetext={pontas[qual] > anoAtual ? `${pontas[qual]}, previsão` : `${pontas[qual]}`}
          />
        ))}
      </div>

      <div className="barra-anos__marcas" aria-hidden="true">
        {marcas.map((m) => (
          <span key={m} style={{ left: `calc(var(--polegar) / 2 + (100% - var(--polegar)) * ${fracao(m)})` }}>
            {m}
          </span>
        ))}
      </div>

      <p className="barra-anos__rotulo" aria-live="polite">
        De <b>{inicio}</b> até <b>{fim}</b> · {anos} {anos === 1 ? 'ano' : 'anos'}
        {projetados > 0 && ` · ${projetados} ${projetados === 1 ? 'projetado' : 'projetados'}`}
      </p>

      <div className="barra-anos__rodape">
        <div className="barra-anos__atalhos">
          <button type="button" className="link" onClick={() => escolher({ inicio: anoAtual, fim: anoAtual })}
            disabled={igual(anoAtual, anoAtual)}>
            último ano completo ({anoAtual})
          </button>
          <button type="button" className="link" onClick={() => escolher({ inicio: primeiro, fim: anoAtual })}
            disabled={igual(primeiro, anoAtual)}>
            só o que já aconteceu
          </button>
          <button type="button" className="link" onClick={() => escolher({ inicio: primeiro, fim: ultimo })}
            disabled={igual(primeiro, ultimo)}>
            todo o período
          </button>
        </div>
        {distancia > 0 && (
          <span className="barra-anos__incerteza">
            {distancia <= 5 ? 'Previsão próxima: mais confiável.' : distancia <= 10 ? 'Previsão média: use como tendência.' : 'Previsão distante: só tendência, pouca certeza.'}
          </span>
        )}
      </div>
    </div>
  )
}
