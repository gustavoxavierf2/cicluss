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

@SpringBootTest
@AutoConfigureMockMvc
class SimuladorControllerTest {

    @Autowired
    MockMvc mvc;

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
    void agenteResponde() throws Exception {
        mvc.perform(post("/api/agente").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pergunta\":\"qual a melhor?\",\"latitude\":-14.866,\"longitude\":-40.839,"
                                + "\"solo\":\"MISTO\",\"culturaPlantadaId\":\"milho\",\"culturaTesteId\":\"feijao\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demonstracao").value(true));
    }
}
