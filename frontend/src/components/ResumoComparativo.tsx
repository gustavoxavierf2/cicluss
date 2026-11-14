import type { Resumo, Simulacao } from '../tipos'

function Barra({ resumo }: { resumo: Resumo }) {
  const total = resumo.anosBons + resumo.anosAtencao + resumo.anosRuins || 1
  return (
    <div className="barra" role="img"
      aria-label={`${resumo.anosBons} bons, ${resumo.anosAtencao} de atenção, ${resumo.anosRuins} ruins`}>
      <span className="barra__bom" style={{ flexGrow: resumo.anosBons / total }} />
      <span className="barra__atencao" style={{ flexGrow: resumo.anosAtencao / total }} />
      <span className="barra__ruim" style={{ flexGrow: resumo.anosRuins / total }} />
    </div>
  )
}

function Linha({ s, papel }: { s: Simulacao; papel: string }) {
  const r = s.resumoPassado
  const total = r.anosBons + r.anosAtencao + r.anosRuins
  return (
    <div className="resumo__linha">
      <div className="resumo__nome">
        <strong>{s.cultura.nome}</strong>
        <span>{papel}</span>
      </div>
      <Barra resumo={r} />
      <span className="resumo__texto">
        <b>{r.anosBons}</b> de {total} anos bons
      </span>
    </div>
  )
}

export function ResumoComparativo({ plantada, alternativa }: { plantada: Simulacao; alternativa: Simulacao | null }) {
  const total = plantada.resumoPassado.anosBons + plantada.resumoPassado.anosAtencao + plantada.resumoPassado.anosRuins
  return (
    <section className="resumo">
      <h2>Nos últimos {total} anos, neste terreno</h2>
      <Linha s={plantada} papel="o que você plantou" />
      {alternativa && <Linha s={alternativa} papel="e se?" />}
    </section>
  )
}
