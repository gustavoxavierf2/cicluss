import type { ItemRanking } from '../tipos'

interface Props {
  itens: ItemRanking[]
  aoTestar: (culturaId: string) => void
}

export function Ranking({ itens, aoTestar }: Props) {
  return (
    <section className="ranking">
      <h2>O que mais combina com este terreno</h2>
      <p className="nota-rodape">Considera só clima e solo dos últimos anos, não preço nem mercado.</p>
      <ol>
        {itens.map((i) => (
          <li key={i.cultura.id} className="ranking__item">
            <span className="ranking__pos">{i.posicao}º</span>
            <div className="ranking__info">
              <div className="ranking__topo">
                <strong>{i.cultura.nome}</strong>
                <button type="button" className="link" onClick={() => aoTestar(i.cultura.id)}>
                  comparar
                </button>
              </div>
              <div className="ranking__trilho">
                <span
                  className={`ranking__barra ranking__barra--${i.nivel.toLowerCase()}`}
                  style={{ width: `${Math.max(i.notaMedia, 2)}%` }}
                />
              </div>
              <span className="ranking__motivo">{i.motivo}</span>
            </div>
          </li>
        ))}
      </ol>
    </section>
  )
}
