import type { ItemRanking, Nivel, Risco } from '../tipos'

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

const COMBINA: Record<Nivel, string> = {
  BOM: 'Combina bem com o clima e a terra',
  ATENCAO: 'Combina em parte',
  RUIM: 'Combina pouco',
}

/** Lista das culturas, da que mais combina com o terreno para a que menos combina. */
export function Ranking({ itens, aoTestar, aoPlanejar }: Props) {
  return (
    <section className="ranking">
      <h2 className="titulo-visao">Ranking: o que mais combina com a sua terra</h2>
      <p className="ranking__regra">
        Ordem pela <b>aptidão</b>: como cada planta foi com o clima dos últimos anos e o tipo de terra.{' '}
        <b>Não é ordem de preço nem de lucro.</b>
      </p>
      <ol className="ranking__lista">
        {itens.map((i) => {
          const nivel = i.nivel.toLowerCase()
          return (
            <li key={i.cultura.id}
              className={`cartao-rank cartao-rank--${nivel} ${i.posicao === 1 ? 'cartao-rank--primeiro' : ''}`}>
              <span className="cartao-rank__posicao" aria-label={`${i.posicao}º lugar`}>{i.posicao}º</span>
              <div className="cartao-rank__texto">
                <strong>{i.cultura.nome}</strong>
                <span className={`cartao-rank__combina cor-${nivel}`}>{COMBINA[i.nivel]}</span>
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
                <button type="button" className="botao-claro" onClick={() => aoTestar(i.cultura.id)}
                  aria-label={`Comparar ${i.cultura.nome} com o que você planta`}>
                  Comparar
                </button>
                <button type="button" className="botao-claro" onClick={() => aoPlanejar(i.cultura.id)}
                  aria-label={`Planejar com ${i.cultura.nome}`}>
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
