import type { ClimaAnual, ResultadoAno, Simulacao } from '../tipos'
import { NOME_NIVEL } from '../tipos'

interface Props {
  plantada: Simulacao
  alternativa: Simulacao | null
  ano: number
}

function umidadeEmPalavras(u: number) {
  if (u < 0.25) return 'seca'
  if (u < 0.4) return 'normal'
  return 'úmida'
}

function Coluna({ titulo, resultado }: { titulo: string; resultado: ResultadoAno | undefined }) {
  if (!resultado) return null
  return (
    <div className={`resultado resultado--${resultado.nivel.toLowerCase()} ${resultado.projetado ? 'resultado--previsao' : ''}`}>
      <span className="resultado__titulo">{titulo}</span>
      <div className="resultado__nota">
        <strong>{NOME_NIVEL[resultado.nivel]}</strong>
        <span>nota {resultado.nota}</span>
      </div>
      <p>{resultado.motivo}</p>
    </div>
  )
}

function Clima({ clima }: { clima: ClimaAnual }) {
  const sufixo = clima.projetado ? ' (prev.)' : ''
  return (
    <dl className="clima">
      <div>
        <dt>Chuva no ano{sufixo}</dt>
        <dd>{Math.round(clima.precipitacao)} mm</dd>
      </div>
      <div>
        <dt>Temperatura média{sufixo}</dt>
        <dd>{clima.temperaturaMedia.toFixed(1)} °C</dd>
      </div>
      <div>
        <dt>Terra</dt>
        <dd>{umidadeEmPalavras(clima.umidadeSolo)}</dd>
      </div>
      <div>
        <dt>Água que a planta perde</dt>
        <dd>{Math.round(clima.evapotranspiracao)} mm</dd>
      </div>
    </dl>
  )
}

export function DetalheAno({ plantada, alternativa, ano }: Props) {
  const a = plantada.anos.find((r) => r.ano === ano)
  const b = alternativa?.anos.find((r) => r.ano === ano)
  if (!a) return null

  return (
    <section className="detalhe">
      <div className="detalhe__cabecalho">
        <h2>{ano}</h2>
        <span className={`etiqueta ${a.projetado ? 'etiqueta--previsao' : ''}`}>
          {a.projetado ? 'previsão' : ano === plantada.anoAtual ? 'hoje: último ano completo' : 'clima que aconteceu'}
        </span>
      </div>
      <div className="detalhe__colunas">
        <Coluna titulo={`${plantada.cultura.nome} (o que você plantou)`} resultado={a} />
        {alternativa && <Coluna titulo={`${alternativa.cultura.nome} (e se?)`} resultado={b} />}
      </div>
      <Clima clima={a.clima} />
    </section>
  )
}
