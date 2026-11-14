import type { ClimaAnual, Nivel, ResultadoAno, Safra, Simulacao } from '../tipos'
import { peso, reais } from '../formato'
import { EpocasDoAno } from './EpocasDoAno'

interface Props {
  plantada: Simulacao
  alternativa: Simulacao | null
  ano: number
}

const SIMBOLO: Record<Nivel, string> = { BOM: '✓', ATENCAO: '!', RUIM: '✕' }
const FRASE: Record<Nivel, string> = { BOM: 'Ano bom', ATENCAO: 'Ano de atenção', RUIM: 'Ano ruim' }

function umidadeEmPalavras(u: number) {
  if (u < 0.25) return 'seca'
  if (u < 0.4) return 'normal'
  return 'úmida'
}

/** Resposta de uma cultura no ano: um selo grande com a cor, uma frase e os dois números que importam. */
function Veredito({ cultura, papel, resultado, safra }: {
  cultura: string
  papel: string
  resultado: ResultadoAno | undefined
  safra?: Safra
}) {
  if (!resultado) return null
  const nivel = resultado.nivel.toLowerCase()
  return (
    <article className={`veredito veredito--${nivel} ${resultado.projetado ? 'veredito--previsao' : ''}`}>
      <span className={`veredito__selo veredito__selo--${nivel}`} aria-hidden="true">{SIMBOLO[resultado.nivel]}</span>
      <div className="veredito__texto">
        <span className="veredito__cultura">{cultura} <small>{papel}</small></span>
        <strong>{FRASE[resultado.nivel]}</strong>
        <p>{resultado.motivo}</p>
      </div>
      {safra && (
        <dl className="veredito__numeros">
          <div>
            <dt>Colheita</dt>
            <dd>{peso(safra.producaoKg)}</dd>
          </div>
          <div className={safra.saldo < 0 ? 'veredito__saldo veredito__saldo--negativo' : 'veredito__saldo'}>
            <dt>{safra.saldo < 0 ? 'Prejuízo' : 'Sobra'}</dt>
            <dd>{reais(Math.abs(safra.saldo))}</dd>
          </div>
        </dl>
      )}
    </article>
  )
}

/** O clima do ano em três quadros grandes, sem termos técnicos. */
function Clima({ clima }: { clima: ClimaAnual }) {
  return (
    <dl className="clima-simples">
      <div>
        <span className="clima-simples__icone" aria-hidden="true">🌧️</span>
        <dt>Chuva no ano</dt>
        <dd>{Math.round(clima.precipitacao)} mm</dd>
      </div>
      <div>
        <span className="clima-simples__icone" aria-hidden="true">🌡️</span>
        <dt>Temperatura</dt>
        <dd>{clima.temperaturaMedia.toFixed(1)} °C</dd>
      </div>
      <div>
        <span className="clima-simples__icone" aria-hidden="true">🌱</span>
        <dt>Terra</dt>
        <dd>{umidadeEmPalavras(clima.umidadeSolo)}</dd>
      </div>
    </dl>
  )
}

export function DetalheAno({ plantada, alternativa, ano }: Props) {
  const a = plantada.anos.find((r) => r.ano === ano)
  const b = alternativa?.anos.find((r) => r.ano === ano)
  if (!a) return null
  const safraA = plantada.safras.find((s) => s.ano === ano)
  const safraB = alternativa?.safras.find((s) => s.ano === ano)

  return (
    <section className="detalhe">
      <div className="detalhe__cabecalho">
        <h2>{ano}</h2>
        <span className={`etiqueta ${a.projetado ? 'etiqueta--previsao' : ''}`}>
          {a.projetado ? 'previsão' : ano === plantada.anoAtual ? 'último ano completo' : 'já aconteceu'}
        </span>
      </div>

      <div className="vereditos">
        <Veredito cultura={plantada.cultura.nome} papel="o que você planta" resultado={a} safra={safraA} />
        {alternativa && (
          <Veredito cultura={alternativa.cultura.nome} papel="para comparar" resultado={b} safra={safraB} />
        )}
      </div>
      {(safraA || safraB) && (
        <p className="nota-rodape">Colheita e dinheiro são uma conta aproximada, com preços de exemplo.</p>
      )}

      <h3 className="detalhe__subtitulo">Como foi o tempo{a.projetado ? ' (previsão)' : ''}</h3>
      <Clima clima={a.clima} />
      <EpocasDoAno clima={a.clima} />
    </section>
  )
}
