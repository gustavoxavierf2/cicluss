package br.com.fieldshift.core.agente;

/**
 * Explica os resultados em linguagem simples. Hoje: {@link AgenteDemonstracao} (regras fixas).
 * Depois: implementação com LLM que recebe o mesmo {@link ContextoAgente} no prompt.
 */
public interface Agente {

    String responder(String pergunta, ContextoAgente contexto);
}
