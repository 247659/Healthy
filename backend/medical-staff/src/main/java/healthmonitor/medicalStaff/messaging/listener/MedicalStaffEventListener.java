package healthmonitor.medicalStaff.messaging.listener;

import healthmonitor.config.RabbitMQConfig;
import healthmonitor.medicalStaff.messaging.event.MedicalStaffRegisterEvent;
import healthmonitor.medicalStaff.messaging.mapper.MedicalStaffEventMapper;
import healthmonitor.medicalStaff.payload.request.MedicalStaffRequest;
import healthmonitor.medicalStaff.service.MedicalStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicalStaffEventListener {
    private final MedicalStaffService medicalStaffService;
    private final MedicalStaffEventMapper medicalStaffEventMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void registerStaff(MedicalStaffRegisterEvent event) {
        MedicalStaffRequest request = medicalStaffEventMapper.toRequest(event);
        medicalStaffService.save(request);
    }
}
