package healthmonitor.notifications;

import healthmonitor.notifications.controller.NotificationController;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.sevice.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @Test
    void getNotificationsForPatient_ShouldReturnUnreadAlerts() throws Exception {
        String patientId = "pat-1";
        UUID alertId = UUID.randomUUID();
        List<AlertDto> responses = List.of(
                AlertDto.builder()
                        .alertId(String.valueOf(alertId))
                        .patientId(patientId)
                        .message("High heart rate")
                        .severity("CRITICAL")
                        .isRead(false)
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        when(alertService.getNotificationsForPatient(patientId)).thenReturn(responses);

        mockMvc.perform(get("/notifications/{patientId}", patientId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].alertId").value(alertId.toString()))
                .andExpect(jsonPath("$[0].patientId").value(patientId))
                .andExpect(jsonPath("$[0].message").value("High heart rate"))
                .andExpect(jsonPath("$[0].read").value(false));

        verify(alertService, times(1)).getNotificationsForPatient(patientId);
    }

    @Test
    void getAllNotifications_ShouldReturnAllAlerts() throws Exception {
        String patientId = "pat-1";
        UUID alertId1 = UUID.randomUUID();
        UUID alertId2 = UUID.randomUUID();
        List<AlertDto> responses = List.of(
                AlertDto.builder()
                        .alertId(String.valueOf(alertId1))
                        .patientId(patientId)
                        .isRead(false)
                        .message("First unread alert")
                        .build(),
                AlertDto.builder()
                        .alertId(String.valueOf(alertId2))
                        .patientId(patientId)
                        .isRead(true)
                        .message("Second read alert")
                        .build()
        );

        when(alertService.getAllNotifications(patientId)).thenReturn(responses);

        // When & Then
        mockMvc.perform(get("/notifications/all/{patientId}", patientId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].alertId").value(alertId1.toString()))
                .andExpect(jsonPath("$[0].read").value(false))
                .andExpect(jsonPath("$[1].alertId").value(alertId2.toString()))
                .andExpect(jsonPath("$[1].read").value(true));

        verify(alertService, times(1)).getAllNotifications(patientId);
    }
}