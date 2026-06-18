package healthmonitor.notifications.sevice;

import healthmonitor.notifications.config.RabbitMQConfig;
import healthmonitor.notifications.model.AlertDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public interface AlertService {
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    void processAlert(AlertDto alertDto);
}
