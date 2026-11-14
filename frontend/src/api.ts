import type { Cultura, ItemRanking, Ponto, Simulacao, TipoSolo } from './tipos'

const BASE = import.meta.env.VITE_API_URL ?? ''

async function chamar<T>(caminho: string, corpo?: unknown): Promise<T> {
  const resp = await fetch(`${BASE}${caminho}`, {
    method: corpo === undefined ? 'GET' : 'POST',
    headers: corpo === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })
  if (!resp.ok) {
    let detalhe = `Erro ${resp.status}`
    try {
      const erro = await resp.json()
      if (erro?.detail) detalhe = erro.detail
    } catch {
      // resposta sem JSON
    }
    throw new Error(detalhe)
  }
  return resp.json() as Promise<T>
}

export const api = {
  culturas: () => chamar<Cultura[]>('/api/culturas'),

  simular: (ponto: Ponto, solo: TipoSolo, culturaId: string) =>
    chamar<Simulacao>('/api/simulacoes', { ...ponto, solo, culturaId }),

  recomendar: (ponto: Ponto, solo: TipoSolo) =>
    chamar<ItemRanking[]>('/api/recomendacoes', { ...ponto, solo }),

  perguntar: (
    pergunta: string,
    ponto: Ponto,
    solo: TipoSolo,
    culturaPlantadaId: string,
    culturaTesteId: string | null,
  ) =>
    chamar<{ resposta: string; demonstracao: boolean }>('/api/agente', {
      pergunta,
      ...ponto,
      solo,
      culturaPlantadaId,
      culturaTesteId,
    }),
}
