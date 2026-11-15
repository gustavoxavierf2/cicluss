import type { ClimaAnual } from '../tipos'

/** Chuva e temperatura dos 4 trimestres do ano, destacando a época mais seca. Sem dado por trimestre, não mostra nada. */
export function EpocasDoAno({ clima }: { clima: ClimaAnual }) {
  const trimestres = clima.trimestres
  if (!trimestres || trimestres.length === 0) return null

  const total = trimestres.reduce((soma, t) => soma + t.chuva, 0)
  const maior = Math.max(...trimestres.map((t) => t.chuva), 1)
  const seco = trimestres.reduce((a, b) => (b.chuva < a.chuva ? b : a))
  const chuvoso = trimestres.reduce((a, b) => (b.chuva > a.chuva ? b : a))
  const parte = (chuva: number) => (total > 0 ? Math.round((100 * chuva) / total) : 0)

  return (
    <section className="epocas">
      <div className="epocas__barras">
        {trimestres.map((t) => (
          <div key={t.nome} className={t === seco ? 'epoca epoca--seca' : 'epoca'}>
            <div className="epoca__trilho" aria-hidden="true">
              <span style={{ height: `${Math.max(4, (100 * t.chuva) / maior)}%` }} />
            </div>
            <b>{Math.round(t.chuva)} mm</b>
            <span>{t.nome}</span>
            <span className="epoca__temp">{t.temperaturaMedia.toFixed(1)} °C</span>
          </div>
        ))}
      </div>
      <p className="epocas__frase">
        Mais chuva: <b>{chuvoso.nome}</b> ({parte(chuvoso.chuva)}% do ano). Período mais crítico: <b>{seco.nome}</b>,
        o mais seco, quando a planta mais sofre.{clima.projetado && ' Na previsão, a chuva é repartida como costuma ser.'}
      </p>
    </section>
  )
}
