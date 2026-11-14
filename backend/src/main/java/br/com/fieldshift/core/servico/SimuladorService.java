package br.com.fieldshift.core.servico;

import br.com.fieldshift.core.clima.ClimaProvider;
import br.com.fieldshift.core.modelo.ClimaAnual;
import br.com.fieldshift.core.modelo.Cultura;
import br.com.fieldshift.core.modelo.ResultadoAno;
import br.com.fieldshift.core.modelo.Resumo;
import br.com.fieldshift.core.modelo.Simulacao;
import br.com.fieldshift.core.modelo.TipoSolo;

import java.time.Clock;
import java.time.Year;
import java.util.List;

/** Monta a linha do tempo: N anos para trás (dado real) e N para frente (projeção). */
public class SimuladorService {

    public static final int ANOS_PARA_CADA_LADO = 15;

    private final ClimaProvider clima;
    private final CulturaRepository culturas;
    private final AptidaoCalculadora calculadora;
    private final Clock relogio;

    public SimuladorService(ClimaProvider clima, CulturaRepository culturas,
                            AptidaoCalculadora calculadora, Clock relogio) {
        this.clima = clima;
        this.culturas = culturas;
        this.calculadora = calculadora;
        this.relogio = relogio;
    }

    /** Último ano completo com dado observado. */
    public int anoAtual() {
        return Year.now(relogio).getValue() - 1;
    }

    public Simulacao simular(double latitude, double longitude, TipoSolo solo, String culturaId) {
        validarCoordenada(latitude, longitude);
        Cultura cultura = culturas.obter(culturaId);
        int anoAtual = anoAtual();
        List<ClimaAnual> serie = clima.serieAnual(latitude, longitude,
                anoAtual - ANOS_PARA_CADA_LADO + 1, anoAtual + ANOS_PARA_CADA_LADO, anoAtual);

        List<ResultadoAno> anos = serie.stream()
                .map(c -> calculadora.avaliar(cultura, c, solo))
                .toList();
        Resumo passado = Resumo.de(anos.stream().filter(a -> !a.projetado()).toList());
        Resumo futuro = Resumo.de(anos.stream().filter(ResultadoAno::projetado).toList());
        return new Simulacao(cultura, latitude, longitude, solo, anoAtual, anos, passado, futuro);
    }

    static void validarCoordenada(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenada fora do mapa: " + latitude + ", " + longitude);
        }
    }
}
