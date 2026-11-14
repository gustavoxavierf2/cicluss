package br.com.fieldshift.api;

import br.com.fieldshift.core.agente.AgenteComReserva;
import br.com.fieldshift.core.agente.ContextoAgente;
import br.com.fieldshift.core.agente.RespostaAgente;
import br.com.fieldshift.core.modelo.Cenario;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.ItemRanking;
import br.com.fieldshift.core.modelo.Planejamento;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.servico.CulturaRepository;
import br.com.fieldshift.core.servico.PlanejamentoService;
import br.com.fieldshift.core.servico.RecomendacaoService;
import br.com.fieldshift.core.servico.SimuladorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SimuladorController {

    private final CulturaRepository culturas;
    private final SimuladorService simulador;
    private final RecomendacaoService recomendacao;
    private final PlanejamentoService planejamento;
    private final AgenteComReserva agente;
    private final String fonteClima;

    public SimuladorController(CulturaRepository culturas, SimuladorService simulador,
                               RecomendacaoService recomendacao, PlanejamentoService planejamento,
                               AgenteComReserva agente,
                               @Value("${fieldshift.clima.fonte:mock}") String fonteClima) {
        this.culturas = culturas;
        this.simulador = simulador;
        this.recomendacao = recomendacao;
        this.planejamento = planejamento;
        this.agente = agente;
        this.fonteClima = fonteClima;
    }

    /** De onde vem o clima, para o site avisar quando os dados são de demonstração. */
    @GetMapping("/info")
    public Requisicoes.Info info() {
        return new Requisicoes.Info(fonteClima, "mock".equals(fonteClima));
    }

    /** Lista de culturas para os selects do front. */
    @GetMapping("/culturas")
    public List<Cultura> culturas() {
        return culturas.todas();
    }

    /** Linha do tempo de uma cultura no terreno: 15 anos para trás e 15 para frente. */
    @PostMapping("/simulacoes")
    public Simulacao simular(@Valid @RequestBody Requisicoes.Simulacao req) {
        return simulador.simular(req.latitude(), req.longitude(), req.solo(), req.culturaId(), req.hectares());
    }

    /** Ranking das culturas mais aptas para o terreno. */
    @PostMapping("/recomendacoes")
    public List<ItemRanking> recomendar(@Valid @RequestBody Requisicoes.Recomendacao req) {
        return recomendacao.recomendar(req.latitude(), req.longitude(), req.solo());
    }

    /** "E se o clima fosse assim?": a mesma regra da nota, com temperatura, chuva e umidade ajustadas. */
    @PostMapping("/cenarios")
    public Cenario cenario(@Valid @RequestBody Requisicoes.Cenario req) {
        return simulador.cenario(req.latitude(), req.longitude(), req.solo(), req.culturaId(), req.hectares(),
                req.anoBase(), req.temperaturaMedia(), req.precipitacao(), req.umidadeSolo());
    }

    /** Plano de plantio com rotação para os próximos 1, 3 ou 5 anos. */
    @PostMapping("/planejamentos")
    public Planejamento planejar(@Valid @RequestBody Requisicoes.Planejamento req) {
        return planejamento.planejar(req.latitude(), req.longitude(), req.solo(), req.hectares(),
                req.culturaPrincipalId(), req.anos());
    }

    /** Pergunta ao agente sobre os resultados daquele terreno. */
    @PostMapping("/agente")
    public Requisicoes.Resposta perguntar(@Valid @RequestBody Requisicoes.Pergunta req) {
        Simulacao plantada = simulador.simular(req.latitude(), req.longitude(), req.solo(), req.culturaPlantadaId(), req.hectares());
        Simulacao alternativa = req.culturaTesteId() == null || req.culturaTesteId().isBlank()
                ? null
                : simulador.simular(req.latitude(), req.longitude(), req.solo(), req.culturaTesteId(), req.hectares());
        List<ItemRanking> ranking = recomendacao.recomendar(req.latitude(), req.longitude(), req.solo());

        RespostaAgente resposta = agente.responder(req.pergunta(), new ContextoAgente(plantada, alternativa, ranking));
        return new Requisicoes.Resposta(resposta.texto(), resposta.demonstracao());
    }
}
