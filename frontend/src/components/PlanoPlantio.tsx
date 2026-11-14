import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Cultura, Nivel, Planejamento, Ponto, TipoSolo } from '../tipos'
import { hectaresEmTexto, peso, reais } from '../formato'

interface Props {
  ponto: Ponto
  solo: TipoSolo
  hectares: number | null
  culturas: Cultura[]
  culturaId: string
  aoEscolherCultura: (id: string) => void
}

const HORIZONTES = [1, 3, 5] as const
const SIMBOLO: Record<Nivel, string> = { BOM: '✓', ATENCAO: '!', RUIM: '✕' }
const FRASE: Record<Nivel, string> = {
  BOM: 'Previsão boa',
  ATENCAO: 'Previsão de atenção',
  RUIM: 'Previsão ruim',
}

/** Plano de plantio com rotação para os próximos anos, a partir da cultura que o agricultor decidiu plantar. */
export function PlanoPlantio({ ponto, solo, hectares, culturas, culturaId, aoEscolherCultura }: Props) {
  const [anos, setAnos] = useState<(typeof HORIZONTES)[number]>(3)
  const [plano, setPlano] = useState<Planejamento | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    if (!culturaId) return
    let cancelado = false
    setCarregando(true)
    api.planejar(ponto, solo, hectares, culturaId, anos)
      .then((p) => {
        if (cancelado) return
        setPlano(p)
        setErro(null)
      })
      .catch((e: Error) => !cancelado && setErro(e.message))
      .finally(() => !cancelado && setCarregando(false))
    return () => {
      cancelado = true
    }
  }, [ponto, solo, hectares, culturaId, anos])

  return (
    <section className="plano">
      <h2 className="titulo-visao">O que plantar nos próximos anos</h2>

      <div className="plano__escolhas">
        <label className="controle">
          <span className="controle__topo">Quero plantar</span>
          <select value={culturaId} onChange={(e) => aoEscolherCultura(e.target.value)}>
            {culturas.map((c) => (
              <option key={c.id} value={c.id}>{c.nome}</option>
            ))}
          </select>
        </label>

        <div className="controle">
          <span className="controle__topo">Planejar para</span>
          <div className="plano__horizontes" role="group" aria-label="Quantos anos planejar">
            {HORIZONTES.map((h) => (
              <button key={h} type="button" aria-pressed={anos === h}
                className={anos === h ? 'opcao opcao--ativa' : 'opcao'} onClick={() => setAnos(h)}>
                {h} {h === 1 ? 'ano' : 'anos'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {erro && <div className="erro" role="alert"><span>{erro}</span></div>}

      {plano && !erro && (
        <div className={carregando ? 'plano__corpo painel__corpo--carregando' : 'plano__corpo'}>
          <dl className="quadros quadros--tres">
            {plano.saldoTotal !== null && (
              <div className={plano.saldoTotal < 0 ? 'quadros__negativo' : undefined}>
                <dt>{plano.saldoTotal < 0 ? 'Prejuízo no período' : 'Sobra no período'}</dt>
                <dd>{reais(Math.abs(plano.saldoTotal))}</dd>
              </div>
            )}
            <div>
              <dt>Anos com risco</dt>
              <dd>{plano.anosDeRisco} de {plano.anos.length}</dd>
            </div>
            <div>
              <dt>Área</dt>
              <dd>{hectaresEmTexto(plano.hectares)}</dd>
            </div>
          </dl>

          <ol className="plano__anos">
            {plano.anos.map((a) => {
              const nivel = a.resultado.nivel.toLowerCase()
              return (
                <li key={a.ano} className={`veredito veredito--${nivel} veredito--previsao`}>
                  <span className={`veredito__selo veredito__selo--${nivel}`} aria-hidden="true">
                    {SIMBOLO[a.resultado.nivel]}
                  </span>
                  <div className="veredito__texto">
                    <span className="veredito__cultura">
                      {a.ano} <small>{a.principal ? 'sua escolha' : 'troca de planta'}</small>
                    </span>
                    <strong>{a.cultura.nome}</strong>
                    <p>{FRASE[a.resultado.nivel]}</p>
                  </div>
                  {a.safra && (
                    <dl className="veredito__numeros">
                      <div>
                        <dt>Colheita</dt>
                        <dd>{peso(a.safra.producaoKg)}</dd>
                      </div>
                      <div className={a.safra.saldo < 0 ? 'veredito__saldo veredito__saldo--negativo' : 'veredito__saldo'}>
                        <dt>{a.safra.saldo < 0 ? 'Prejuízo' : 'Sobra'}</dt>
                        <dd>{reais(Math.abs(a.safra.saldo))}</dd>
                      </div>
                    </dl>
                  )}
                  {a.resultado.nivel !== 'BOM' && <p className="veredito__linha">⚠️ {a.cuidado}</p>}
                  {a.planoB && (
                    <p className="veredito__linha">
                      🔄 Se o ano começar ruim, troque por <b>{a.planoB.nome.toLowerCase()}</b>.
                    </p>
                  )}
                  <details className="porque veredito__linha">
                    <summary>Entender por quê</summary>
                    <ul>
                      <li>{a.porque}</li>
                      <li>{a.resultado.motivo}</li>
                      {a.resultado.nivel === 'BOM' && <li>{a.cuidado}</li>}
                    </ul>
                  </details>
                </li>
              )
            })}
          </ol>

          <div className="plano__avisos">
            <h3>Fique de olho</h3>
            <ul>
              {plano.avisos.map((aviso) => (
                <li key={aviso}>{aviso}</li>
              ))}
            </ul>
          </div>
          {plano.saldoTotal !== null && (
            <p className="nota-rodape">Colheita e dinheiro são uma conta aproximada, com preços de exemplo.</p>
          )}
        </div>
      )}
    </section>
  )
}
