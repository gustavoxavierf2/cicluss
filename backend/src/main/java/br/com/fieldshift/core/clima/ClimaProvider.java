package br.com.fieldshift.core.clima;

import br.com.fieldshift.core.modelo.ClimaAnual;

import java.util.List;

/**
 * De onde vem o clima de um ponto do mapa. Hoje é o {@link MockClimaProvider};
 * quando o pessoal dos dados entregar, entra uma implementação que lê os dados da NASA
 * (NASA POWER / SMAP para o passado, projeção para o futuro) sem mudar o resto do sistema.
 */
public interface ClimaProvider {

    /**
     * Série anual de anoInicio a anoFim (inclusive), em ordem.
     * Anos maiores que {@code anoAtual} devem vir com {@code projetado = true}.
     */
    List<ClimaAnual> serieAnual(double latitude, double longitude, int anoInicio, int anoFim, int anoAtual);
}
