package healthmonitor.notifications.sevice;

import healthmonitor.notifications.config.RabbitMQConfig;
import healthmonitor.notifications.model.AlertDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.List;

public interface AlertService {
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    void processAlert(AlertDto alertDto);

    List<AlertDto> getNotificationsForPatient(String patientId);

    List<AlertDto> getAllNotifications(String patientId);
}
