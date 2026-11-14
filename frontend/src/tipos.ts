// Espelha os records do backend (pacote br.com.fieldshift.core.modelo).

export type TipoSolo = 'ARENOSO' | 'ARGILOSO' | 'MISTO' | 'NAO_SEI'
export type Nivel = 'BOM' | 'ATENCAO' | 'RUIM'
export type Risco = 'NENHUM' | 'SECA' | 'CHUVA_EXCESSIVA' | 'CALOR' | 'FRIO' | 'SOLO_INADEQUADO'

export type GrupoRotacao = 'GRAMINEA' | 'LEGUMINOSA' | 'RAIZ' | 'PERENE'

export interface Economia {
  produtividadeKgHa: number
  precoPorKg: number
  custoPorHa: number
  fonte: string
}

/** Estimativa de safra e dinheiro de uma cultura em um ano (valores em kg e R$). */
export interface Safra {
  ano: number
  hectares: number
  producaoKg: number
  receita: number
  custo: number
  saldo: number
}

export interface Faixa {
  idealMin: number
  idealMax: number
  absMin: number
  absMax: number
}

export interface Cultura {
  id: string
  nome: string
  temperatura: Faixa
  chuva: Faixa
  solos: TipoSolo[]
  cicloDias: number
  fonte: string
  grupoRotacao: GrupoRotacao | null
  economia: Economia | null
}

export interface ClimaAnual {
  ano: number
  projetado: boolean
  temperaturaMedia: number
  temperaturaMaxima: number
  temperaturaMinima: number
  precipitacao: number
  umidadeSolo: number
  evapotranspiracao: number
  /** Chuva e temperatura dos 4 trimestres; null quando a fonte só tem o valor do ano. */
  trimestres: Trimestre[] | null
}

export interface Trimestre {
  nome: string
  chuva: number
  temperaturaMedia: number
}

export interface IndicadorSolo {
  nome: string
  nivel: Nivel
  texto: string
}

export interface Estrategia {
  id: string
  nome: string
  descricao: string
  atual: boolean
  sequencia: Cultura[]
  anos: { ano: number; cultura: string; nota: number; nivel: Nivel; risco: Risco }[]
  resumo: Resumo
  saldoTotal: number | null
  solo: IndicadorSolo[]
  pontosSolo: number
  mesesDescoberto: number
}

export interface ComparacaoEstrategias {
  estrategias: Estrategia[]
  recomendadaId: string
  porque: string
  epocaSeca: string | null
  hectares: number
}

export interface SoloEstimado {
  tipo: TipoSolo
  argila: number
  areia: number
  silte: number
  fonte: string
}

export interface ResultadoAno {
  ano: number
  projetado: boolean
  nota: number
  nivel: Nivel
  risco: Risco
  motivo: string
  clima: ClimaAnual
}

export interface Resumo {
  anosBons: number
  anosAtencao: number
  anosRuins: number
  notaMedia: number
}

export interface Simulacao {
  cultura: Cultura
  latitude: number
  longitude: number
  solo: TipoSolo | null
  anoAtual: number
  anos: ResultadoAno[]
  resumoPassado: Resumo
  resumoFuturo: Resumo
  hectares: number | null
  safras: Safra[]
}

export interface Cenario {
  cultura: Cultura
  resultado: ResultadoAno
  safra: Safra | null
}

export interface AnoPlano {
  ano: number
  cultura: Cultura
  principal: boolean
  resultado: ResultadoAno
  safra: Safra | null
  porque: string
  cuidado: string
  planoB: Cultura | null
}

export interface Planejamento {
  principal: Cultura
  hectares: number
  anos: AnoPlano[]
  saldoTotal: number | null
  anosDeRisco: number
  resumo: string
  avisos: string[]
}

export interface ItemRanking {
  cultura: Cultura
  posicao: number
  notaMedia: number
  nivel: Nivel
  anosBons: number
  totalAnos: number
  principalRisco: Risco
  motivo: string
}

export interface Ponto {
  latitude: number
  longitude: number
}

export const NOME_NIVEL: Record<Nivel, string> = {
  BOM: 'Bom',
  ATENCAO: 'Atenção',
  RUIM: 'Ruim',
}

export const NOME_SOLO: Record<TipoSolo, string> = {
  ARENOSO: 'Arenoso (terra solta, clara)',
  ARGILOSO: 'Argiloso (terra pesada, gruda na mão)',
  MISTO: 'Misto',
  NAO_SEI: 'Não sei',
}
