package healthmonitor.notifications.controller;

import healthmonitor.notifications.sevice.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AlertService alertService;

    @GetMapping("/{patientId}")
    public ResponseEntity<?> getNotificationsForPatient(@PathVariable String patientId) {
        return ResponseEntity.ok().body(alertService.getNotificationsForPatient(patientId));
    }

    @GetMapping("/all/{patientId}")
    public ResponseEntity<?> getAllNotifications(@PathVariable String patientId) {
        return ResponseEntity.ok().body(alertService.getAllNotifications(patientId));
    }
}
