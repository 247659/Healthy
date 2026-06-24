package healthmonitor.fallback;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/medical-staff")
    public ResponseEntity<?> medicalStaffFallback() {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
