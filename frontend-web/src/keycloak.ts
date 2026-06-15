import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
    url: 'http://localhost:9090', // Zmień na właściwy port Keycloak, jeśli jest inny
    realm: 'healthmonitor-realm',            // Nazwa realmu z Twojego pliku realm-export.json
    clientId: 'account'        // Upewnij się, że taki klient istnieje w Keycloak
});

export default keycloak;