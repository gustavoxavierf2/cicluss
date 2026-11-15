import { useEffect, useRef, useState } from 'react'
import type { CSSProperties, KeyboardEvent, PointerEvent } from 'react'
import { api } from '../api'
import type { AnoPlano, Cultura, Horizonte, Janela, Nivel, Planejamento, Ponto, TipoSolo } from '../tipos'
import { HORIZONTES, NOME_FASE, acaoDaJanela, iconeDaJanela } from '../tipos'
import { hectaresEmTexto, reais } from '../formato'

interface Props {
  ponto: Ponto
  solo: TipoSolo
  hectares: number | null
  culturas: Cultura[]
  culturaId: string
  aoEscolherCultura: (id: string) => void
  horizonte: Horizonte
  aoMudarHorizonte: (h: Horizonte) => void
  aoImprimir: (plano: Planejamento) => void
  imprimindo: boolean
}

const FRASE: Record<Nivel, string> = {
  BOM: 'Previsão boa',
  ATENCAO: 'Previsão de atenção',
  RUIM: 'Previsão ruim',
}

/** A época que carrega a previsão e o dinheiro do ano: a da safra principal. */
function janelaDaSafra(janelas: Janela[], ano: AnoPlano) {
  const doAno = janelas.filter((j) => j.ano === ano.ano)
  return doAno.find((j) => j.fase === 'SAFRA' && j.culturaId === ano.cultura.id)
    ?? doAno.find((j) => j.culturaId === ano.cultura.id)
    ?? doAno[0]
}

/** Simulador de rotação: linha do tempo por época do ano para os próximos 1, 3 ou 5 anos. */
export function PlanoPlantio(props: Props) {
  const { ponto, solo, hectares, culturas, culturaId, aoEscolherCultura, horizonte, aoMudarHorizonte, aoImprimir, imprimindo } = props
  const [plano, setPlano] = useState<Planejamento | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [posicao, setPosicao] = useState(0)
  const trilho = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!culturaId) return
    let cancelado = false
    setCarregando(true)
    api.planejar(ponto, solo, hectares, culturaId, horizonte)
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
  }, [ponto, solo, hectares, culturaId, horizonte])

  const janelas = plano?.janelas ?? []
  const indice = Math.min(posicao, Math.max(0, janelas.length - 1))
  const atual = janelas[indice]

  function arrastar(e: PointerEvent<HTMLDivElement>) {
    if (e.type === 'pointermove' && e.buttons === 0) return
    const caixa = trilho.current?.getBoundingClientRect()
    if (!caixa || janelas.length === 0) return
    const parte = (e.clientX - caixa.left) / caixa.width
    setPosicao(Math.max(0, Math.min(janelas.length - 1, Math.floor(parte * janelas.length))))
  }

  function teclar(e: KeyboardEvent) {
    const passo = { ArrowRight: 1, ArrowUp: 1, ArrowLeft: -1, ArrowDown: -1, Home: -janelas.length, End: janelas.length }[e.key]
    if (!passo) return
    e.preventDefault()
    setPosicao(Math.max(0, Math.min(janelas.length - 1, indice + passo)))
  }

  return (
    <section className="plano">
      <div className="plano__topo">
        <h2 className="titulo-visao">Simulador de rotação</h2>
        {atual && (
          <dl className="plano__clima" aria-live="polite">
            <div>
              <dt>🌡️ Temp. média</dt>
              <dd>{atual.temperaturaMedia.toFixed(1).replace('.', ',')} °C</dd>
            </div>
            <div>
              <dt>💧 Umidade do solo</dt>
              <dd>{Math.round(atual.umidadeSolo * 100)}%</dd>
            </div>
          </dl>
        )}
      </div>

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
          <span className="controle__topo">Simular</span>
          <div className="plano__horizontes" role="group" aria-label="Quantos anos simular">
            {HORIZONTES.map((h) => (
              <button key={h} type="button" aria-pressed={horizonte === h}
                className={horizonte === h ? 'opcao opcao--ativa' : 'opcao'} onClick={() => aoMudarHorizonte(h)}>
                {h} {h === 1 ? 'ano' : 'anos'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {erro && <div className="erro" role="alert"><span>{erro}</span></div>}

      {plano && atual && !erro && (
        <div className={carregando ? 'plano__corpo painel__corpo--carregando' : 'plano__corpo'}>
          <div className="linha-tempo">
            <div className={`linha-tempo__atual fase--${atual.fase.toLowerCase()}`}>
              <span className="linha-tempo__icone" aria-hidden="true">{iconeDaJanela(atual)}</span>
              <div>
                <span className="linha-tempo__periodo">
                  {atual.inicio} – {atual.fim} <small>{NOME_FASE[atual.fase]}</small>
                </span>
                <strong>{acaoDaJanela(janelas, indice)}</strong>
              </div>
            </div>

            <div className="linha-tempo__trilho" ref={trilho} role="slider" tabIndex={0}
              aria-label="Época do ano" aria-valuemin={1} aria-valuemax={janelas.length} aria-valuenow={indice + 1}
              aria-valuetext={`${atual.inicio} a ${atual.fim}: ${acaoDaJanela(janelas, indice)}`}
              style={{ '--epocas': janelas.length, '--pos': indice } as CSSProperties}
              onPointerDown={(e) => {
                e.currentTarget.setPointerCapture(e.pointerId)
                arrastar(e)
              }}
              onPointerMove={arrastar} onKeyDown={teclar}>
              {janelas.map((j, i) => (
                <span key={j.inicio}
                  className={`linha-tempo__epoca fase--${j.fase.toLowerCase()} ${i === indice ? 'linha-tempo__epoca--atual' : ''} ${i % 3 === 0 ? 'linha-tempo__epoca--janeiro' : ''}`}>
                  {iconeDaJanela(j)}
                </span>
              ))}
              <span className="linha-tempo__cursor" aria-hidden="true" />
            </div>
            <div className="linha-tempo__anos" aria-hidden="true">
              {plano.anos.map((a) => (
                <span key={a.ano}>{a.ano}</span>
              ))}
            </div>
            <div className="linha-tempo__legenda">
              <span className="fase--safra">Safra</span>
              <span className="fase--safrinha">Safrinha</span>
              <span className="fase--entressafra">Entressafra</span>
              <small>Arraste para andar no tempo</small>
            </div>
          </div>

          <ol className="epocas-plano">
            {janelas.map((j, i) => {
              const ano = plano.anos.find((a) => a.ano === j.ano)
              const daSafra = ano && janelaDaSafra(janelas, ano) === j
              return (
                <li key={j.inicio}>
                  <button type="button" aria-current={i === indice ? 'step' : undefined}
                    className={`epoca-plano fase--${j.fase.toLowerCase()} ${i === indice ? 'epoca-plano--atual' : ''}`}
                    onClick={() => setPosicao(i)}>
                    <span className="epoca-plano__icone" aria-hidden="true">{iconeDaJanela(j)}</span>
                    <span className="epoca-plano__texto">
                      <span className="epoca-plano__periodo">{j.inicio} a {j.fim} · {NOME_FASE[j.fase]}</span>
                      <strong>{acaoDaJanela(janelas, i)}</strong>
                      <span><b>Por quê?</b> {j.porque}</span>
                      {daSafra && ano && (
                        <span className={`epoca-plano__previsao cor-${ano.resultado.nivel.toLowerCase()}`}>
                          {FRASE[ano.resultado.nivel]} para {ano.ano}
                          {ano.safra && ` · ${ano.safra.saldo < 0 ? 'prejuízo' : 'sobra'} de ${reais(Math.abs(ano.safra.saldo))}`}
                          {ano.resultado.nivel !== 'BOM' && `. ${ano.cuidado}`}
                          {ano.planoB && ` Se o ano começar ruim, troque por ${ano.planoB.nome.toLowerCase()}.`}
                        </span>
                      )}
                    </span>
                  </button>
                </li>
              )
            })}
          </ol>

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

          <div className="plano__avisos">
            <h3>Fique de olho</h3>
            <ul>
              {plano.avisos.map((aviso) => (
                <li key={aviso}>{aviso}</li>
              ))}
            </ul>
          </div>

          <button type="button" className="botao botao--imprimir" disabled={imprimindo || carregando}
            onClick={() => aoImprimir(plano)}>
            {imprimindo ? 'Preparando o relatório…' : '🖨️ Imprimir Relatório de Rotação de Culturas'}
          </button>

          <p className="nota-rodape">
            {atual.mediaDoAno && 'Clima de demonstração: temperatura e umidade são a média do ano, não da época. '}
            {plano.saldoTotal !== null && 'O dinheiro é uma conta aproximada da safra principal, com preços de exemplo. '}
            É uma tendência do clima, não uma previsão do tempo.
          </p>
        </div>
      )}
    </section>
  )
}
