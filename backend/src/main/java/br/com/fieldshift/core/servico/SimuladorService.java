package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.clima.ClimaProvider;
import br.com.fieldshift.core.modelo.Cenario;
import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Resumo;
import br.com.fieldshift.core.modelo.Safra;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.TipoSolo;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Objects;

/** Monta a linha do tempo: N anos para trás (dado real) e N para frente (projeção). */
public class SimuladorService {

    public static final int ANOS_PARA_CADA_LADO = 15;

    private final ClimaProvider clima;
    private final CulturaRepository culturas;
    private final AptidaoCalculadora calculadora;
    private final SafraCalculadora safra;
    private final Clock relogio;

    public SimuladorService(ClimaProvider clima, CulturaRepository culturas,
                            AptidaoCalculadora calculadora, Clock relogio) {
        this.clima = clima;
        this.culturas = culturas;
        this.calculadora = calculadora;
        this.safra = new SafraCalculadora();
        this.relogio = relogio;
    }

    /** Último ano completo com dado observado. */
    public int anoAtual() {
        return Year.now(relogio).getValue() - 1;
    }

    public Simulacao simular(double latitude, double longitude, TipoSolo solo, String culturaId) {
        return simular(latitude, longitude, solo, culturaId, null);
    }

    /** @param hectares área do terreno; com ela a simulação traz a estimativa de safra de cada ano (pode ser null) */
    public Simulacao simular(double latitude, double longitude, TipoSolo solo, String culturaId, Double hectares) {
        validarCoordenada(latitude, longitude);
        Cultura cultura = culturas.obter(culturaId);
        int anoAtual = anoAtual();
        List<ClimaAnual> serie = serie(latitude, longitude, anoAtual);

        List<ResultadoAno> anos = serie.stream()
                .map(c -> calculadora.avaliar(cultura, c, solo))
                .toList();
        Resumo passado = Resumo.de(anos.stream().filter(a -> !a.projetado()).toList());
        Resumo futuro = Resumo.de(anos.stream().filter(ResultadoAno::projetado).toList());
        List<Safra> safras = hectares == null
                ? List.of()
                : anos.stream().map(a -> safra.estimar(cultura, a, hectares)).filter(Objects::nonNull).toList();
        return new Simulacao(cultura, latitude, longitude, solo, anoAtual, anos, passado, futuro, hectares, safras);
    }

    /**
     * "E se o clima fosse assim?": pega o clima do ano base e troca os valores que o agricultor ajustou.
     * A nota sai da mesma regra de sempre ({@link AptidaoCalculadora}).
     *
     * @param temperaturaMedia °C; null mantém a do ano base
     * @param precipitacao     chuva do ano em mm; null mantém a do ano base
     * @param umidadeSolo      de 0 (seco) a 1 (encharcado); null mantém a do ano base
     */
    public Cenario cenario(double latitude, double longitude, TipoSolo solo, String culturaId, Double hectares,
                           int anoBase, Double temperaturaMedia, Double precipitacao, Double umidadeSolo) {
        validarCoordenada(latitude, longitude);
        Cultura cultura = culturas.obter(culturaId);
        ClimaAnual base = serie(latitude, longitude, anoAtual()).stream()
                .filter(c -> c.ano() == anoBase)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ano fora do intervalo: " + anoBase));

        if (temperaturaMedia != null && (temperaturaMedia < -10 || temperaturaMedia > 50)) {
            throw new IllegalArgumentException("Temperatura fora do intervalo (-10 a 50 °C)");
        }
        if (precipitacao != null && (precipitacao < 0 || precipitacao > 6000)) {
            throw new IllegalArgumentException("Chuva fora do intervalo (0 a 6000 mm)");
        }
        if (umidadeSolo != null && (umidadeSolo < 0 || umidadeSolo > 1)) {
            throw new IllegalArgumentException("Umidade do solo fora do intervalo (0 a 1)");
        }

        double temperatura = temperaturaMedia == null ? base.temperaturaMedia() : temperaturaMedia;
        double diferenca = temperatura - base.temperaturaMedia();
        ClimaAnual ajustado = new ClimaAnual(base.ano(), base.projetado(), temperatura,
                base.temperaturaMaxima() + diferenca, base.temperaturaMinima() + diferenca,
                precipitacao == null ? base.precipitacao() : precipitacao,
                umidadeSolo == null ? base.umidadeSolo() : umidadeSolo,
                base.evapotranspiracao());

        ResultadoAno resultado = calculadora.avaliar(cultura, ajustado, solo);
        return new Cenario(cultura, resultado, hectares == null ? null : safra.estimar(cultura, resultado, hectares));
    }

    private List<ClimaAnual> serie(double latitude, double longitude, int anoAtual) {
        return clima.serieAnual(latitude, longitude,
                anoAtual - ANOS_PARA_CADA_LADO + 1, anoAtual + ANOS_PARA_CADA_LADO, anoAtual);
    }

    static void validarCoordenada(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenada fora do mapa: " + latitude + ", " + longitude);
        }
    }
}
