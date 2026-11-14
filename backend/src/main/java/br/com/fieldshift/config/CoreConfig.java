package br.com.fieldshift.config;

import br.com.fieldshift.core.agente.Agente;
import br.com.fieldshift.core.agente.AgenteDemonstracao;
import br.com.fieldshift.core.clima.ClimaProvider;
import br.com.fieldshift.core.clima.MockClimaProvider;
import br.com.fieldshift.core.servico.AptidaoCalculadora;
import br.com.fieldshift.core.servico.CulturaRepository;
import br.com.fieldshift.core.servico.RecomendacaoService;
import br.com.fieldshift.core.servico.SimuladorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Clock;

/**
 * Liga as peças do núcleo (pacote core, que é Java puro). Para trocar os dados falsos
 * pelos da NASA, basta trocar o bean de {@link ClimaProvider}.
 */
@Configuration
public class CoreConfig {

    @Bean
    public ClimaProvider climaProvider() {
        return new MockClimaProvider();
    }

    @Bean
    public CulturaRepository culturaRepository(ObjectMapper mapper) throws IOException {
        return CulturaRepository.deJson(new ClassPathResource("culturas.json").getInputStream(), mapper);
    }

    @Bean
    public AptidaoCalculadora aptidaoCalculadora() {
        return new AptidaoCalculadora();
    }

    @Bean
    public SimuladorService simuladorService(ClimaProvider clima, CulturaRepository culturas,
                                             AptidaoCalculadora calculadora) {
        return new SimuladorService(clima, culturas, calculadora, Clock.systemDefaultZone());
    }

    @Bean
    public RecomendacaoService recomendacaoService(SimuladorService simulador, CulturaRepository culturas) {
        return new RecomendacaoService(simulador, culturas);
    }

    @Bean
    public Agente agente() {
        return new AgenteDemonstracao();
    }
}
