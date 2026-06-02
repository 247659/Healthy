package healthmonitor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/medical-staff")
public class MedicalStaffController {

    @GetMapping("/hello")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Hello World");
    }
}
