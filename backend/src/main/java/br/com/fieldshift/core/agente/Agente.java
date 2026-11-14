package br.com.fieldshift.core.agente;

/**
 * Explica os resultados em linguagem simples. {@link AgenteAnthropic} usa IA;
 * {@link AgenteDemonstracao} usa regras fixas. {@link AgenteComReserva} escolhe entre os dois.
 */
public interface Agente {

    String responder(String pergunta, ContextoAgente contexto);
}
