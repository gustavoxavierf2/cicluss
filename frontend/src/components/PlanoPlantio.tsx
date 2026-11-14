import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Cultura, Planejamento, Ponto, TipoSolo } from '../tipos'
import { NOME_NIVEL } from '../tipos'
import { hectaresEmTexto, reais } from '../formato'
import { AvisoEstimativa, SafraEstimada } from './SafraEstimada'

interface Props {
  ponto: Ponto
  solo: TipoSolo
  hectares: number | null
  culturas: Cultura[]
  culturaId: string
  aoEscolherCultura: (id: string) => void
}

const HORIZONTES = [1, 3, 5] as const

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
      <h2>Planejamento dos próximos anos</h2>
      <p className="nota-rodape">
        Escolha a cultura que você decidiu plantar e por quanto tempo quer planejar. O plano troca a cultura de um ano
        para o outro (rotação), o que ajuda a terra e faz a renda não depender de uma coisa só.
      </p>

      <div className="plano__escolhas">
        <label className="controle">
          <span className="controle__topo">Cultura que você decidiu plantar</span>
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
          <div className="plano__resumo">
            <p>{plano.resumo}</p>
            <div className="plano__numeros">
              <div>
                <span>Área</span>
                <b>{hectaresEmTexto(plano.hectares)}</b>
              </div>
              {plano.saldoTotal !== null && (
                <div>
                  <span>{plano.saldoTotal < 0 ? 'Prejuízo estimado' : 'Sobra estimada'}</span>
                  <b>{reais(Math.abs(plano.saldoTotal))}</b>
                </div>
              )}
              <div>
                <span>Anos com risco</span>
                <b>{plano.anosDeRisco} de {plano.anos.length}</b>
              </div>
            </div>
          </div>

          <ol className="plano__anos">
            {plano.anos.map((a) => (
              <li key={a.ano} className={`resultado resultado--${a.resultado.nivel.toLowerCase()} resultado--previsao`}>
                <div className="plano__ano-topo">
                  <span className="plano__ano">{a.ano}</span>
                  <strong>{a.cultura.nome}</strong>
                  <span className="etiqueta">{a.principal ? 'sua escolha' : 'rotação'}</span>
                  <span className="plano__nivel">
                    previsão: {NOME_NIVEL[a.resultado.nivel].toLowerCase()} (nota {a.resultado.nota})
                  </span>
                </div>
                <p>{a.porque}</p>
                <p>{a.resultado.motivo}</p>
                <SafraEstimada safra={a.safra} />
                <p className="plano__cuidado">{a.cuidado}</p>
                {a.planoB && (
                  <p className="plano__cuidado">
                    Plano B: se o ano começar ruim, <b>{a.planoB.nome.toLowerCase()}</b> aguenta melhor esse clima.
                  </p>
                )}
              </li>
            ))}
          </ol>

          <div className="plano__avisos">
            <h3>Para não ficar na mão</h3>
            <ul>
              {plano.avisos.map((aviso) => (
                <li key={aviso}>{aviso}</li>
              ))}
            </ul>
          </div>
          {plano.saldoTotal !== null && <AvisoEstimativa />}
        </div>
      )}
    </section>
  )
}
