package healthmonitor.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import healthmonitor.config.RabbitMQConfig;
import healthmonitor.dto.VitalSignsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class StandardVitalsService implements VitalsService {

    private final InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String organization;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processVitals(VitalSignsDto dto) {
        log.info("InfluxDB entry for patient: {}, HR: {}", dto.getPatientId(), dto.getMeasurements().getHeartRate());

        try {
            Instant timestamp = Instant.parse(dto.getTimestamp());

            Point point = Point.measurement("patient_vitals")
                    .addTag("patient_id", dto.getPatientId())
                    .addField("heart_rate", dto.getMeasurements().getHeartRate())
                    .addField("systolic_bp", dto.getMeasurements().getBloodPressure().getSystolic())
                    .addField("diastolic_bp", dto.getMeasurements().getBloodPressure().getDiastolic())
                    .addField("temperature", dto.getMeasurements().getTemperature())
                    .addField("spO2", dto.getMeasurements().getSpO2())
                    .time(timestamp, WritePrecision.NS);

            // Zapis blokujący (synchroniczny) do bazy
            influxDBClient.getWriteApiBlocking().writePoint(bucket, organization, point);

        } catch (Exception e) {
            log.error("Error writing to InfluxDB: {}", e.getMessage());
        }
    }
}
