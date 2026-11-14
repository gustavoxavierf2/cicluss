package br.com.fieldshift.api;

import br.com.fieldshift.core.agente.Agente;
import br.com.fieldshift.core.agente.AgenteDemonstracao;
import br.com.fieldshift.core.agente.ContextoAgente;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.ItemRanking;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.servico.CulturaRepository;
import br.com.fieldshift.core.servico.RecomendacaoService;
import br.com.fieldshift.core.servico.SimuladorService;
import jakarta.validation.Valid;
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
    private final Agente agente;

    public SimuladorController(CulturaRepository culturas, SimuladorService simulador,
                               RecomendacaoService recomendacao, Agente agente) {
        this.culturas = culturas;
        this.simulador = simulador;
        this.recomendacao = recomendacao;
        this.agente = agente;
    }

    /** Lista de culturas para os selects do front. */
    @GetMapping("/culturas")
    public List<Cultura> culturas() {
        return culturas.todas();
    }

    /** Linha do tempo de uma cultura no terreno: 15 anos para trás e 15 para frente. */
    @PostMapping("/simulacoes")
    public Simulacao simular(@Valid @RequestBody Requisicoes.Simulacao req) {
        return simulador.simular(req.latitude(), req.longitude(), req.solo(), req.culturaId());
    }

    /** Ranking das culturas mais aptas para o terreno. */
    @PostMapping("/recomendacoes")
    public List<ItemRanking> recomendar(@Valid @RequestBody Requisicoes.Recomendacao req) {
        return recomendacao.recomendar(req.latitude(), req.longitude(), req.solo());
    }

    /** Pergunta ao agente sobre os resultados daquele terreno. */
    @PostMapping("/agente")
    public Requisicoes.Resposta perguntar(@Valid @RequestBody Requisicoes.Pergunta req) {
        Simulacao plantada = simulador.simular(req.latitude(), req.longitude(), req.solo(), req.culturaPlantadaId());
        Simulacao alternativa = req.culturaTesteId() == null || req.culturaTesteId().isBlank()
                ? null
                : simulador.simular(req.latitude(), req.longitude(), req.solo(), req.culturaTesteId());
        List<ItemRanking> ranking = recomendacao.recomendar(req.latitude(), req.longitude(), req.solo());

        String resposta = agente.responder(req.pergunta(), new ContextoAgente(plantada, alternativa, ranking));
        return new Requisicoes.Resposta(resposta, agente instanceof AgenteDemonstracao);
    }
}
