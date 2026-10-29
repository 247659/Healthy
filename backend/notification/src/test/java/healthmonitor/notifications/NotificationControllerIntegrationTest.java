package healthmonitor.notifications;

import healthmonitor.notifications.config.SecurityConfig;
import healthmonitor.notifications.controller.NotificationController;
import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.sevice.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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

        when(alertService.getNotificationsForPatient(patientId, "id")).thenReturn(responses);

        mockMvc.perform(get("/notifications/{patientId}", patientId)
                        .param("userId", "id")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].alertId").value(alertId.toString()))
                .andExpect(jsonPath("$[0].patientId").value(patientId))
                .andExpect(jsonPath("$[0].message").value("High heart rate"))
                .andExpect(jsonPath("$[0].read").value(false));

        verify(alertService, times(1)).getNotificationsForPatient(patientId, "id");
    }

    @Test
    void getAllNotifications_ShouldReturnAllAlerts() throws Exception {
        String patientId = "pat-1";
        UUID alertId1 = UUID.randomUUID();
        UUID alertId2 = UUID.randomUUID();

        AlertDto dto1 = AlertDto.builder()
                .alertId(String.valueOf(alertId1))
                .patientId(patientId)
                .isRead(false)
                .message("First unread alert")
                .build();

        AlertDto dto2 = AlertDto.builder()
                .alertId(String.valueOf(alertId2))
                .patientId(patientId)
                .isRead(true)
                .message("Second read alert")
                .build();

        Page<AlertDto> responses = new PageImpl<>(List.of(dto1, dto2));

        when(alertService.getAllNotifications(patientId, "id", 0, 10)).thenReturn(responses);

        mockMvc.perform(get("/notifications/all/{patientId}", patientId)
                        .param("userId", "id")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.size()").value(2))
                .andExpect(jsonPath("$.content[0].alertId").value(alertId1.toString()))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[1].alertId").value(alertId2.toString()))
                .andExpect(jsonPath("$.content[1].read").value(true));

        verify(alertService, times(1)).getAllNotifications(patientId, "id", 0, 10);
    }

    @Test
    void markAsRead_ShouldMarkNotificationsAsRead() throws Exception {
        String json = "{\"ids\":[\"alert-1\",\"alert-2\"]}";

        doNothing().when(alertService).markAsRead(anyList(), eq("id"));

        mockMvc.perform(put("/notifications/mark-as-read")
                        .param("userId", "id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))))
                .andExpect(status().isOk());

        verify(alertService, times(1)).markAsRead(eq(List.of("alert-1", "alert-2")), eq("id"));
    }
}