package org.lf10.stimmungsumfrage.Controllers;


import org.junit.jupiter.api.Test;
import org.lf10.stimmungsumfrage.Models.EmployeeFeedback;
import org.lf10.stimmungsumfrage.service.CrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrudController.class)
class CrudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CrudService crudService;

    private static EmployeeFeedback feedback(Long id, long reactionId, String text) {
        EmployeeFeedback f = new EmployeeFeedback();
        f.setId(id);
        f.setReaction_id(reactionId);
        f.setFeedback_text(text);
        return f;
    }

    @Test
    void addFeedback_returnsCreatedFeedback() throws Exception {
        EmployeeFeedback input = feedback(null, 1L, "Super");
        EmployeeFeedback created = feedback(1L, 1L, "Super");
        when(crudService.create(any(EmployeeFeedback.class))).thenReturn(created);

        String json = objectMapper.writeValueAsString(input);
        mockMvc.perform(post("/feedback/addFeedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.feedback_text").value("Super"));

        verify(crudService).create(any(EmployeeFeedback.class));
    }

    @Test
    void getFeedbackById_returnsFeedback_whenFound() throws Exception {
        EmployeeFeedback f = feedback(1L, 2L, "Gut");
        when(crudService.getById("1")).thenReturn(f);

        mockMvc.perform(get("/feedback/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.feedback_text").value("Gut"));
    }

    @Test
    void getFeedbackById_returns404_whenNotFound() throws Exception {
        when(crudService.getById("999")).thenReturn(null);

        mockMvc.perform(get("/feedback/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFeedbackAll_returnsList() throws Exception {
        List<EmployeeFeedback> list = List.of(
                feedback(1L, 1L, "A"),
                feedback(2L, 2L, "B")
        );
        when(crudService.getAll()).thenReturn(list);

        mockMvc.perform(get("/feedback/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].feedback_text").value("A"))
                .andExpect(jsonPath("$[1].feedback_text").value("B"));
    }

    @Test
    void updateFeedback_returnsUpdatedFeedback() throws Exception {
        EmployeeFeedback updated = feedback(1L, 2L, "Aktualisiert");
        when(crudService.update(any(EmployeeFeedback.class))).thenReturn(updated);

        String json = objectMapper.writeValueAsString(updated);
        mockMvc.perform(put("/feedback/updateFeedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback_text").value("Aktualisiert"));

        verify(crudService).update(any(EmployeeFeedback.class));
    }

    @Test
    void delete_returnsOk_whenFeedbackExists() throws Exception {
        EmployeeFeedback f = feedback(1L, 1L, "Löschen");
        when(crudService.getById("1")).thenReturn(f);
        doNothing().when(crudService).delete("1");

        mockMvc.perform(delete("/feedback/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(crudService).delete("1");
    }

    @Test
    void delete_returns404_whenFeedbackNotExists() throws Exception {
        when(crudService.getById("999")).thenReturn(null);

        mockMvc.perform(delete("/feedback/delete/999"))
                .andExpect(status().isNotFound());

        verify(crudService, never()).delete(anyString());
    }
}
