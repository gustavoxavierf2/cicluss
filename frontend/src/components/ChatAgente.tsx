import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import type { Cultura, Horizonte, Ponto, SoloEstimado, TipoSolo } from '../tipos'
import { HORIZONTES } from '../tipos'
import type { Respostas } from './Seletores'
import tatuNotebook from '../assets/tatu-notebook.png'

export interface Mensagem {
  autor: 'voce' | 'agente'
  texto: string
}

/** Pedido para o Raul (re)começar as perguntas; `voz` liga a conversa falada. */
export interface PedidoGuia {
  vez: number
  voz: boolean
}

interface Props {
  aberto: boolean
  aoAbrir: () => void
  aoFechar: () => void
  pedidoGuia: PedidoGuia | null
  mensagens: Mensagem[]
  aoDizer: (m: Mensagem) => void
  /** O agente está respondendo uma pergunta livre. */
  carregando: boolean
  demonstracao: boolean
  sugestoes: string[]
  aoPerguntar: (pergunta: string) => void
  culturas: Cultura[]
  respostas: Respostas
  aoMudar: (r: Respostas) => void
  ponto: Ponto | null
  soloEstimado: SoloEstimado | null
  aoMudarHorizonte: (h: Horizonte) => void
  temResultado: boolean
  calculando: boolean
  erro: string | null
  aoCalcular: () => void
  aoImprimir: () => void
  imprimindo: boolean
}

/** As mesmas perguntas do menu, uma de cada vez. */
type Passo = 'planta' | 'comparar' | 'mapa' | 'solo' | 'hectares' | 'anos' | 'calculando' | 'fim'

const PERGUNTA: Record<Passo, string> = {
  planta: 'O que você planta?',
  comparar: 'Quer comparar com outra planta?',
  mapa: 'Onde fica o seu terreno? Clique no mapa, bem em cima da sua terra.',
  solo: 'Como é a terra?',
  hectares: 'Quantos hectares tem a área?',
  anos: 'Planejar para quantos anos: 1, 3 ou 5?',
  calculando: 'Calculando com o clima do seu terreno…',
  fim: 'Pronto! A rotação recomendada e o plano, época por época, estão no painel. Quer imprimir o relatório?',
}

const SOLOS: [string, TipoSolo, string][] = [
  ['nao sei', 'NAO_SEI', 'Não sei'],
  ['aren', 'ARENOSO', 'Arenoso'],
  ['argil', 'ARGILOSO', 'Argiloso'],
  ['mist', 'MISTO', 'Misto'],
]

const NUMERO_FALADO: Record<string, number> = { um: 1, uma: 1, dois: 2, duas: 2, tres: 3, quatro: 4, cinco: 5, dez: 10 }

/** Reconhecimento de fala do navegador (Chrome e Edge têm; Firefox não). */
interface Reconhecedor {
  lang: string
  onresult: ((e: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null
  onend: (() => void) | null
  onerror: (() => void) | null
  start(): void
  stop(): void
}
type CriaReconhecedor = new () => Reconhecedor
const janela = window as unknown as { SpeechRecognition?: CriaReconhecedor; webkitSpeechRecognition?: CriaReconhecedor }
const CriarReconhecedor = janela.SpeechRecognition ?? janela.webkitSpeechRecognition
const PODE_FALAR = 'speechSynthesis' in window

/** "Café Arábica!" vira "cafe arabica", para comparar o que foi dito com as opções. */
function normalizar(texto: string) {
  return texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, ' ').trim()
}

function acharCultura(dito: string, culturas: Cultura[]) {
  return culturas.find((c) => {
    const nome = normalizar(c.nome)
    return dito.includes(nome) || dito.split(' ').includes(nome.split(' ')[0])
  })
}

function acharNumero(texto: string): number | null {
  const achado = texto.replace(',', '.').match(/\d+(\.\d+)?/)
  if (achado) return Number(achado[0])
  const palavra = normalizar(texto).split(' ').find((p) => p in NUMERO_FALADO)
  return palavra ? NUMERO_FALADO[palavra] : null
}

/** O Raul: guia o agricultor pelas perguntas até o relatório e depois tira dúvidas sobre o resultado. */
export function ChatAgente(props: Props) {
  const {
    aberto, aoAbrir, aoFechar, pedidoGuia, mensagens, aoDizer, carregando, demonstracao, sugestoes, aoPerguntar,
    culturas, respostas, aoMudar, ponto, soloEstimado, aoMudarHorizonte, temResultado, calculando, erro,
    aoCalcular, aoImprimir, imprimindo,
  } = props
  const [texto, setTexto] = useState('')
  const [passo, setPasso] = useState<Passo | null>(null)
  const [voz, setVoz] = useState(false)
  const [ouvindo, setOuvindo] = useState(false)
  const caixa = useRef<HTMLDivElement>(null)
  const pontoAoPerguntar = useRef<Ponto | null>(null)
  const reconhecedor = useRef<Reconhecedor | null>(null)

  function ir(proximo: Passo, antes?: string) {
    setPasso(proximo)
    aoDizer({ autor: 'agente', texto: antes ? `${antes} ${PERGUNTA[proximo]}` : PERGUNTA[proximo] })
  }

  function iniciar() {
    ir('planta', 'Oi! Eu sou o Raul, o tatu do Ciclus. Vou fazer umas perguntas rápidas e montar o seu relatório de rotação de culturas.')
  }

  function naoEntendi(dica: string) {
    aoDizer({ autor: 'agente', texto: dica })
  }

  function responder(resposta: string) {
    const dito = normalizar(resposta)
    if (!dito || carregando) return
    if (passo === null || passo === 'fim') {
      if (dito.includes('recomecar') || !temResultado) return iniciar()
      return aoPerguntar(resposta)
    }
    aoDizer({ autor: 'voce', texto: resposta })

    if (passo === 'planta') {
      const c = acharCultura(dito, culturas)
      if (!c) return naoEntendi('Não achei essa planta. Escolha uma da lista.')
      aoMudar({
        ...respostas,
        culturaPlantadaId: c.id,
        culturaTesteId: respostas.culturaTesteId === c.id ? '' : respostas.culturaTesteId,
      })
      return ir('comparar')
    }
    if (passo === 'comparar') {
      const c = acharCultura(dito, culturas.filter((x) => x.id !== respostas.culturaPlantadaId))
      if (!c && !dito.startsWith('nao')) return naoEntendi('Escolha uma planta da lista ou diga "não comparar".')
      aoMudar({ ...respostas, culturaTesteId: c?.id ?? '' })
      pontoAoPerguntar.current = ponto
      return ir('mapa', ponto ? 'Já tem um lugar marcado no mapa; se a sua terra for em outro, clique nele.' : undefined)
    }
    if (passo === 'mapa') {
      if (ponto && dito.includes('marcado')) return ir('solo')
      return naoEntendi('Essa eu preciso que você mostre: clique no mapa, bem em cima da sua terra.')
    }
    if (passo === 'solo') {
      const solo = SOLOS.find(([inicio]) => dito.includes(inicio))
      if (!solo) return naoEntendi('Pode ser arenoso, argiloso, misto ou "não sei".')
      aoMudar({ ...respostas, solo: solo[1] })
      return ir('hectares')
    }
    if (passo === 'hectares') {
      const n = acharNumero(resposta)
      if (!n || n <= 0 || n > 100000) return naoEntendi('Diga só o número de hectares, por exemplo 2.')
      aoMudar({ ...respostas, hectares: String(n) })
      return ir('anos')
    }
    if (passo === 'anos') {
      const n = acharNumero(resposta)
      const h = HORIZONTES.find((x) => x === n)
      if (!h) return naoEntendi('O plano é para 1, 3 ou 5 anos. Qual você prefere?')
      aoMudarHorizonte(h)
      aoCalcular()
      return ir('calculando')
    }
  }

  // Sempre a versão mais nova, para o microfone e a fala (que respondem depois) não usarem dados velhos.
  const atual = useRef({ responder, passo })
  atual.current = { responder, passo }

  function ouvir() {
    if (!CriarReconhecedor || reconhecedor.current) return
    const r = new CriarReconhecedor()
    r.lang = 'pt-BR'
    r.onresult = (e) => {
      const dito = e.results[0]?.[0]?.transcript.trim()
      if (dito) atual.current.responder(dito)
    }
    r.onend = r.onerror = () => {
      reconhecedor.current = null
      setOuvindo(false)
    }
    try {
      r.start()
      reconhecedor.current = r
      setOuvindo(true)
    } catch {
      setOuvindo(false)
    }
  }

  // Botão "Falar com o Raul" (ou abrir o chat sem resultado): começa as perguntas.
  useEffect(() => {
    if (!pedidoGuia) return
    setVoz(pedidoGuia.voz && PODE_FALAR)
    if (passo === null || passo === 'fim') iniciar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pedidoGuia])

  // Passo do mapa: a resposta é o clique no mapa.
  useEffect(() => {
    if (passo === 'mapa' && ponto && ponto !== pontoAoPerguntar.current) ir('solo', 'Terreno marcado!')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ponto, passo])

  // Espera a conta terminar para oferecer o relatório.
  useEffect(() => {
    if (passo !== 'calculando') return
    if (erro) {
      aoDizer({ autor: 'agente', texto: `Não consegui calcular: ${erro}` })
      setPasso(null)
    } else if (temResultado && !calculando) {
      ir('fim')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [passo, temResultado, calculando, erro])

  // Conversa por voz: o Raul lê a última fala dele e, se for uma pergunta, já escuta a resposta.
  useEffect(() => {
    const ultima = mensagens[mensagens.length - 1]
    if (!voz || !aberto || !PODE_FALAR || ultima?.autor !== 'agente') return
    let cancelado = false
    const fala = new SpeechSynthesisUtterance(ultima.texto)
    fala.lang = 'pt-BR'
    fala.onend = () => {
      const p = atual.current.passo
      if (!cancelado && p && p !== 'mapa' && p !== 'calculando' && p !== 'fim') ouvir()
    }
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(fala)
    return () => {
      cancelado = true
      window.speechSynthesis.cancel()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mensagens, voz, aberto])

  // Rola só a caixa de mensagens (não a página) até a última mensagem.
  useEffect(() => {
    const el = caixa.current
    if (el) el.scrollTop = el.scrollHeight
  }, [mensagens, carregando, aberto])

  function enviar(e: FormEvent) {
    e.preventDefault()
    responder(texto)
    setTexto('')
  }

  if (!aberto) {
    return (
      <button type="button" className="chat-botao" onClick={aoAbrir} aria-expanded={false} aria-controls="chat-raul"
        aria-label={`Abrir a conversa com o Raul. ${temResultado ? 'Pergunte sobre o seu resultado' : 'Ele te guia passo a passo'}`}>
        <img src={tatuNotebook} alt="" width="64" height="60" />
        <span>
          <b>Tire dúvidas com o Raul</b>
          <small>{temResultado ? 'Pergunte sobre o seu resultado' : 'Ele te guia passo a passo'}</small>
        </span>
      </button>
    )
  }

  const emPergunta = passo !== null && passo !== 'fim'
  const outras = culturas.filter((c) => c.id !== respostas.culturaPlantadaId).map((c) => c.nome)
  const opcoes: string[] =
    passo === 'planta' ? culturas.map((c) => c.nome)
    : passo === 'comparar' ? ['Não comparar', ...outras]
    : passo === 'mapa' ? (ponto ? ['Usar o lugar marcado'] : [])
    : passo === 'solo' ? SOLOS.map(([, tipo, nome]) => (soloEstimado?.tipo === tipo ? `${nome} (sugestão do mapa)` : nome))
    : passo === 'hectares' ? ['1', '2', '5', '10']
    : passo === 'anos' ? HORIZONTES.map((h) => (h === 1 ? '1 ano' : `${h} anos`))
    : passo === 'calculando' ? []
    : temResultado ? [...sugestoes, 'Recomeçar as perguntas'] : ['Recomeçar as perguntas']

  return (
    <section className="chat" id="chat-raul" aria-label="Conversa com o Raul">
      <div className="chat__cabecalho">
        <div>
          <img className="chat__rosto" src={tatuNotebook} alt="" width="44" height="41" />
          <h2>Raul</h2>
          {demonstracao && temResultado && <span className="etiqueta etiqueta--demo">demonstração</span>}
        </div>
        <div>
          {PODE_FALAR && (
            <button type="button" className="chat__voz" aria-pressed={voz} onClick={() => setVoz(!voz)}
              title={voz ? 'Desligar a voz do Raul' : 'Ouvir o Raul falar'}>
              {voz ? '🔊' : '🔇'}
            </button>
          )}
          <button type="button" className="fechar" onClick={aoFechar} aria-label="Fechar conversa" aria-expanded={true}
            aria-controls="chat-raul">
            ×
          </button>
        </div>
      </div>

      <div className="chat__mensagens" aria-live="polite" ref={caixa}>
        {mensagens.length === 0 && (
          <div className="chat__vazio">
            <img src={tatuNotebook} alt="" width="120" height="112" />
            <p>Pergunte sobre um ano, sobre o futuro ou qual cultura combina mais com o seu terreno.</p>
          </div>
        )}
        {mensagens.map((m, i) => (
          <div key={i} className={`bolha bolha--${m.autor}`}>
            {m.texto}
          </div>
        ))}
        {(carregando || passo === 'calculando') && <div className="bolha bolha--agente bolha--digitando">pensando…</div>}
      </div>

      {passo === 'fim' && (
        <button type="button" className="botao botao--imprimir" disabled={imprimindo} onClick={aoImprimir}>
          {imprimindo ? 'Preparando o relatório…' : '🖨️ Imprimir Relatório de Rotação de Culturas'}
        </button>
      )}

      <div className="chat__sugestoes">
        {opcoes.map((s) => (
          <button key={s} type="button" className={emPergunta ? 'chip chip--resposta' : 'chip'}
            onClick={() => responder(s)} disabled={carregando}>
            {s}
          </button>
        ))}
      </div>

      <form className="chat__form" onSubmit={enviar}>
        <input
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder={emPergunta ? 'Escreva a resposta…' : 'Escreva sua pergunta…'}
          aria-label={emPergunta ? 'Sua resposta' : 'Sua pergunta'}
        />
        {CriarReconhecedor && (
          <button type="button" className={ouvindo ? 'chat__microfone chat__microfone--ouvindo' : 'chat__microfone'}
            aria-pressed={ouvindo} aria-label={ouvindo ? 'Parar de ouvir' : 'Responder falando'}
            onClick={() => (ouvindo ? reconhecedor.current?.stop() : ouvir())}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
              strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3zM5 11a7 7 0 0 0 14 0M12 18v3" />
            </svg>
          </button>
        )}
        <button type="submit" className="botao" disabled={carregando || !texto.trim()}>
          Enviar
        </button>
      </form>
    </section>
  )
}
