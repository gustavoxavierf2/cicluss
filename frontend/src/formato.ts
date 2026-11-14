const inteiro = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 })

/** 12500 vira "R$ 12.500"; negativo vira "-R$ 1.200". */
export function reais(valor: number) {
  return `${valor < 0 ? '-' : ''}R$ ${inteiro.format(Math.abs(valor))}`
}

/** 5000 kg vira "5.000 kg"; a partir de 10 mil kg mostra em toneladas. */
export function peso(kg: number) {
  if (kg >= 10000) {
    return `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(kg / 1000)} toneladas`
  }
  return `${inteiro.format(kg)} kg`
}

export function hectaresEmTexto(ha: number) {
  const n = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(ha)
  return `${n} ${ha === 1 ? 'hectare' : 'hectares'}`
}
