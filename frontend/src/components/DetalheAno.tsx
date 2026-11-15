import type { ClimaAnual, Nivel, ResultadoAno, Safra, Simulacao } from '../tipos'
import { terraEmPalavras } from '../tipos'
import { peso, reais } from '../formato'
import { EpocasDoAno } from './EpocasDoAno'

interface Props {
  plantada: Simulacao
  alternativa: Simulacao | null
  ano: number
}

const SIMBOLO: Record<Nivel, string> = { BOM: '✓', ATENCAO: '!', RUIM: '✕' }
const FRASE: Record<Nivel, string> = { BOM: 'Ano bom', ATENCAO: 'Ano de atenção', RUIM: 'Ano ruim' }

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
            <dt>Colheita estimada</dt>
            <dd>{peso(safra.producaoKg)}</dd>
          </div>
          <div className={safra.saldo < 0 ? 'veredito__saldo veredito__saldo--negativo' : 'veredito__saldo'}>
            <dt>{safra.saldo < 0 ? 'Prejuízo estimado' : 'Sobra estimada'}</dt>
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
        <dt>Umidade da terra</dt>
        <dd>{Math.round(clima.umidadeSolo * 100)}% <small>{terraEmPalavras(clima.umidadeSolo)}</small></dd>
      </div>
    </dl>
  )
}

/** Um ano em blocos independentes: resultado, comparação, condições, distribuição e limitações. */
export function DetalheAno({ plantada, alternativa, ano }: Props) {
  const a = plantada.anos.find((r) => r.ano === ano)
  const b = alternativa?.anos.find((r) => r.ano === ano)
  if (!a) return null
  const safraA = plantada.safras.find((s) => s.ano === ano)
  const safraB = alternativa?.safras.find((s) => s.ano === ano)
  const temEpocas = !!a.clima.trimestres && a.clima.trimestres.length > 0

  return (
    <section className="detalhe">
      <div className="detalhe__cabecalho">
        <h2>{ano}</h2>
        <span className={`etiqueta ${a.projetado ? 'etiqueta--previsao' : ''}`}>
          {a.projetado ? 'previsão' : ano === plantada.anoAtual ? 'último ano completo' : 'já aconteceu'}
        </span>
      </div>

      <section className="bloco bloco--resultado">
        <h3 className="bloco__titulo">Como foi para {plantada.cultura.nome.toLowerCase()}</h3>
        <Veredito cultura={plantada.cultura.nome} papel="o que você planta" resultado={a} safra={safraA} />
      </section>

      {alternativa && b && (
        <section className="bloco bloco--comparacao">
          <h3 className="bloco__titulo">Comparação com {alternativa.cultura.nome.toLowerCase()}</h3>
          <Veredito cultura={alternativa.cultura.nome} papel="para comparar" resultado={b} safra={safraB} />
        </section>
      )}

      <section className="bloco bloco--ambiente">
        <h3 className="bloco__titulo">Condições do ano</h3>
        <Clima clima={a.clima} />
      </section>

      {temEpocas && (
        <section className="bloco bloco--ambiente">
          <h3 className="bloco__titulo">Chuva ao longo do ano</h3>
          <EpocasDoAno clima={a.clima} />
        </section>
      )}

      {(a.projetado || safraA || safraB) && (
        <section className="bloco bloco--limite">
          <h3 className="bloco__titulo">Limitações da leitura</h3>
          <ul>
            {a.projetado && <li>Este ano é uma tendência calculada do histórico, não uma previsão do tempo.</li>}
            {(safraA || safraB) && <li>Colheita e dinheiro são uma conta aproximada, com preços de exemplo.</li>}
          </ul>
        </section>
      )}
    </section>
  )
}
