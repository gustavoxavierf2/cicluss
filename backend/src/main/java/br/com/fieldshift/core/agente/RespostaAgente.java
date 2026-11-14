package br.com.fieldshift.core.agente;

/**
 * Resposta do agente e quem respondeu.
 *
 * @param demonstracao true quando veio do {@link AgenteDemonstracao} (regras fixas), false quando veio da IA
 */
public record RespostaAgente(String texto, boolean demonstracao) {
}
