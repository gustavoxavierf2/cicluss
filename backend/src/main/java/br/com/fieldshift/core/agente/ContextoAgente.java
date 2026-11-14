package br.com.fieldshift.core.agente;

import br.com.fieldshift.core.modelo.ItemRanking;
import br.com.fieldshift.core.modelo.Simulacao;

import java.util.List;

/**
 * Tudo o que o agente sabe para responder. Ele só explica estes números; não calcula nada.
 *
 * @param plantada    simulação da cultura que o agricultor plantou por último
 * @param alternativa simulação da cultura que ele quer testar (pode ser null)
 */
public record ContextoAgente(Simulacao plantada, Simulacao alternativa, List<ItemRanking> ranking) {
}
