import type { Cultura, TipoSolo } from '../tipos'
import { NOME_SOLO } from '../tipos'

export interface Respostas {
  solo: TipoSolo
  culturaPlantadaId: string
  culturaTesteId: string
  /** Texto do campo; vazio quando o agricultor não informou a área. */
  hectares: string
}

interface Props {
  culturas: Cultura[]
  respostas: Respostas
  aoMudar: (r: Respostas) => void
  podeCalcular: boolean
  carregando: boolean
  aoCalcular: () => void
}

const SOLOS: TipoSolo[] = ['NAO_SEI', 'ARENOSO', 'ARGILOSO', 'MISTO']

/** Linha de seletores numerados no topo (no estilo do AgroClimate). */
export function Seletores({ culturas, respostas, aoMudar, podeCalcular, carregando, aoCalcular }: Props) {
  return (
    <div className="seletores">
      <label className="seletor">
        <span className="seletor__titulo">1. O que você planta?</span>
        <select
          value={respostas.culturaPlantadaId}
          onChange={(e) => {
            const id = e.target.value
            aoMudar({
              ...respostas,
              culturaPlantadaId: id,
              culturaTesteId: respostas.culturaTesteId === id ? '' : respostas.culturaTesteId,
            })
          }}
        >
          {culturas.map((c) => (
            <option key={c.id} value={c.id}>{c.nome}</option>
          ))}
        </select>
      </label>

      <label className="seletor">
        <span className="seletor__titulo">2. Comparar com</span>
        <select value={respostas.culturaTesteId} onChange={(e) => aoMudar({ ...respostas, culturaTesteId: e.target.value })}>
          <option value="">Não comparar</option>
          {culturas
            .filter((c) => c.id !== respostas.culturaPlantadaId)
            .map((c) => (
              <option key={c.id} value={c.id}>{c.nome}</option>
            ))}
        </select>
      </label>

      <label className="seletor">
        <span className="seletor__titulo">3. Como é a terra?</span>
        <select value={respostas.solo} onChange={(e) => aoMudar({ ...respostas, solo: e.target.value as TipoSolo })}>
          {SOLOS.map((s) => (
            <option key={s} value={s}>{NOME_SOLO[s]}</option>
          ))}
        </select>
      </label>

      <label className="seletor seletor--curto">
        <span className="seletor__titulo">4. Hectares</span>
        <input
          type="number"
          inputMode="decimal"
          min="0.1"
          step="0.5"
          placeholder="ex.: 2"
          value={respostas.hectares}
          onChange={(e) => aoMudar({ ...respostas, hectares: e.target.value })}
        />
      </label>

      <div className="seletor seletor--acao">
        <button type="button" className="botao" disabled={!podeCalcular || carregando} onClick={aoCalcular}>
          {carregando ? 'Calculando…' : 'Ver resultado'}
        </button>
      </div>
    </div>
  )
}
