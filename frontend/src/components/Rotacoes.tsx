import { useEffect, useState } from 'react'
import { api } from '../api'
import type { ComparacaoEstrategias, Estrategia, Nivel, Ponto, TipoSolo } from '../tipos'
import { NOME_NIVEL } from '../tipos'
import { reais } from '../formato'

interface Props {
  ponto: Ponto
  solo: TipoSolo
  hectares: number | null
  culturaPlantadaId: string
  culturaTesteId: string | null
  /** Cultura que o plano logo abaixo está usando. */
  culturaDoPlanoId: string
  aoPlanejar: (culturaId: string) => void
}

const SIMBOLO: Record<Nivel, string> = { BOM: '✓', ATENCAO: '!', RUIM: '✕' }

/** Nomes do dia a dia para os sinais de saúde da terra que vêm do servidor. */
const NOME_DO_SINAL: Record<string, string> = {
  'Nitrogênio': 'Adubo natural',
  'Pragas e doenças': 'Menos praga',
  'Terra coberta': 'Terra coberta',
  'Palha e matéria orgânica': 'Palha na terra',
}

function Cartao({ e, indicada, aoPlanejar }: { e: Estrategia; indicada: boolean; aoPlanejar: (id: string) => void }) {
  const total = e.resumo.anosBons + e.resumo.anosAtencao + e.resumo.anosRuins
  return (
    <li className={indicada ? 'estrategia estrategia--indicada' : 'estrategia'}>
      <div className="estrategia__topo">
        <strong>{e.nome}</strong>
        {e.atual && <span className="etiqueta">o que você faz hoje</span>}
        {indicada && <span className="etiqueta etiqueta--indicada">recomendada</span>}
      </div>

      <div className="estrategia__faixa" role="img"
        aria-label={`${e.resumo.anosBons} anos bons, ${e.resumo.anosAtencao} de atenção e ${e.resumo.anosRuins} ruins`}>
        {e.anos.map((a) => (
          <span key={a.ano} className={`estrategia__ano estrategia__ano--${a.nivel.toLowerCase()}`}
            title={`${a.ano}: ${a.cultura}, ${NOME_NIVEL[a.nivel].toLowerCase()}`}>
            {a.cultura.slice(0, 2)}
          </span>
        ))}
      </div>
      <div className="estrategia__escala">
        <span>{e.anos[0]?.ano}</span>
        <span>{e.anos[e.anos.length - 1]?.ano}</span>
      </div>

      <dl className="quadros">
        <div>
          <dt>Anos bons</dt>
          <dd>{e.resumo.anosBons} de {total}</dd>
        </div>
        {e.saldoTotal !== null && (
          <div className={e.saldoTotal < 0 ? 'quadros__negativo' : undefined}>
            <dt>{e.saldoTotal < 0 ? 'Prejuízo' : 'Sobra'}</dt>
            <dd>{reais(Math.abs(e.saldoTotal))}</dd>
          </div>
        )}
      </dl>

      <ul className="sinais" aria-label="Saúde da terra">
        {e.solo.map((s) => (
          <li key={s.nome} className={`sinal-curto sinal-curto--${s.nivel.toLowerCase()}`}>
            <span className="sinal__marca" aria-hidden="true">{SIMBOLO[s.nivel]}</span>
            {NOME_DO_SINAL[s.nome] ?? s.nome}
          </li>
        ))}
      </ul>

      <details className="porque">
        <summary>Entender por quê</summary>
        <ul>
          <li>{e.descricao}</li>
          {e.solo.map((s) => (
            <li key={s.nome}>{s.texto}</li>
          ))}
        </ul>
      </details>

      {!e.atual && (
        <button type="button" className="botao-claro" onClick={() => aoPlanejar(e.sequencia[0].id)}>
          Planejar esta rotação
        </button>
      )}
    </li>
  )
}

const anosBons = (e: Estrategia) => `${e.resumo.anosBons} de ${e.resumo.anosBons + e.resumo.anosAtencao + e.resumo.anosRuins} anos bons`

/**
 * Primeira metade de "Rotação e planejamento": a rotação recomendada (o quê, por quê, cuidado) e a ação de
 * planejar. A comparação com o que o agricultor faz hoje e os sinais da terra ficam guardados em detalhes.
 */
export function Rotacoes({ ponto, solo, hectares, culturaPlantadaId, culturaTesteId, culturaDoPlanoId, aoPlanejar }: Props) {
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

  const indicada = comparacao?.estrategias.find((e) => e.id === comparacao.recomendadaId)
  const atual = comparacao?.estrategias.find((e) => e.atual)
  const jaNoPlano = indicada?.sequencia[0]?.id === culturaDoPlanoId

  return (
    <section className="rotacoes">
      <h2 className="titulo-visao">Rotação recomendada para este terreno</h2>

      {erro && <div className="erro" role="alert"><span>{erro}</span></div>}
      {!comparacao && !erro && carregando && <p className="nota-rodape">Calculando…</p>}

      {comparacao && indicada && !erro && (
        <div className={carregando ? 'plano__corpo painel__corpo--carregando' : 'plano__corpo'}>
          <div className="recomendacao">
            <span className="destaque__rotulo">O que plantar, ano após ano</span>
            <ol className="recomendacao__sequencia"
              aria-label={`Sequência: ${indicada.sequencia.map((c) => c.nome).join(', depois ')}`}>
              {indicada.sequencia.map((c, i) => (
                <li key={`${c.id}-${i}`}>{c.nome}</li>
              ))}
            </ol>
            <p><b>Por quê?</b> {comparacao.porque}</p>
            {comparacao.epocaSeca && <p><b>Cuidado:</b> {comparacao.epocaSeca}</p>}
            {atual && !indicada.atual && (
              <p className="recomendacao__compara">
                Hoje você faz <b>{atual.nome.toLowerCase()}</b>: {anosBons(atual)}. Com a recomendada:{' '}
                <b>{anosBons(indicada)}</b>.
              </p>
            )}
            {indicada.atual && <p className="recomendacao__compara">É o que você já faz hoje: {anosBons(indicada)}.</p>}
            <button type="button" className="botao botao--imprimir" disabled={jaNoPlano}
              onClick={() => aoPlanejar(indicada.sequencia[0].id)}>
              {jaNoPlano ? '✓ O plano abaixo já começa por esta rotação' : 'Planejar esta rotação'}
            </button>
          </div>

          <details className="porque rotacoes__comparar">
            <summary>Comparar com o que você faz hoje e com outras opções</summary>
            <p className="nota-rodape">
              Cada quadradinho é um ano que já passou. As letras são a planta daquele ano e a cor mostra como foi.
            </p>
            <ol className="rotacoes__lista">
              {comparacao.estrategias.map((e) => (
                <Cartao key={e.id} e={e} indicada={e.id === comparacao.recomendadaId} aoPlanejar={aoPlanejar} />
              ))}
            </ol>
            <p className="nota-rodape">
              Os sinais da terra são regras simples, não exame de solo. O dinheiro usa preços de exemplo.
            </p>
          </details>
        </div>
      )}
    </section>
  )
}
