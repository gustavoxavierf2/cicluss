package br.com.fieldshift.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem chave e com clima falso: os testes nunca chamam a Anthropic nem a NASA de verdade.
@SpringBootTest(properties = {"fieldshift.agente.chave=", "fieldshift.clima.fonte=mock"})
@AutoConfigureMockMvc
class SimuladorControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void infoDizAFonteDoClima() throws Exception {
        mvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonteClima").value("mock"))
                .andExpect(jsonPath("$.climaDeDemonstracao").value(true));
    }

    @Test
    void listaCulturas() throws Exception {
        mvc.perform(get("/api/culturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("milho"));
    }

    @Test
    void simula30Anos() throws Exception {
        mvc.perform(post("/api/simulacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-14.866,\"longitude\":-40.839,\"solo\":\"MISTO\",\"culturaId\":\"milho\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anos.length()").value(30));
    }

    @Test
    void culturaInexistenteDa400() throws Exception {
        mvc.perform(post("/api/simulacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-14.866,\"longitude\":-40.839,\"culturaId\":\"cacau\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void semCoordenadaDa400() throws Exception {
        mvc.perform(post("/api/simulacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"culturaId\":\"milho\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulacaoComHectaresTrazSafra() throws Exception {
        mvc.perform(post("/api/simulacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-14.866,\"longitude\":-40.839,\"solo\":\"MISTO\",\"culturaId\":\"milho\",\"hectares\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safras.length()").value(30))
                .andExpect(jsonPath("$.safras[0].custo").value(7000));
    }

    @Test
    void cenarioComClimaAjustado() throws Exception {
        mvc.perform(post("/api/cenarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-14.866,\"longitude\":-40.839,\"solo\":\"MISTO\",\"culturaId\":\"milho\","
                                + "\"hectares\":1,\"anoBase\":2025,\"temperaturaMedia\":24,\"precipitacao\":900,\"umidadeSolo\":0.4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado.nota").value(100))
                .andExpect(jsonPath("$.safra.producaoKg").value(5000));
    }

    @Test
    void planejamentoDe3Anos() throws Exception {
        mvc.perform(post("/api/planejamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-14.866,\"longitude\":-40.839,\"solo\":\"MISTO\",\"hectares\":2,"
                                + "\"culturaPrincipalId\":\"milho\",\"anos\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anos.length()").value(3))
                .andExpect(jsonPath("$.anos[0].cultura.id").value("milho"))
                .andExpect(jsonPath("$.anos[1].principal").value(false));
    }

    @Test
    void planejamentoDe2AnosDa400() throws Exception {
        mvc.perform(post("/api/planejamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-14.866,\"longitude\":-40.839,\"culturaPrincipalId\":\"milho\",\"anos\":2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agenteResponde() throws Exception {
        mvc.perform(post("/api/agente").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pergunta\":\"qual a melhor?\",\"latitude\":-14.866,\"longitude\":-40.839,"
                                + "\"solo\":\"MISTO\",\"culturaPlantadaId\":\"milho\",\"culturaTesteId\":\"feijao\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demonstracao").value(true));
    }
}
