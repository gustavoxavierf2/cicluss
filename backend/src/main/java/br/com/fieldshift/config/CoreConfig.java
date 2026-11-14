package br.com.fieldshift.config;

import br.com.fieldshift.core.agente.AgenteComReserva;
import br.com.fieldshift.core.clima.ClimaProvider;
import br.com.fieldshift.core.clima.HttpClimaProvider;
import br.com.fieldshift.core.clima.MockClimaProvider;
import br.com.fieldshift.core.servico.AptidaoCalculadora;
import br.com.fieldshift.core.servico.CulturaRepository;
import br.com.fieldshift.core.servico.RecomendacaoService;
import br.com.fieldshift.core.servico.SimuladorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger LOG = LoggerFactory.getLogger(CoreConfig.class);

    /**
     * fieldshift.clima.fonte=mock  → dados falsos (padrão)
     * fieldshift.clima.fonte=http  → serviço do pessoal dos dados em fieldshift.clima.url
     */
    @Bean
    public ClimaProvider climaProvider(@Value("${fieldshift.clima.fonte:mock}") String fonte,
                                       @Value("${fieldshift.clima.url:}") String url,
                                       ObjectMapper mapper) {
        return switch (fonte) {
            case "mock" -> new MockClimaProvider();
            case "http" -> {
                if (url.isBlank()) {
                    throw new IllegalStateException("fieldshift.clima.fonte=http exige fieldshift.clima.url");
                }
                yield new HttpClimaProvider(url, mapper);
            }
            default -> throw new IllegalStateException("fieldshift.clima.fonte desconhecida: " + fonte);
        };
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

    /**
     * Com fieldshift.agente.chave (variável de ambiente ANTHROPIC_API_KEY) o chat usa a IA;
     * sem ela, ou se a IA falhar ou demorar, responde o agente de demonstração.
     */
    @Bean
    public AgenteComReserva agente(@Value("${fieldshift.agente.chave:}") String chave,
                                   @Value("${fieldshift.agente.modelo:claude-sonnet-5-5}") String modelo,
                                   @Value("${fieldshift.clima.fonte:mock}") String fonteClima) {
        AgenteComReserva agente = AgenteComReserva.deConfiguracao(chave, modelo, "mock".equals(fonteClima));
        LOG.info(agente.temIa()
                ? "Agente: IA ligada (modelo " + modelo + ")."
                : "Agente: modo demonstração (ANTHROPIC_API_KEY não definida).");
        return agente;
    }
}
