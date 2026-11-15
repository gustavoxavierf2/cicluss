import { useEffect, useRef, useState } from 'react'
import { api } from './api'
import type { Cultura, Horizonte, ItemRanking, Planejamento, Ponto, Simulacao, SoloEstimado } from './tipos'
import { NOME_SOLO } from './tipos'
import { MapaTerreno } from './components/MapaTerreno'
import { Seletores } from './components/Seletores'
import type { Respostas } from './components/Seletores'
import { BarraAnos } from './components/BarraAnos'
import { DetalheAno } from './components/DetalheAno'
import { ResumoComparativo } from './components/ResumoComparativo'
import { ResumoPeriodo } from './components/ResumoPeriodo'
import type { IntervaloAnos } from './intervalo'
import { agregar } from './intervalo'
import { Ranking } from './components/Ranking'
import { CenarioClima } from './components/CenarioClima'
import { PlanoPlantio } from './components/PlanoPlantio'
import { Rotacoes } from './components/Rotacoes'
import { ChatAgente } from './components/ChatAgente'
import type { Mensagem, PedidoGuia } from './components/ChatAgente'
import { RelatorioRotacao } from './components/RelatorioRotacao'
import logo from './assets/logo-ciclus.png'
import tatuPlantando from './assets/tatu-plantando.png'
import tatuDormindo from './assets/tatu-dormindo.png'
import './App.css'

interface Resultado {
  plantada: Simulacao
  alternativa: Simulacao | null
  ranking: ItemRanking[]
}

type Aba = 'anos' | 'rotacao' | 'plantar'

/** As três visões do resultado, escolhidas no menu da direita. */
const VISOES: { id: Aba; nome: string; icone: string }[] = [
  { id: 'anos', nome: 'Ano a ano', icone: 'M7 3v3M17 3v3M4 9h16M5 6h14a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z' },
  { id: 'rotacao', nome: 'Rotação e planejamento', icone: 'M20 12a8 8 0 1 1-2.34-5.66M20 4v4h-4' },
  { id: 'plantar', nome: 'O que plantar', icone: 'M12 21v-9M12 12c0-4 3-6 7-6 0 4-3 6-7 6zM12 15c0-3-2-5-6-5 0 3 2 5 6 5z' },
]

/** Área digitada pelo agricultor; null quando está vazia ou inválida. */
function lerHectares(texto: string): number | null {
  const n = Number(texto.replace(',', '.'))
  return texto.trim() !== '' && Number.isFinite(n) && n > 0 ? n : null
}

export default function App() {
  const [culturas, setCulturas] = useState<Cultura[]>([])
  const [ponto, setPonto] = useState<Ponto | null>(null)
  const [respostas, setRespostas] = useState<Respostas>({ solo: 'NAO_SEI', culturaPlantadaId: '', culturaTesteId: '', hectares: '1' })
  const [ativo, setAtivo] = useState(false)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  // O intervalo é o foco da aba "Ano a ano"; `ano` é o ano aberto em detalhe, sempre dentro dele.
  const [intervalo, setIntervalo] = useState<IntervaloAnos>({ inicio: 0, fim: 0 })
  const [ano, setAno] = useState(0)
  const [aba, setAba] = useState<Aba>('anos')
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [mensagens, setMensagens] = useState<Mensagem[]>([])
  const [pensando, setPensando] = useState(false)
  const [demonstracao, setDemonstracao] = useState(true)
  const [culturaPlanoId, setCulturaPlanoId] = useState('')
  const [fonteClima, setFonteClima] = useState<string | null>(null)
  const [soloEstimado, setSoloEstimado] = useState<SoloEstimado | null>(null)
  const [painelAberto, setPainelAberto] = useState(true)
  const [painelGrande, setPainelGrande] = useState(false)
  const [horizonte, setHorizonte] = useState<Horizonte>(3)
  const [chatAberto, setChatAberto] = useState(false)
  const [pedidoGuia, setPedidoGuia] = useState<PedidoGuia | null>(null)
  const [relatorio, setRelatorio] = useState<{ plano: Planejamento; feitoEm: string } | null>(null)
  const [imprimindo, setImprimindo] = useState(false)
  const hectares = lerHectares(respostas.hectares)
  const pedido = useRef(0)

  useEffect(() => {
    api.info().then((i) => setFonteClima(i.fonteClima)).catch(() => setFonteClima(null))
    api.culturas()
      .then((lista) => {
        setCulturas(lista)
        setRespostas((r) => ({
          ...r,
          culturaPlantadaId: lista[0]?.id ?? '',
          culturaTesteId: lista[1]?.id ?? '',
        }))
      })
      .catch(() => setErro('Não consegui falar com o servidor. O backend está rodando na porta 8080?'))
  }, [])

  // Ao marcar o terreno, busca no mapa de solos que tipo de terra é ali (só uma sugestão).
  useEffect(() => {
    setSoloEstimado(null)
    if (!ponto) return
    let cancelado = false
    api.soloDoPonto(ponto)
      .then((s) => !cancelado && setSoloEstimado(s))
      .catch(() => undefined)
    return () => {
      cancelado = true
    }
  }, [ponto])

  // Depois do primeiro "Ver resultado", mudar o terreno ou um seletor recalcula sozinho.
  useEffect(() => {
    if (!ativo || !ponto || !respostas.culturaPlantadaId) return
    const meu = ++pedido.current
    setCarregando(true)
    setErro(null)
    const teste = respostas.culturaTesteId || null
    Promise.all([
      api.simular(ponto, respostas.solo, respostas.culturaPlantadaId, hectares),
      teste ? api.simular(ponto, respostas.solo, teste, hectares) : Promise.resolve(null),
      api.recomendar(ponto, respostas.solo),
    ])
      .then(([plantada, alternativa, ranking]) => {
        if (meu !== pedido.current) return
        setResultado({ plantada, alternativa, ranking })
        const existe = (a: number) => plantada.anos.some((x) => x.ano === a)
        setAno((atual) => (existe(atual) ? atual : plantada.anoAtual))
        // Padrão: só o último ano completo, como era antes de existir o intervalo.
        setIntervalo((i) => (existe(i.inicio) && existe(i.fim) ? i : { inicio: plantada.anoAtual, fim: plantada.anoAtual }))
      })
      .catch((e: Error) => meu === pedido.current && setErro(e.message))
      .finally(() => meu === pedido.current && setCarregando(false))
  }, [ativo, ponto, respostas.solo, respostas.culturaPlantadaId, respostas.culturaTesteId, hectares])

  function perguntar(pergunta: string) {
    if (!ponto || !resultado) return
    setMensagens((m) => [...m, { autor: 'voce', texto: pergunta }])
    setPensando(true)
    api.perguntar(pergunta, ponto, respostas.solo, respostas.culturaPlantadaId, respostas.culturaTesteId || null, hectares)
      .then((r) => {
        setDemonstracao(r.demonstracao)
        setMensagens((m) => [...m, { autor: 'agente', texto: r.resposta }])
      })
      .catch((e: Error) => setMensagens((m) => [...m, { autor: 'agente', texto: `Não consegui responder: ${e.message}` }]))
      .finally(() => setPensando(false))
  }

  // O relatório só existe na folha impressa: monta, espera as figuras carregarem e chama a impressão.
  useEffect(() => {
    if (!relatorio) return
    let cancelado = false
    const figuras = Array.from(document.querySelectorAll<HTMLImageElement>('.relatorio img'))
    Promise.all(figuras.map((f) => f.decode().catch(() => undefined))).then(() => !cancelado && window.print())
    return () => {
      cancelado = true
    }
  }, [relatorio])

  /** Imprime o plano que já está na tela ou, vindo do Raul, busca o plano da cultura plantada. */
  function imprimirRelatorio(pronto?: Planejamento) {
    if (!ponto) return
    setImprimindo(true)
    const plano = pronto
      ? Promise.resolve(pronto)
      : api.planejar(ponto, respostas.solo, hectares, culturaPlanoId || respostas.culturaPlantadaId, horizonte)
    plano
      .then((p) => setRelatorio({ plano: p, feitoEm: new Date().toLocaleDateString('pt-BR') }))
      .catch((e: Error) => setMensagens((m) => [...m, { autor: 'agente', texto: `Não consegui montar o relatório: ${e.message}` }]))
      .finally(() => setImprimindo(false))
  }

  /** Abre a conversa com o Raul; sem resultado (ou a pedido), ele começa a fazer as perguntas. */
  function abrirChat(guiar: boolean, voz = false) {
    setChatAberto(true)
    if (guiar || !resultado) setPedidoGuia({ vez: Date.now(), voz })
  }

  function compararCom(culturaId: string) {
    if (culturaId === respostas.culturaPlantadaId) return
    setRespostas({ ...respostas, culturaTesteId: culturaId })
    setAba('anos')
    setPainelAberto(true)
  }

  /** Abre o plano (segunda metade de "Rotação e planejamento") com a cultura escolhida. */
  function planejarCom(culturaId: string) {
    setCulturaPlanoId(culturaId)
    setAba('rotacao')
    setPainelAberto(true)
    setTimeout(() => document.getElementById('plano')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100)
  }

  /**
   * Histórico observado para o relatório: o clima vale para qualquer cultura; o desempenho só entra
   * se a cultura do plano for uma das duas já simuladas (não busca nada novo).
   */
  function historicoDe(culturaId: string) {
    if (!resultado) return null
    const { plantada, alternativa } = resultado
    const doPlano = [plantada, alternativa].find((x) => x?.cultura.id === culturaId) ?? null
    const g = agregar(doPlano ?? plantada, null, { inicio: plantada.anos[0].ano, fim: plantada.anoAtual })
    return g && { ...g, temCultura: doPlano !== null }
  }

  function mudarIntervalo(i: IntervaloAnos) {
    setIntervalo(i)
    setAno((atual) => (atual < i.inicio || atual > i.fim ? i.fim : atual))
  }

  const anos = resultado?.plantada.anos ?? []
  const agregado = resultado ? agregar(resultado.plantada, resultado.alternativa, intervalo) : null
  const barraAnos = resultado && (
    <BarraAnos
      primeiro={anos[0].ano}
      ultimo={anos[anos.length - 1].ano}
      anoAtual={resultado.plantada.anoAtual}
      intervalo={intervalo}
      aoMudar={mudarIntervalo}
    />
  )
  // O painel da esquerda aparece sempre: com o guia no começo, depois com o resultado ou o erro.
  const temPainel = true
  const sugestoes = resultado
    ? [
        `O que aconteceu em ${ano}?`,
        resultado.alternativa ? 'Vale a pena trocar?' : 'Qual a melhor cultura pra mim?',
        'E no futuro?',
      ]
    : []

  return (
    <div className={`app app--mapa ${temPainel && painelAberto ? 'app--painel' : ''} ${painelGrande ? 'app--painel-grande' : ''} ${chatAberto ? 'app--chat' : ''}`}>
      <div className="palco">
        <MapaTerreno ponto={ponto} aoEscolher={setPonto} />
      </div>

      <aside className="menu" aria-label="Seu terreno">
        <div className="menu__marca">
          <div className="menu__topo">
            <h1 className="topo__logo">
              <img src={logo} alt="Ciclus" width="120" height="36" />
            </h1>
            <button type="button" className="assistente-botao" onClick={() => abrirChat(true, true)}
              title="O Raul faz as perguntas em voz alta e monta o seu relatório">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4"
                strokeLinecap="round" aria-hidden="true">
                <path d="M4 10v4M8 6v12M12 3v18M16 7v10M20 10v4" />
              </svg>
              Assistente
            </button>
          </div>
          <p>Qual rotação de culturas protege a sua terra e a sua renda?</p>
        </div>

        <Seletores
          culturas={culturas}
          respostas={respostas}
          aoMudar={setRespostas}
          podeCalcular={!!ponto && culturas.length > 0}
          carregando={carregando}
          aoCalcular={() => {
            setAtivo(true)
            setPainelAberto(true)
          }}
        />

        <div className="menu__terreno">
          <p className="mapa__legenda">
            {ponto
              ? `Terreno marcado: ${ponto.latitude.toFixed(4)}, ${ponto.longitude.toFixed(4)}. Clique em outro lugar do mapa para mudar.`
              : 'Clique no mapa onde fica o seu terreno.'}
          </p>
          {soloEstimado && (
            <p className="mapa__solo">
              Pelo mapa de solos, a terra aqui parece <b>{NOME_SOLO[soloEstimado.tipo].split(' (')[0].toLowerCase()}</b>
              : {Math.round(soloEstimado.argila)}% de argila e {Math.round(soloEstimado.areia)}% de areia.{' '}
              {respostas.solo !== soloEstimado.tipo && (
                <button type="button" className="link"
                  onClick={() => setRespostas({ ...respostas, solo: soloEstimado.tipo })}>
                  usar esta terra
                </button>
              )}
              <span className="mapa__fonte">Fonte: {soloEstimado.fonte}. É uma estimativa, não análise de solo.</span>
            </p>
          )}
        </div>

        <nav className="visoes" aria-label="O que ver">
          <h2 className="menu__titulo">O que ver</h2>
          {VISOES.map((v) => (
            <button key={v.id} type="button" disabled={!resultado}
              aria-current={resultado && aba === v.id ? 'page' : undefined}
              className={resultado && aba === v.id ? 'visao visao--ativa' : 'visao'}
              onClick={() => {
                setAba(v.id)
                setPainelAberto(true)
              }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d={v.icone} />
              </svg>
              {v.nome}
            </button>
          ))}
        </nav>

        <div className="menu__rodape">
          <div className="legenda" aria-label="Legenda das cores">
            <span className="legenda__item legenda__item--bom">Bom</span>
            <span className="legenda__item legenda__item--atencao">Atenção</span>
            <span className="legenda__item legenda__item--ruim">Ruim</span>
          </div>
          <span className="topo__demo">
            {fonteClima === 'nasa'
              ? 'clima: NASA POWER · culturas e preços de exemplo'
              : fonteClima === 'http'
                ? 'culturas e preços de exemplo'
                : 'dados de demonstração'}
          </span>
        </div>
      </aside>

      {!resultado && !erro && (
        <p className="dica" role="status">
          {!ponto
            ? 'Clique no mapa onde fica o seu terreno'
            : carregando
              ? 'Buscando o clima da NASA para este terreno…'
              : 'Agora aperte Ver resultado'}
        </p>
      )}

      {resultado && aba === 'anos' && !(painelGrande && painelAberto) && (
        <div className="tempo">{barraAnos}</div>
      )}

      {temPainel && !painelAberto && (
        <button type="button" className="painel-abrir" onClick={() => setPainelAberto(true)}>
          Abrir resultado
        </button>
      )}

      {temPainel && painelAberto && (
        <section className="painel" aria-label="Resultado">
          <header className="painel__cabecalho">
            <h2>{resultado ? VISOES.find((v) => v.id === aba)?.nome : 'Como usar'}</h2>
            <div className="painel__acoes">
              <button type="button" className="painel__aumentar" aria-pressed={painelGrande}
                onClick={() => setPainelGrande(!painelGrande)}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2"
                  strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  {painelGrande
                    ? <path d="M9 4v5H4M15 4v5h5M9 20v-5H4M15 20v-5h5" />
                    : <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" />}
                </svg>
                {painelGrande ? 'Diminuir' : 'Aumentar'}
              </button>
              <button type="button" className="fechar" onClick={() => setPainelAberto(false)} aria-label="Fechar painel">
                ×
              </button>
            </div>
          </header>

          <div className={`painel__corpo ${carregando && resultado ? 'painel__corpo--carregando' : ''}`}>
            {erro && (
              <div className="erro" role="alert">
                {!resultado && <img className="erro__tatu" src={tatuDormindo} alt="" width="84" height="61" />}
                <span>{erro}</span>
              </div>
            )}

            {!resultado && !erro && (
              <div className="guia">
                <img className="guia__tatu" src={tatuPlantando} alt="" width="150" height="147" />
                <h2>Comece em 3 passos</h2>
                <ol className="passos">
                  <li className={respostas.culturaPlantadaId ? 'passo passo--feito' : 'passo'}>
                    <span className="passo__numero" aria-hidden="true">1</span>
                    <div>
                      <b>Escolha o que você planta</b>
                      <span>No menu verde, do lado direito.</span>
                    </div>
                  </li>
                  <li className={ponto ? 'passo passo--feito' : 'passo'}>
                    <span className="passo__numero" aria-hidden="true">2</span>
                    <div>
                      <b>Clique no mapa</b>
                      <span>Bem em cima de onde fica a sua terra.</span>
                    </div>
                  </li>
                  <li className="passo">
                    <span className="passo__numero" aria-hidden="true">3</span>
                    <div>
                      <b>Aperte “Ver resultado”</b>
                      <span>É o botão laranja do menu.</span>
                    </div>
                  </li>
                </ol>
                <button type="button" className="botao-claro guia__raul" onClick={() => abrirChat(true)}>
                  Prefere conversar? O Raul pergunta e monta tudo
                </button>
                <p className="guia__fim">
                  Aí você vê se cada ano foi <b className="cor-bom">bom</b>, de <b className="cor-atencao">atenção</b>{' '}
                  ou <b className="cor-ruim">ruim</b> para a sua planta.
                </p>
              </div>
            )}

            {resultado && aba === 'anos' && (
              <>
                {painelGrande && barraAnos}
                {agregado && agregado.anos > 1
                  ? <ResumoPeriodo agregado={agregado} plantada={resultado.plantada} ano={ano} aoEscolherAno={setAno} />
                  : <ResumoComparativo plantada={resultado.plantada} alternativa={resultado.alternativa} />}
                <DetalheAno plantada={resultado.plantada} alternativa={resultado.alternativa} ano={ano} />
                {ponto && (
                  <CenarioClima
                    ponto={ponto}
                    solo={respostas.solo}
                    hectares={hectares}
                    plantada={resultado.plantada}
                    alternativa={resultado.alternativa}
                    ano={ano}
                  />
                )}
              </>
            )}

            {resultado && ponto && aba === 'rotacao' && (
              <>
                <Rotacoes
                  ponto={ponto}
                  solo={respostas.solo}
                  hectares={hectares}
                  culturaPlantadaId={respostas.culturaPlantadaId}
                  culturaTesteId={respostas.culturaTesteId || null}
                  culturaDoPlanoId={culturaPlanoId || respostas.culturaPlantadaId}
                  aoPlanejar={planejarCom}
                />
                <PlanoPlantio
                  ponto={ponto}
                  solo={respostas.solo}
                  hectares={hectares}
                  culturas={culturas}
                  culturaId={culturaPlanoId || respostas.culturaPlantadaId}
                  aoEscolherCultura={setCulturaPlanoId}
                  horizonte={horizonte}
                  aoMudarHorizonte={setHorizonte}
                  aoImprimir={imprimirRelatorio}
                  imprimindo={imprimindo}
                />
              </>
            )}

            {resultado && aba === 'plantar' && (
              <Ranking itens={resultado.ranking} aoTestar={compararCom} aoPlanejar={planejarCom} />
            )}
          </div>
        </section>
      )}

      <ChatAgente
        aberto={chatAberto}
        aoAbrir={() => abrirChat(false)}
        aoFechar={() => setChatAberto(false)}
        pedidoGuia={pedidoGuia}
        mensagens={mensagens}
        aoDizer={(m) => setMensagens((lista) => [...lista, m])}
        carregando={pensando}
        demonstracao={demonstracao}
        sugestoes={sugestoes}
        aoPerguntar={perguntar}
        culturas={culturas}
        respostas={respostas}
        aoMudar={setRespostas}
        ponto={ponto}
        soloEstimado={soloEstimado}
        aoMudarHorizonte={setHorizonte}
        temResultado={!!resultado}
        calculando={carregando}
        erro={erro}
        aoCalcular={() => {
          setAtivo(true)
          setCulturaPlanoId('')
          setAba('rotacao')
          setPainelAberto(true)
        }}
        aoImprimir={() => imprimirRelatorio()}
        imprimindo={imprimindo}
      />

      {relatorio && ponto && resultado && (
        <RelatorioRotacao
          plano={relatorio.plano}
          feitoEm={relatorio.feitoEm}
          ponto={ponto}
          solo={respostas.solo}
          soloEstimado={soloEstimado}
          fonteClima={fonteClima}
          historico={historicoDe(relatorio.plano.principal.id)}
        />
      )}
    </div>
  )
}
