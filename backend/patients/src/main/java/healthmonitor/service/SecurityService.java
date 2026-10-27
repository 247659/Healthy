package healthmonitor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;


@Component("securityService")
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    public boolean isOwner(Jwt jwt, String patientId) throws AccessDeniedException {
        if (jwt == null || patientId == null) {
            return false;
        }

        String userSub = jwt.getSubject();

        if (!userSub.equals(patientId)) {
            log.warn("User with sub {} is not authorized to access patient with id {}", userSub, patientId);
            throw new AccessDeniedException("Access denied");
        }
        return true;
    }
}
