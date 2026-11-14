package br.com.fieldshift.core.agente;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Escolhe quem responde: o agente de IA quando ele existe e responde a tempo; senão o
 * agente reserva (demonstração). A resposta diz qual dos dois foi usado.
 */
public class AgenteComReserva {

    public static final Duration TEMPO_LIMITE_PADRAO = Duration.ofSeconds(20);

    private static final System.Logger LOG = System.getLogger(AgenteComReserva.class.getName());

    private final Agente ia;
    private final Agente reserva;
    private final Duration tempoLimite;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * @param ia          agente de IA; null quando não há chave configurada
     * @param reserva     agente usado quando a IA não existe, falha ou demora demais
     * @param tempoLimite quanto esperar pela IA antes de cair na reserva
     */
    public AgenteComReserva(Agente ia, Agente reserva, Duration tempoLimite) {
        this.ia = ia;
        this.reserva = reserva;
        this.tempoLimite = tempoLimite;
    }

    /**
     * Monta a escolha a partir da configuração: com chave da API, tenta a IA primeiro;
     * sem chave, responde sempre pelo modo demonstração.
     */
    public static AgenteComReserva deConfiguracao(String chave, String modelo, boolean dadosDeDemonstracao) {
        Agente ia = chave == null || chave.isBlank()
                ? null
                : new AgenteAnthropic(chave.strip(), modelo, dadosDeDemonstracao, TEMPO_LIMITE_PADRAO);
        return new AgenteComReserva(ia, new AgenteDemonstracao(), TEMPO_LIMITE_PADRAO);
    }

    public boolean temIa() {
        return ia != null;
    }

    public RespostaAgente responder(String pergunta, ContextoAgente contexto) {
        if (ia != null) {
            Future<String> tarefa = executor.submit(() -> ia.responder(pergunta, contexto));
            try {
                String texto = tarefa.get(tempoLimite.toMillis(), TimeUnit.MILLISECONDS);
                if (texto != null && !texto.isBlank()) {
                    return new RespostaAgente(texto.strip(), false);
                }
                LOG.log(System.Logger.Level.WARNING, "Agente de IA devolveu resposta vazia; usando o modo demonstração.");
            } catch (TimeoutException e) {
                tarefa.cancel(true);
                LOG.log(System.Logger.Level.WARNING, "Agente de IA demorou mais de {0} s; usando o modo demonstração.",
                        tempoLimite.toSeconds());
            } catch (ExecutionException e) {
                LOG.log(System.Logger.Level.WARNING, "Agente de IA falhou ({0}); usando o modo demonstração.",
                        String.valueOf(e.getCause()));
            } catch (InterruptedException e) {
                tarefa.cancel(true);
                Thread.currentThread().interrupt();
            }
        }
        return new RespostaAgente(reserva.responder(pergunta, contexto), true);
    }
}
