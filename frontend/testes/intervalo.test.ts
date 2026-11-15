// Roda com `npm test` (node --test, sem dependência extra).
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { agregar, normalizarIntervalo } from '../src/intervalo.ts'

type Nivel = 'BOM' | 'ATENCAO' | 'RUIM'

interface AnoDeTeste {
  ano: number
  nivel: Nivel
  nota: number
  risco?: string
  projetado?: boolean
  chuva?: number
  temperatura?: number
  umidade?: number
}

/** Simulação mínima: só os campos que a agregação lê. */
function simulacao(nome: string, anos: AnoDeTeste[], safras: { ano: number; producaoKg: number; saldo: number }[] = []) {
  return {
    cultura: { nome },
    anos: anos.map((a) => ({
      ano: a.ano,
      projetado: a.projetado ?? false,
      nota: a.nota,
      nivel: a.nivel,
      risco: a.risco ?? 'NENHUM',
      clima: {
        precipitacao: a.chuva ?? 800,
        temperaturaMedia: a.temperatura ?? 22,
        umidadeSolo: a.umidade ?? 0.4,
      },
    })),
    safras,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } as any
}

const MILHO = simulacao(
  'Milho',
  [
    { ano: 2023, nivel: 'BOM', nota: 80, chuva: 900, temperatura: 22, umidade: 0.5 },
    { ano: 2024, nivel: 'RUIM', nota: 30, risco: 'SECA', chuva: 400, temperatura: 24, umidade: 0.2 },
    { ano: 2025, nivel: 'ATENCAO', nota: 55, risco: 'SECA', chuva: 650, temperatura: 23, umidade: 0.35 },
    { ano: 2026, nivel: 'ATENCAO', nota: 60, risco: 'CALOR', projetado: true, chuva: 700, temperatura: 25, umidade: 0.3 },
  ],
  [
    { ano: 2023, producaoKg: 4000, saldo: 500 },
    { ano: 2024, producaoKg: 1500, saldo: -2000 },
    { ano: 2025, producaoKg: 2750, saldo: -750 },
    { ano: 2026, producaoKg: 3000, saldo: -500 },
  ],
)

test('intervalo de um ano traz os valores daquele ano', () => {
  const r = agregar(MILHO, null, { inicio: 2024, fim: 2024 })
  assert.equal(r?.anos, 1)
  assert.deepEqual([r?.plantada.bons, r?.plantada.atencao, r?.plantada.ruins], [0, 0, 1])
  assert.equal(r?.plantada.notaMedia, 30)
  assert.equal(r?.chuvaMedia, 400)
  assert.deepEqual(r?.chuvaMinima, { ano: 2024, valor: 400 })
  assert.deepEqual(r?.chuvaMaxima, { ano: 2024, valor: 400 })
  assert.equal(r?.plantada.saldo, -2000)
  assert.equal(r?.misturaProjecao, false)
  assert.equal(r?.alternativa, null)
})

test('intervalo invertido é posto em ordem e limitado aos anos disponíveis', () => {
  assert.deepEqual(normalizarIntervalo({ inicio: 2025, fim: 2023 }, 2011, 2040), { inicio: 2023, fim: 2025 })
  assert.deepEqual(normalizarIntervalo({ inicio: 1990, fim: 2090 }, 2011, 2040), { inicio: 2011, fim: 2040 })
  const direto = agregar(MILHO, null, { inicio: 2023, fim: 2025 })
  const invertido = agregar(MILHO, null, { inicio: 2025, fim: 2023 })
  assert.deepEqual(invertido, direto)
})

test('intervalo com anos projetados conta e avisa a mistura', () => {
  const r = agregar(MILHO, null, { inicio: 2023, fim: 2026 })
  assert.equal(r?.anos, 4)
  assert.equal(r?.projetados, 1)
  assert.equal(r?.misturaProjecao, true)
  // Só tendência: não é mistura.
  const futuro = agregar(MILHO, null, { inicio: 2026, fim: 2026 })
  assert.equal(futuro?.projetados, 1)
  assert.equal(futuro?.misturaProjecao, false)
})

test('sem safra, produção e saldo ficam nulos', () => {
  const semSafra = simulacao('Sorgo', [{ ano: 2024, nivel: 'BOM', nota: 75 }])
  const r = agregar(semSafra, null, { inicio: 2024, fim: 2024 })
  assert.equal(r?.plantada.producaoKg, null)
  assert.equal(r?.plantada.saldo, null)
})

test('produção e saldo são somados, não promediados', () => {
  const r = agregar(MILHO, MILHO, { inicio: 2023, fim: 2025 })
  assert.equal(r?.plantada.producaoKg, 8250)
  assert.equal(r?.plantada.saldo, -2250)
  assert.equal(r?.alternativa?.saldo, -2250)
})

test('médias, mínimo, máximo e risco mais repetido', () => {
  const r = agregar(MILHO, null, { inicio: 2023, fim: 2025 })
  assert.equal(r?.chuvaMedia, 650)
  assert.equal(r?.temperaturaMedia, 23)
  assert.ok(Math.abs((r?.umidadeMedia ?? 0) - 0.35) < 1e-9)
  assert.deepEqual(r?.chuvaMinima, { ano: 2024, valor: 400 })
  assert.deepEqual(r?.chuvaMaxima, { ano: 2023, valor: 900 })
  assert.equal(r?.plantada.notaMedia, 55)
  assert.deepEqual(r?.plantada.principalRisco, { risco: 'SECA', anos: 2 })
})

test('médias ignoram anos sem valor e intervalo fora da simulação devolve null', () => {
  const comBuraco = simulacao('Feijão', [
    { ano: 2024, nivel: 'BOM', nota: 70, chuva: NaN },
    { ano: 2025, nivel: 'BOM', nota: 90, chuva: 600 },
  ])
  assert.equal(agregar(comBuraco, null, { inicio: 2024, fim: 2025 })?.chuvaMedia, 600)
  assert.equal(agregar(MILHO, null, { inicio: 1990, fim: 1995 }), null)
})
