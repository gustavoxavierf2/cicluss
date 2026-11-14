import { useEffect, useState } from 'react'
import { api } from '../api'
import type { ComparacaoEstrategias, Estrategia, Ponto, TipoSolo } from '../tipos'
import { NOME_NIVEL } from '../tipos'
import { reais } from '../formato'

interface Props {
  ponto: Ponto
  solo: TipoSolo
  hectares: number | null
  culturaPlantadaId: string
  culturaTesteId: string | null
  aoPlanejar: (culturaId: string) => void
}

const SINAL: Record<string, string> = { BOM: '✓', ATENCAO: '!', RUIM: '✕' }

function Cartao({ e, indicada, aoPlanejar }: { e: Estrategia; indicada: boolean; aoPlanejar: (id: string) => void }) {
  const total = e.resumo.anosBons + e.resumo.anosAtencao + e.resumo.anosRuins
  return (
    <li className={indicada ? 'estrategia estrategia--indicada' : 'estrategia'}>
      <div className="estrategia__topo">
        <strong>{e.nome}</strong>
        {e.atual && <span className="etiqueta">o que você faz hoje</span>}
        {indicada && <span className="etiqueta etiqueta--indicada">mais indicada</span>}
      </div>
      <p className="estrategia__descricao">{e.descricao}</p>

      <div className="estrategia__faixa" role="img"
        aria-label={`${e.resumo.anosBons} anos bons, ${e.resumo.anosAtencao} de atenção e ${e.resumo.anosRuins} ruins`}>
        {e.anos.map((a) => (
          <span key={a.ano} className={`estrategia__ano estrategia__ano--${a.nivel.toLowerCase()}`}
            title={`${a.ano}: ${a.cultura}, ${NOME_NIVEL[a.nivel].toLowerCase()} (nota ${a.nota})`}>
            {a.cultura.slice(0, 2)}
          </span>
        ))}
      </div>
      <div className="estrategia__escala">
        <span>{e.anos[0]?.ano}</span>
        <span>{e.anos[e.anos.length - 1]?.ano}</span>
      </div>

      <div className="plano__numeros">
        <div>
          <span>Anos bons</span>
          <b>{e.resumo.anosBons} de {total}</b>
        </div>
        <div>
          <span>Anos ruins</span>
          <b>{e.resumo.anosRuins}</b>
        </div>
        {e.saldoTotal !== null && (
          <div>
            <span>{e.saldoTotal < 0 ? 'Prejuízo estimado' : 'Sobra estimada'}</span>
            <b>{reais(Math.abs(e.saldoTotal))}</b>
          </div>
        )}
        <div>
          <span>Terra descoberta</span>
          <b>{e.mesesDescoberto} {e.mesesDescoberto === 1 ? 'mês' : 'meses'} por ano</b>
        </div>
      </div>

      <ul className="estrategia__solo">
        {e.solo.map((s) => (
          <li key={s.nome} className={`sinal sinal--${s.nivel.toLowerCase()}`}>
            <span className="sinal__marca" aria-hidden="true">{SINAL[s.nivel]}</span>
            <span>
              <b>{s.nome}:</b> {s.texto}
            </span>
          </li>
        ))}
      </ul>

      {!e.atual && (
        <button type="button" className="link" onClick={() => aoPlanejar(e.sequencia[0].id)}>
          planejar os próximos anos com esta rotação
        </button>
      )}
    </li>
  )
}

/** Compara estratégias de rotação (inclui o que o agricultor faz hoje) pelo clima e pela saúde do solo. */
export function Rotacoes({ ponto, solo, hectares, culturaPlantadaId, culturaTesteId, aoPlanejar }: Props) {
  const [comparacao, setComparacao] = useState<ComparacaoEstrategias | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    if (!culturaPlantadaId) return
    let cancelado = false
    setCarregando(true)
    api.estrategias(ponto, solo, hectares, culturaPlantadaId, culturaTesteId)
      .then((c) => {
        if (cancelado) return
        setComparacao(c)
        setErro(null)
      })
      .catch((e: Error) => !cancelado && setErro(e.message))
      .finally(() => !cancelado && setCarregando(false))
    return () => {
      cancelado = true
    }
  }, [ponto, solo, hectares, culturaPlantadaId, culturaTesteId])

  return (
    <section className="rotacoes">
      <h2>Qual rotação de culturas cuida melhor da terra?</h2>
      <p className="nota-rodape">
        Cada opção é testada com o clima que realmente aconteceu neste terreno. Cada quadradinho é um ano: as duas letras são a
        cultura plantada e a cor mostra se o ano foi bom, de atenção ou ruim.
      </p>

      {erro && <div className="erro" role="alert"><span>{erro}</span></div>}
      {!comparacao && !erro && carregando && <p className="nota-rodape">Calculando…</p>}

      {comparacao && !erro && (
        <div className={carregando ? 'plano__corpo painel__corpo--carregando' : 'plano__corpo'}>
          <div className="plano__resumo">
            <p><b>{comparacao.porque}</b></p>
            {comparacao.epocaSeca && <p className="rotacoes__epoca">{comparacao.epocaSeca}</p>}
          </div>

          <ol className="rotacoes__lista">
            {comparacao.estrategias.map((e) => (
              <Cartao key={e.id} e={e} indicada={e.id === comparacao.recomendadaId} aoPlanejar={aoPlanejar} />
            ))}
          </ol>

          <p className="nota-rodape">
            Os sinais de saúde da terra são regras simples (tipo de planta e tempo que ela fica no chão), não análise de
            solo. A sobra em reais usa preços e custos de exemplo.
          </p>
        </div>
      )}
    </section>
  )
}
