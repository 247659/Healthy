package healthmonitor.notifications.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // "/queue" dla wiadomości prywatnych (1 to 1), "/topic" dla grupowych
        config.enableSimpleBroker("/queue", "/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Frontend będzie się łączył pod ten adres: ws://localhost:8085/api/v1/ws-notifications
        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns("*") // WAŻNE: Na froncie podepnij JWT token
                .withSockJS();
    }
}
