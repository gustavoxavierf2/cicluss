import type { Safra } from '../tipos'
import { peso, reais } from '../formato'

/** Estimativa de colheita e de dinheiro de uma cultura em um ano. */
export function SafraEstimada({ safra }: { safra: Safra | null | undefined }) {
  if (!safra) return null
  return (
    <dl className="safra">
      <div>
        <dt>Safra estimada</dt>
        <dd>{peso(safra.producaoKg)}</dd>
      </div>
      <div>
        <dt>Venda</dt>
        <dd>{reais(safra.receita)}</dd>
      </div>
      <div>
        <dt>Custo</dt>
        <dd>{reais(safra.custo)}</dd>
      </div>
      <div className={safra.saldo < 0 ? 'safra__saldo safra__saldo--negativo' : 'safra__saldo'}>
        <dt>{safra.saldo < 0 ? 'Prejuízo' : 'Sobra'}</dt>
        <dd>{reais(Math.abs(safra.saldo))}</dd>
      </div>
    </dl>
  )
}

export function AvisoEstimativa() {
  return (
    <p className="nota-rodape">
      Safra e valores são uma estimativa simples, feita com preços e custos de exemplo. Servem para ter uma ideia,
      não são garantia.
    </p>
  )
}
