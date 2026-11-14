package br.com.fieldshift.core.modelo;

/**
 * Tipo de terra estimado para um ponto do mapa a partir de um mapa de solos.
 *
 * @param argila % de argila na camada de 0 a 30 cm
 * @param areia  % de areia
 * @param silte  % de silte
 */
public record SoloEstimado(TipoSolo tipo, double argila, double areia, double silte, String fonte) {
}
