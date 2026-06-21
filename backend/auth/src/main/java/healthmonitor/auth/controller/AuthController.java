package healthmonitor.auth.controller;

import healthmonitor.auth.model.dto.LoginRequestDto;
import healthmonitor.auth.model.dto.LogoutRequestDto;
import healthmonitor.auth.model.dto.PatientRegistrationDto;
import healthmonitor.auth.model.dto.RefreshTokenRequestDto;
import healthmonitor.auth.model.dto.TokenResponseDto;
import healthmonitor.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/patient")
    public ResponseEntity<TokenResponseDto> registerPatient(@Valid @RequestBody PatientRegistrationDto patientDto) {
        authService.registerPatient(patientDto);
        TokenResponseDto login = authService.login(new LoginRequestDto(patientDto.getEmail(), patientDto.getPassword()));
        return ResponseEntity.status(HttpStatus.CREATED).body(login);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody LoginRequestDto dto) {
        TokenResponseDto tokens = authService.login(dto);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/loginDoctor")
    public ResponseEntity<TokenResponseDto> loginDoctor(@RequestBody LoginRequestDto dto) {
        TokenResponseDto tokens = authService.loginDoctor(dto);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequestDto dto) {
        authService.logout(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refreshToken(@RequestBody RefreshTokenRequestDto dto) {
        TokenResponseDto newTokens = authService.refresh(dto);
        return ResponseEntity.ok(newTokens);
    }

    @PutMapping("/users/{userId}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        String newPassword = request.get("password");
        authService.changePassword(userId, newPassword);

        return ResponseEntity.ok().build();
    }
}
