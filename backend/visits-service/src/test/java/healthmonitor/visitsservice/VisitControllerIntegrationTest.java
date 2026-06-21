package healthmonitor.visitsservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.visitsservice.visits.controller.VisitController;
import healthmonitor.visitsservice.visits.payload.request.VisitRequest;
import healthmonitor.visitsservice.visits.payload.response.VisitResponse;
import healthmonitor.visitsservice.visits.service.VisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisitController.class)
class VisitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VisitService visitService;

    @Test
    void getAll_ShouldReturnList() throws Exception {
        when(visitService.getAll()).thenReturn(List.of());
        mockMvc.perform(get("/visits")).andExpect(status().isOk());
    }

    @Test
    void getById_ShouldReturnVisit() throws Exception {
        UUID id = UUID.randomUUID();
        when(visitService.getById(id)).thenReturn(new VisitResponse(id, UUID.randomUUID(), "p", LocalDateTime.now(), 30, "n", LocalDateTime.now()));

        mockMvc.perform(get("/visits/" + id)).andExpect(status().isOk());
    }

    @Test
    void save_ShouldReturnVisit() throws Exception {
        VisitRequest req = new VisitRequest(UUID.randomUUID(), "p", LocalDateTime.now(), 30, "n");
        when(visitService.save(any())).thenReturn(new VisitResponse(UUID.randomUUID(), req.medicalStaffId(), "p", LocalDateTime.now(), 30, "n", LocalDateTime.now()));

        mockMvc.perform(post("/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_ShouldReturnNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/visits/" + id)).andExpect(status().isNoContent());
        verify(visitService).delete(id);
    }

    @Test
    void update_ShouldReturnUpdatedVisit() throws Exception {
        UUID id = UUID.randomUUID();
        VisitRequest req = new VisitRequest(UUID.randomUUID(), "p", LocalDateTime.now(), 30, "n");
        when(visitService.update(eq(id), any())).thenReturn(new VisitResponse(id, req.medicalStaffId(), "p", LocalDateTime.now(), 30, "n", LocalDateTime.now()));

        mockMvc.perform(put("/visits/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}