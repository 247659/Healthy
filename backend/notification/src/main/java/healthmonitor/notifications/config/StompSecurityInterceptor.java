package healthmonitor.notifications.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class StompSecurityInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter;

    public StompSecurityInterceptor(
            JwtDecoder jwtDecoder,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        // 1. UWIERZYTELNIANIE PRZY NAWIĄZYWANIU POŁĄCZENIA STOMP (CONNECT)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AuthenticationCredentialsNotFoundException("Brak lub niepoprawny nagłówek Authorization w ramce CONNECT");
            }

            String token = authHeader.substring(7);
            // Walidacja podpisu i daty ważności tokenu przez Keycloak JwtDecoder
            Jwt jwt = jwtDecoder.decode(token);
            // Konwersja JWT na Authentication wraz z rolami z realm_access.roles
            AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);

            // Zapisanie uwierzytelnionego użytkownika w sesji WebSocket
            accessor.setUser(authentication);
        }

        // 2. AUTORYZACJA PRZY SUBSKRYPCJI KANAŁU (SUBSCRIBE)
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination == null) {
                throw new AccessDeniedException("Brak ścieżki docelowej (destination) subskrypcji");
            }

            if (!(accessor.getUser() instanceof JwtAuthenticationToken auth)) {
                throw new AuthenticationCredentialsNotFoundException("Brak uwierzytelnienia w sesji WebSocket");
            }

            String userSub = auth.getToken().getSubject(); // UUID z Keycloaka
            Collection<GrantedAuthority> authorities = auth.getAuthorities();

            // Przypadek 1: Subskrypcja przez Spring User Destination (/user/queue/alerts)
            // Spring sam izoluje ten kanał po Principal.getName() (czyli 'sub' z tokena)
            if (destination.equals("/user/queue/alerts")) {
                if (!hasRole(authorities, "ROLE_DOCTOR") && !hasRole(authorities, "ROLE_PATIENT")) {
                    throw new AccessDeniedException("Brak wymaganej roli (DOCTOR lub PATIENT) do subskrypcji alertów");
                }
            }
            // Przypadek 2: Jawna subskrypcja kanału lekarza z ID w ścieżce (np. /topic/doctor/{doctorId}/alerts lub /queue/doctor/{doctorId}/alerts)
            else if (destination.startsWith("/topic/doctor/") || destination.startsWith("/queue/doctor/")) {
                if (!hasRole(authorities, "ROLE_DOCTOR")) {
                    throw new AccessDeniedException("Tylko lekarz (ROLE_DOCTOR) może subskrybować ten kanał");
                }
                String targetDoctorId = extractIdFromDestination(destination, "doctor");
                if (!userSub.equals(targetDoctorId)) {
                    throw new AccessDeniedException("Lekarz może subskrybować wyłącznie własny kanał (niezgodne doctor_id)");
                }
            }
            // Przypadek 3: Jawna subskrypcja kanału pacjenta z ID w ścieżce (np. /topic/patient/{patientId}/alerts)
            else if (destination.startsWith("/topic/patient/") || destination.startsWith("/queue/patient/")) {
                boolean isDoctor = hasRole(authorities, "ROLE_DOCTOR");
                boolean isPatient = hasRole(authorities, "ROLE_PATIENT");

                if (!isDoctor && !isPatient) {
                    throw new AccessDeniedException("Brak wymaganej roli (DOCTOR lub PATIENT)");
                }
                if (isPatient && !isDoctor) {
                    String targetPatientId = extractIdFromDestination(destination, "patient");
                    if (!userSub.equals(targetPatientId)) {
                        throw new AccessDeniedException("Pacjent może subskrybować wyłącznie własny kanał");
                    }
                }
            } else {
                throw new AccessDeniedException("Nieautoryzowany kanał subskrypcji: " + destination);
            }
        }

        return message;
    }

    private boolean hasRole(Collection<GrantedAuthority> authorities, String role) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    /**
     * Wyciąga ID ze ścieżki np. "/topic/doctor/550e8400-e29b.../alerts" -> "550e8400-e29b..."
     */
    private String extractIdFromDestination(String destination, String prefixType) {
        String[] parts = destination.split("/");
        // Szukamy segmentu "doctor" lub "patient" i bierzemy kolejny element jako ID
        for (int i = 0; i < parts.length - 1; i++) {
            if (prefixType.equals(parts[i])) {
                return parts[i + 1];
            }
        }
        throw new AccessDeniedException("Nieprawidłowy format ścieżki kanału: " + destination);
    }
}