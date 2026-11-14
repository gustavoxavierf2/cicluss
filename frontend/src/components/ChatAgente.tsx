import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'

export interface Mensagem {
  autor: 'voce' | 'agente'
  texto: string
}

interface Props {
  mensagens: Mensagem[]
  carregando: boolean
  demonstracao: boolean
  sugestoes: string[]
  aoPerguntar: (pergunta: string) => void
}

/** Botão flutuante no canto do mapa que abre a conversa com o agente. */
export function ChatAgente({ mensagens, carregando, demonstracao, sugestoes, aoPerguntar }: Props) {
  const [aberto, setAberto] = useState(false)
  const [texto, setTexto] = useState('')
  const caixa = useRef<HTMLDivElement>(null)

  // Rola só a caixa de mensagens (não a página) até a última mensagem.
  useEffect(() => {
    const el = caixa.current
    if (el) el.scrollTop = el.scrollHeight
  }, [mensagens, carregando, aberto])

  function enviar(e: FormEvent) {
    e.preventDefault()
    const p = texto.trim()
    if (!p || carregando) return
    aoPerguntar(p)
    setTexto('')
  }

  if (!aberto) {
    return (
      <button type="button" className="chat-botao" onClick={() => setAberto(true)}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
          strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" />
        </svg>
        Tire dúvidas
      </button>
    )
  }

  return (
    <section className="chat" aria-label="Conversa com o agente">
      <div className="chat__cabecalho">
        <div>
          <h2>Tire dúvidas</h2>
          {demonstracao && <span className="etiqueta etiqueta--demo">modo demonstração</span>}
        </div>
        <button type="button" className="fechar" onClick={() => setAberto(false)} aria-label="Fechar conversa">
          ×
        </button>
      </div>

      <div className="chat__mensagens" aria-live="polite" ref={caixa}>
        {mensagens.length === 0 && (
          <p className="chat__vazio">
            Pergunte sobre um ano, sobre o futuro ou qual cultura combina mais com o seu terreno.
          </p>
        )}
        {mensagens.map((m, i) => (
          <div key={i} className={`bolha bolha--${m.autor}`}>
            {m.texto}
          </div>
        ))}
        {carregando && <div className="bolha bolha--agente bolha--digitando">pensando…</div>}
      </div>

      <div className="chat__sugestoes">
        {sugestoes.map((s) => (
          <button key={s} type="button" className="chip" onClick={() => aoPerguntar(s)} disabled={carregando}>
            {s}
          </button>
        ))}
      </div>

      <form className="chat__form" onSubmit={enviar}>
        <input
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Escreva sua pergunta…"
          aria-label="Sua pergunta"
        />
        <button type="submit" className="botao" disabled={carregando || !texto.trim()}>
          Enviar
        </button>
      </form>
    </section>
  )
}
