import { useEffect, useRef, useState } from 'react'
import { api } from './api'
import type { Cultura, ItemRanking, Ponto, Simulacao, SoloEstimado } from './tipos'
import { NOME_SOLO } from './tipos'
import { MapaTerreno } from './components/MapaTerreno'
import { Seletores } from './components/Seletores'
import type { Respostas } from './components/Seletores'
import { BarraAnos } from './components/BarraAnos'
import { DetalheAno } from './components/DetalheAno'
import { ResumoComparativo } from './components/ResumoComparativo'
import { Ranking } from './components/Ranking'
import { CenarioClima } from './components/CenarioClima'
import { PlanoPlantio } from './components/PlanoPlantio'
import { Rotacoes } from './components/Rotacoes'
import { ChatAgente } from './components/ChatAgente'
import type { Mensagem } from './components/ChatAgente'
import logo from './assets/logo-ciclus.png'
import tatuPlantando from './assets/tatu-plantando.png'
import tatuDormindo from './assets/tatu-dormindo.png'
import './App.css'

interface Resultado {
  plantada: Simulacao
  alternativa: Simulacao | null
  ranking: ItemRanking[]
}

type Aba = 'anos' | 'rotacao' | 'plantar' | 'plano'

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
        setAno((atual) => (plantada.anos.some((a) => a.ano === atual) ? atual : plantada.anoAtual))
        setMensagens([])
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

  function compararCom(culturaId: string) {
    if (culturaId === respostas.culturaPlantadaId) return
    setRespostas({ ...respostas, culturaTesteId: culturaId })
    setAba('anos')
  }

  function planejarCom(culturaId: string) {
    setCulturaPlanoId(culturaId)
    setAba('plano')
  }

  const anos = resultado?.plantada.anos ?? []
  const sugestoes = resultado
    ? [
        `O que aconteceu em ${ano}?`,
        resultado.alternativa ? 'Vale a pena trocar?' : 'Qual a melhor cultura pra mim?',
        'E no futuro?',
      ]
    : []

  return (
    <div className="app">
      <header className="topo">
        <div className="topo__marca">
          <h1 className="topo__logo">
            <img src={logo} alt="Ciclus" width="120" height="36" />
          </h1>
          <p>Qual rotação de culturas protege a sua terra e a sua renda? Simulador com dados de satélite da NASA</p>
        </div>
        <span className="topo__demo">
          {fonteClima === 'nasa'
            ? 'clima: NASA POWER · culturas e preços de exemplo'
            : fonteClima === 'http'
              ? 'culturas e preços de exemplo'
              : 'dados de demonstração'}
        </span>
      </header>

      <Seletores
        culturas={culturas}
        respostas={respostas}
        aoMudar={setRespostas}
        podeCalcular={!!ponto && culturas.length > 0}
        carregando={carregando}
        aoCalcular={() => setAtivo(true)}
      />

      <main className="conteudo">
        <section className="mapa">
          <h2 className="secao-titulo">Onde fica o seu terreno?</h2>
          <MapaTerreno ponto={ponto} aoEscolher={setPonto} />
          <p className="mapa__legenda">
            {ponto
              ? `Terreno marcado: ${ponto.latitude.toFixed(4)}, ${ponto.longitude.toFixed(4)}. Clique em outro lugar para mudar.`
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
        </section>

        <section className="painel">
          <nav className="abas" role="tablist">
            <button type="button" role="tab" aria-selected={aba === 'anos'} className={aba === 'anos' ? 'aba aba--ativa' : 'aba'}
              onClick={() => setAba('anos')}>
              Ano a ano
            </button>
            <button type="button" role="tab" aria-selected={aba === 'rotacao'} className={aba === 'rotacao' ? 'aba aba--ativa' : 'aba'}
              onClick={() => setAba('rotacao')}>
              Rotação
            </button>
            <button type="button" role="tab" aria-selected={aba === 'plantar'} className={aba === 'plantar' ? 'aba aba--ativa' : 'aba'}
              onClick={() => setAba('plantar')}>
              O que plantar
            </button>
            <button type="button" role="tab" aria-selected={aba === 'plano'} className={aba === 'plano' ? 'aba aba--ativa' : 'aba'}
              onClick={() => setAba('plano')}>
              Planejamento
            </button>
          </nav>

          <div className={`painel__corpo ${carregando && resultado ? 'painel__corpo--carregando' : ''}`}>
            {erro && (
              <div className="erro" role="alert">
                {!resultado && <img className="erro__tatu" src={tatuDormindo} alt="" width="84" height="61" />}
                <span>{erro}</span>
              </div>
            )}

            {!resultado && !erro && (
              <div className="vazio">
                <div className="vazio__texto">
                  <h2>Seu terreno, ano a ano</h2>
                  <ol>
                    <li>Escolha o que você planta hoje e o que quer comparar, lá em cima.</li>
                    <li>Clique no mapa onde fica o terreno.</li>
                    <li>Aperte <b>Ver resultado</b>.</li>
                  </ol>
                  <p>
                    Você vai ver, ano a ano, como cada cultura teria ido com o clima que realmente aconteceu ali, e
                    comparar rotações de culturas pelo clima e pela saúde da terra.
                  </p>
                </div>
                <img className="vazio__tatu" src={tatuPlantando} alt="" width="150" height="147" />
              </div>
            )}

            {resultado && aba === 'anos' && (
              <>
                <BarraAnos
                  primeiro={anos[0].ano}
                  ultimo={anos[anos.length - 1].ano}
                  anoAtual={resultado.plantada.anoAtual}
                  ano={ano}
                  aoMudar={setAno}
                />
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
                <ResumoComparativo plantada={resultado.plantada} alternativa={resultado.alternativa} />
              </>
            )}

            {resultado && ponto && aba === 'rotacao' && (
              <Rotacoes
                ponto={ponto}
                solo={respostas.solo}
                hectares={hectares}
                culturaPlantadaId={respostas.culturaPlantadaId}
                culturaTesteId={respostas.culturaTesteId || null}
                aoPlanejar={planejarCom}
              />
            )}

            {resultado && aba === 'plantar' && (
              <Ranking itens={resultado.ranking} aoTestar={compararCom} aoPlanejar={planejarCom} />
            )}

            {resultado && ponto && aba === 'plano' && (
              <PlanoPlantio
                ponto={ponto}
                solo={respostas.solo}
                hectares={hectares}
                culturas={culturas}
                culturaId={culturaPlanoId || respostas.culturaPlantadaId}
                aoEscolherCultura={setCulturaPlanoId}
              />
            )}
          </div>
        </section>
      </main>

      {resultado && (
        <ChatAgente
          mensagens={mensagens}
          carregando={pensando}
          demonstracao={demonstracao}
          sugestoes={sugestoes}
          aoPerguntar={perguntar}
        />
      )}
    </div>
  )
}
