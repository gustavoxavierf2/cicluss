package br.com.fieldshift.core.agente;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;

import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Agente com IA de verdade: manda a pergunta e os números do {@link ContextoAgente} para a
 * API da Anthropic (Messages API) e devolve a explicação em linguagem simples.
 * Ele não calcula nada; só explica os números que recebeu.
 *
 * Qualquer falha vira exceção; quem decide cair no modo demonstração é o {@link AgenteComReserva}.
 */
public class AgenteAnthropic implements Agente {

    static final String INSTRUCOES = """
            Você é o assistente do Ciclus, um site que mostra ao pequeno agricultor do Sudoeste da Bahia \
            como cada cultura teria se saído no terreno dele nos últimos 15 anos e como deve se sair nos \
            próximos 15. Quem fala com você é um agricultor leigo: ele não lê gráfico técnico nem conhece \
            termos de agronomia ou de clima.

            Em cada mensagem você recebe os números já calculados pelo sistema para o terreno dele e, \
            no fim, a pergunta. Seu trabalho é só explicar esses números.

            Como responder:
            - Em português simples, do jeito que se conversa na roça, em no máximo 4 frases curtas. \
            Texto corrido, sem listas, sem títulos e sem negrito.
            - Use somente os números e fatos que estão na mensagem. Não invente dados, não faça contas \
            novas de aptidão e não traga informação de fora. Se a resposta não estiver nos números \
            recebidos, diga que o sistema não tem essa informação.
            - Fale "bom", "de atenção" ou "ruim" em vez de termos técnicos. Evite palavras como \
            "evapotranspiração" ou "projeção"; prefira "água que a planta perde" e "previsão".
            - Sempre que falar de um ano marcado como previsão, deixe claro que é previsão e que pode \
            não acontecer assim.
            - Não fale de preço, venda, mercado ou lucro: o sistema só considera clima e solo. Se \
            perguntarem sobre isso, explique essa limitação em uma frase.
            """;

    static final String AVISO_DEMONSTRACAO = """

            Importante: por enquanto os números de clima deste site são de demonstração, ainda não são \
            os dados reais da NASA. Avise isso ao agricultor em uma frase curta na sua resposta.
            """;

    private final AnthropicClient cliente;
    private final String modelo;
    private final boolean dadosDeDemonstracao;

    /**
     * @param chave               chave da API (vem da variável de ambiente ANTHROPIC_API_KEY, só no backend)
     * @param modelo              id do modelo, ex.: claude-sonnet-5-5
     * @param dadosDeDemonstracao true enquanto o clima vem do MockClimaProvider
     * @param tempoLimite         tempo máximo da chamada HTTP
     */
    public AgenteAnthropic(String chave, String modelo, boolean dadosDeDemonstracao, Duration tempoLimite) {
        this.cliente = AnthropicOkHttpClient.builder()
                .apiKey(chave)
                .timeout(tempoLimite)
                .maxRetries(0)
                .build();
        this.modelo = modelo;
        this.dadosDeDemonstracao = dadosDeDemonstracao;
    }

    @Override
    public String responder(String pergunta, ContextoAgente contexto) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(4000L)
                .system(instrucoes(dadosDeDemonstracao))
                // Resposta curta de chat: esforço baixo para responder rápido.
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage(mensagem(pergunta, contexto))
                .build();

        Message resposta = cliente.messages().create(params);
        String texto = resposta.content().stream()
                .flatMap(bloco -> bloco.text().stream())
                .map(t -> t.text())
                .collect(Collectors.joining())
                .strip();
        if (texto.isEmpty()) {
            throw new IllegalStateException("Modelo não devolveu texto (stop_reason: "
                    + resposta.stopReason().map(Object::toString).orElse("?") + ")");
        }
        return texto;
    }

    static String instrucoes(boolean dadosDeDemonstracao) {
        return dadosDeDemonstracao ? INSTRUCOES + AVISO_DEMONSTRACAO : INSTRUCOES;
    }

    static String mensagem(String pergunta, ContextoAgente contexto) {
        return "<numeros_do_sistema>\n" + ContextoEmTexto.de(contexto) + "</numeros_do_sistema>\n\n"
                + "Pergunta do agricultor: " + (pergunta == null ? "" : pergunta.strip());
    }
}
