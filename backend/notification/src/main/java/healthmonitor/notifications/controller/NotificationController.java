package healthmonitor.notifications.controller;

import healthmonitor.notifications.model.AlertDto;
import healthmonitor.notifications.model.MarkAsReadRequest;
import healthmonitor.notifications.sevice.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AlertService alertService;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasRole('ROLE_PATIENT') or hasRole('ROLE_DOCTOR')")
    public ResponseEntity<?> getNotificationsForPatient(@PathVariable String patientId, @RequestParam String userId) {
        return ResponseEntity.ok().body(alertService.getNotificationsForPatient(patientId, userId));
    }

    @GetMapping("/all/{patientId}")
    @PreAuthorize("hasRole('ROLE_PATIENT') or hasRole('ROLE_DOCTOR')")
    public ResponseEntity<Page<AlertDto>> getAllNotifications(@PathVariable String patientId,
                                                              @RequestParam String userId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok().body(alertService.getAllNotifications(patientId, userId, page, size));
    }

    @PutMapping("/mark-as-read")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<Void> markAsRead(@RequestBody MarkAsReadRequest ids, @RequestParam String userId) {
        alertService.markAsRead(ids.ids(), userId);
        return ResponseEntity.ok().build();
    }
}
