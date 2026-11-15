import type { Risco, Simulacao } from './tipos'

/** Intervalo inclusivo de anos escolhido na barra do tempo. */
export interface IntervaloAnos {
  inicio: number
  fim: number
}

export interface ValorDoAno {
  ano: number
  valor: number
}

/** Como uma cultura foi nos anos do intervalo. */
export interface ResumoCultura {
  nome: string
  bons: number
  atencao: number
  ruins: number
  notaMedia: number
  /** Risco que mais se repetiu (fora "nenhum") e em quantos anos; null quando não houve risco. */
  principalRisco: { risco: Risco; anos: number } | null
  /** Somas do período; null quando a simulação não tem safra (sem hectares ou sem valores da cultura). */
  producaoKg: number | null
  saldo: number | null
}

export interface AgregadoPeriodo {
  inicio: number
  fim: number
  anos: number
  /** Quantos anos do intervalo são tendência, não clima observado. */
  projetados: number
  /** true quando o intervalo junta anos observados e anos de tendência. */
  misturaProjecao: boolean
  chuvaMedia: number | null
  chuvaMinima: ValorDoAno | null
  chuvaMaxima: ValorDoAno | null
  temperaturaMedia: number | null
  /** Umidade da zona de raiz, de 0 a 1. */
  umidadeMedia: number | null
  plantada: ResumoCultura
  alternativa: ResumoCultura | null
}

/** Põe o intervalo em ordem (início ≤ fim) e dentro dos anos disponíveis. */
export function normalizarIntervalo(i: IntervaloAnos, primeiro: number, ultimo: number): IntervaloAnos {
  const limitar = (ano: number) => Math.min(ultimo, Math.max(primeiro, Math.round(ano)))
  const a = limitar(i.inicio)
  const b = limitar(i.fim)
  return { inicio: Math.min(a, b), fim: Math.max(a, b) }
}

/** Média só dos valores que existem; null quando não há nenhum. */
function media(valores: number[]): number | null {
  const validos = valores.filter((v) => Number.isFinite(v))
  return validos.length === 0 ? null : validos.reduce((soma, v) => soma + v, 0) / validos.length
}

function resumir(s: Simulacao, i: IntervaloAnos): ResumoCultura {
  const anos = s.anos.filter((a) => a.ano >= i.inicio && a.ano <= i.fim)
  const safras = s.safras.filter((x) => x.ano >= i.inicio && x.ano <= i.fim)

  const vezes = new Map<Risco, number>()
  for (const a of anos) {
    if (a.risco !== 'NENHUM') vezes.set(a.risco, (vezes.get(a.risco) ?? 0) + 1)
  }
  let principalRisco: ResumoCultura['principalRisco'] = null
  for (const [risco, n] of vezes) {
    if (!principalRisco || n > principalRisco.anos) principalRisco = { risco, anos: n }
  }

  return {
    nome: s.cultura.nome,
    bons: anos.filter((a) => a.nivel === 'BOM').length,
    atencao: anos.filter((a) => a.nivel === 'ATENCAO').length,
    ruins: anos.filter((a) => a.nivel === 'RUIM').length,
    notaMedia: Math.round(media(anos.map((a) => a.nota)) ?? 0),
    principalRisco,
    // Dinheiro e colheita são somados, nunca tirada a média.
    producaoKg: safras.length === 0 ? null : safras.reduce((soma, x) => soma + x.producaoKg, 0),
    saldo: safras.length === 0 ? null : safras.reduce((soma, x) => soma + x.saldo, 0),
  }
}

/**
 * Resumo do intervalo, calculado só com os anos que a simulação já trouxe (não chama a API).
 * Devolve null quando nenhum ano da simulação cai no intervalo.
 */
export function agregar(plantada: Simulacao, alternativa: Simulacao | null, intervalo: IntervaloAnos): AgregadoPeriodo | null {
  const i = normalizarIntervalo(intervalo, -Infinity, Infinity)
  const anos = plantada.anos.filter((a) => a.ano >= i.inicio && a.ano <= i.fim)
  if (anos.length === 0) return null

  const chuvas = anos
    .map((a) => ({ ano: a.ano, valor: a.clima.precipitacao }))
    .filter((c) => Number.isFinite(c.valor))
  const projetados = anos.filter((a) => a.projetado).length

  return {
    inicio: anos[0].ano,
    fim: anos[anos.length - 1].ano,
    anos: anos.length,
    projetados,
    misturaProjecao: projetados > 0 && projetados < anos.length,
    chuvaMedia: media(chuvas.map((c) => c.valor)),
    chuvaMinima: chuvas.length === 0 ? null : chuvas.reduce((a, b) => (b.valor < a.valor ? b : a)),
    chuvaMaxima: chuvas.length === 0 ? null : chuvas.reduce((a, b) => (b.valor > a.valor ? b : a)),
    temperaturaMedia: media(anos.map((a) => a.clima.temperaturaMedia)),
    umidadeMedia: media(anos.map((a) => a.clima.umidadeSolo)),
    plantada: resumir(plantada, i),
    alternativa: alternativa ? resumir(alternativa, i) : null,
  }
}
