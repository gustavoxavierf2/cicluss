import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Cenario, ClimaAnual, Ponto, Simulacao, TipoSolo } from '../tipos'
import { NOME_NIVEL } from '../tipos'
import { AvisoEstimativa, SafraEstimada } from './SafraEstimada'

interface Props {
  ponto: Ponto
  solo: TipoSolo
  hectares: number | null
  plantada: Simulacao
  alternativa: Simulacao | null
  ano: number
}

type Terra = 'seca' | 'normal' | 'umida'

const UMIDADE: Record<Terra, number> = { seca: 0.15, normal: 0.32, umida: 0.5 }

function terraDe(umidade: number): Terra {
  if (umidade < 0.25) return 'seca'
  if (umidade < 0.4) return 'normal'
  return 'umida'
}

function Cartao({ titulo, cenario }: { titulo: string; cenario: Cenario | null }) {
  if (!cenario) return null
  const r = cenario.resultado
  return (
    <div className={`resultado resultado--${r.nivel.toLowerCase()}`}>
      <span className="resultado__titulo">{titulo}</span>
      <div className="resultado__nota">
        <strong>{NOME_NIVEL[r.nivel]}</strong>
        <span>nota {r.nota}</span>
      </div>
      <p>{r.motivo}</p>
      <SafraEstimada safra={cenario.safra} />
    </div>
  )
}

/** "E se o clima fosse diferente?": o agricultor mexe na temperatura, na chuva e na terra e vê o resultado. */
export function CenarioClima({ ponto, solo, hectares, plantada, alternativa, ano }: Props) {
  const base: ClimaAnual | undefined = plantada.anos.find((a) => a.ano === ano)?.clima
  const [aberto, setAberto] = useState(false)
  const [temperatura, setTemperatura] = useState(0)
  const [chuva, setChuva] = useState(0)
  const [terra, setTerra] = useState<Terra>('normal')
  const [cenarios, setCenarios] = useState<{ a: Cenario; b: Cenario | null } | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  // Ao trocar de ano (ou de terreno), os controles voltam para o clima daquele ano.
  useEffect(() => {
    if (!base) return
    setTemperatura(base.temperaturaMedia)
    setChuva(Math.round(base.precipitacao))
    setTerra(terraDe(base.umidadeSolo))
  }, [base])

  const plantadaId = plantada.cultura.id
  const alternativaId = alternativa?.cultura.id ?? null

  useEffect(() => {
    if (!aberto || !base) return
    let cancelado = false
    const espera = setTimeout(() => {
      const pedir = (culturaId: string) =>
        // A terra só entra no pedido quando o agricultor trocou; senão vale a umidade medida no ano.
        api.cenario(ponto, solo, culturaId, hectares, ano, temperatura, chuva,
          terra === terraDe(base.umidadeSolo) ? null : UMIDADE[terra])
      Promise.all([pedir(plantadaId), alternativaId ? pedir(alternativaId) : Promise.resolve(null)])
        .then(([a, b]) => {
          if (cancelado) return
          setCenarios({ a, b })
          setErro(null)
        })
        .catch((e: Error) => !cancelado && setErro(e.message))
    }, 250)
    return () => {
      cancelado = true
      clearTimeout(espera)
    }
  }, [aberto, base, ponto, solo, hectares, ano, temperatura, chuva, terra, plantadaId, alternativaId])

  if (!base) return null

  const mudou =
    temperatura !== base.temperaturaMedia || chuva !== Math.round(base.precipitacao) || terra !== terraDe(base.umidadeSolo)

  function voltar() {
    if (!base) return
    setTemperatura(base.temperaturaMedia)
    setChuva(Math.round(base.precipitacao))
    setTerra(terraDe(base.umidadeSolo))
  }

  return (
    <section className="cenario">
      <button type="button" className="cenario__abrir" aria-expanded={aberto} onClick={() => setAberto(!aberto)}>
        <span>E se o clima fosse diferente?</span>
        <span aria-hidden="true">{aberto ? '−' : '+'}</span>
      </button>

      {aberto && (
        <div className="cenario__corpo">
          <p className="nota-rodape">
            Mexa nos controles para testar um ano mais quente, mais seco ou mais chuvoso. Começa com o clima de {ano}.
          </p>

          <div className="cenario__controles">
            <label className="controle">
              <span className="controle__topo">
                Temperatura média <b>{temperatura.toFixed(1)} °C</b>
              </span>
              <input type="range" min={10} max={35} step={0.5} value={temperatura}
                onChange={(e) => setTemperatura(Number(e.target.value))} />
              <span className="controle__escala"><span>10 °C</span><span>35 °C</span></span>
            </label>

            <label className="controle">
              <span className="controle__topo">
                Chuva no ano <b>{chuva} mm</b>
              </span>
              <input type="range" min={0} max={2500} step={10} value={chuva}
                onChange={(e) => setChuva(Number(e.target.value))} />
              <span className="controle__escala"><span>0 mm</span><span>2.500 mm</span></span>
            </label>

            <label className="controle">
              <span className="controle__topo">Terra</span>
              <select value={terra} onChange={(e) => setTerra(e.target.value as Terra)}>
                <option value="seca">Seca</option>
                <option value="normal">Normal</option>
                <option value="umida">Úmida</option>
              </select>
            </label>
          </div>

          <button type="button" className="link" onClick={voltar} disabled={!mudou}>
            voltar para o clima de {ano}
          </button>

          {erro && <div className="erro" role="alert"><span>{erro}</span></div>}

          {cenarios && (
            <>
              <div className="detalhe__colunas cenario__resultado" aria-live="polite">
                <Cartao titulo={`${plantada.cultura.nome} com esse clima`} cenario={cenarios.a} />
                {alternativa && <Cartao titulo={`${alternativa.cultura.nome} com esse clima`} cenario={cenarios.b} />}
              </div>
              {cenarios.a.safra && <AvisoEstimativa />}
            </>
          )}
        </div>
      )}
    </section>
  )
}
