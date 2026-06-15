package healthmonitor.service;

import healthmonitor.model.LoginRequestDto;
import healthmonitor.model.LogoutRequestDto;
import healthmonitor.model.PatientRegistrationDto;
import healthmonitor.model.TokenResponseDto;

public interface AuthService {
    void registerPatient(PatientRegistrationDto patientDto);

    TokenResponseDto login(LoginRequestDto loginRequest);

    void logout(LogoutRequestDto logoutRequestDto);
}
