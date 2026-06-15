package healthmonitor.controller;

import healthmonitor.model.dto.LoginRequestDto;
import healthmonitor.model.dto.LogoutRequestDto;
import healthmonitor.model.dto.PatientRegistrationDto;
import healthmonitor.model.dto.RefreshTokenRequestDto;
import healthmonitor.model.dto.TokenResponseDto;
import healthmonitor.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
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
}
