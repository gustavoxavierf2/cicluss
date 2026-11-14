import type { ItemRanking, Risco } from '../tipos'

interface Props {
  itens: ItemRanking[]
  aoTestar: (culturaId: string) => void
  aoPlanejar: (culturaId: string) => void
}

const CUIDADO: Record<Risco, string> = {
  NENHUM: 'Sem problema que se repete',
  SECA: 'Cuidado com a seca',
  CHUVA_EXCESSIVA: 'Cuidado com chuva demais',
  CALOR: 'Cuidado com o calor',
  FRIO: 'Cuidado com o frio',
  SOLO_INADEQUADO: 'A terra não é a mais indicada',
}

/** Lista das culturas, da que mais combina com o terreno para a que menos combina. */
export function Ranking({ itens, aoTestar, aoPlanejar }: Props) {
  return (
    <section className="ranking">
      <h2 className="titulo-visao">O que mais combina com a sua terra</h2>
      <p className="nota-rodape">Pelo clima dos últimos anos e pelo tipo de terra. Não olha preço de venda.</p>
      <ol className="ranking__lista">
        {itens.map((i) => {
          const nivel = i.nivel.toLowerCase()
          return (
            <li key={i.cultura.id} className={`cartao-rank cartao-rank--${nivel}`}>
              <span className={`veredito__selo veredito__selo--${nivel}`} aria-hidden="true">{i.posicao}º</span>
              <div className="cartao-rank__texto">
                <strong>{i.cultura.nome}</strong>
                <span className="cartao-rank__anos">
                  <b>{i.anosBons}</b> de {i.totalAnos} anos bons
                </span>
                <div className="ranking__trilho" aria-hidden="true">
                  <span className={`ranking__barra ranking__barra--${nivel}`}
                    style={{ width: `${Math.max((100 * i.anosBons) / Math.max(i.totalAnos, 1), 3)}%` }} />
                </div>
                <span className="cartao-rank__cuidado">{CUIDADO[i.principalRisco]}</span>
              </div>
              <div className="cartao-rank__botoes">
                <button type="button" className="botao-claro" onClick={() => aoTestar(i.cultura.id)}>
                  Comparar
                </button>
                <button type="button" className="botao-claro" onClick={() => aoPlanejar(i.cultura.id)}>
                  Planejar
                </button>
              </div>
            </li>
          )
        })}
      </ol>
    </section>
  )
}
