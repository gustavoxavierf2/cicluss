package br.com.fieldshift.core.modelo;

/** Resultado de um "e se o clima fosse assim?" para uma cultura. safra é null sem hectares ou sem valores. */
public record Cenario(Cultura cultura, ResultadoAno resultado, Safra safra) {
}
