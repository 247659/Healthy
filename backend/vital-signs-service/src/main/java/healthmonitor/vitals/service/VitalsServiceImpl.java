package healthmonitor.vitals.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import healthmonitor.vitals.dto.VitalSignsDto;
import healthmonitor.vitals.messaging.event.VitalsRegisteredEvent;
import healthmonitor.vitals.messaging.publisher.VitalsEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class VitalsServiceImpl implements VitalsService {

    private final InfluxDBClient influxDBClient;
    private final VitalsEventPublisher vitalsEventPublisher;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String organization;

    @Override
    public List<VitalSignsDto> getPatientHistory(String patientId, Instant from, Instant to) {
        log.info("Quering patient vitals history from db: {}", patientId);
        List<VitalSignsDto> history = new ArrayList<>();
        String fluxQuery = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"vitals\") " +
                        "|> filter(fn: (r) => r[\"patient_id\"] == \"%s\") " +
                        "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> sort(columns: [\"_time\"], desc: true)",
                bucket, from.toString(), to.toString(), patientId
        );

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery, organization);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    VitalSignsDto dto = new VitalSignsDto();
                    dto.setPatientId(patientId);
                    dto.setTimestamp(Objects.requireNonNull(record.getTime()).toString());

                    VitalSignsDto.Measurements measurements = new VitalSignsDto.Measurements();

                    if (record.getValueByKey("heart_rate") != null) {
                        measurements.setHeartRate(((Number) Objects.requireNonNull(record.getValueByKey("heart_rate"))).intValue());
                    }
                    if (record.getValueByKey("temperature") != null) {
                        measurements.setTemperature(((Number) Objects.requireNonNull(record.getValueByKey("temperature"))).doubleValue());
                    }
                    if (record.getValueByKey("spO2") != null) {
                        measurements.setSpO2(((Number) Objects.requireNonNull(record.getValueByKey("spO2"))).intValue());
                    }

                    VitalSignsDto.BloodPressure bp = new VitalSignsDto.BloodPressure();
                    if (record.getValueByKey("systolic_bp") != null) {
                        bp.setSystolic(((Number) Objects.requireNonNull(record.getValueByKey("systolic_bp"))).intValue());
                    }

                    if (record.getValueByKey("diastolic_bp") != null) {
                        bp.setDiastolic(((Number) Objects.requireNonNull(record.getValueByKey("diastolic_bp"))).intValue());
                    }
                    measurements.setBloodPressure(bp);

                    dto.setMeasurements(measurements);
                    history.add(dto);
                }
            }
        } catch (Exception e) {
            log.error("Error during quering patient data from db {}: {}", patientId, e.getMessage());
        }

        return history;
    }

    @Override
    public void processAndSaveVitals(VitalSignsDto dto) {
        try {
            Instant timestamp = Instant.parse(dto.getTimestamp());

            Point point = Point.measurement("vitals")
                    .addTag("patient_id", dto.getPatientId())
                    .addField("heart_rate", dto.getMeasurements().getHeartRate())
                    .addField("systolic_bp", dto.getMeasurements().getBloodPressure().getSystolic())
                    .addField("diastolic_bp", dto.getMeasurements().getBloodPressure().getDiastolic())
                    .addField("temperature", dto.getMeasurements().getTemperature())
                    .addField("spO2", dto.getMeasurements().getSpO2())
                    .time(timestamp, WritePrecision.NS);

            influxDBClient.getWriteApiBlocking().writePoint(bucket, organization, point);

            VitalsRegisteredEvent event = VitalsRegisteredEvent.builder()
                    .patientId(dto.getPatientId())
                    .timestamp(timestamp)
                    .heartRate(dto.getMeasurements().getHeartRate())
                    .systolicBp(dto.getMeasurements().getBloodPressure().getSystolic())
                    .diastolicBp(dto.getMeasurements().getBloodPressure().getDiastolic())
                    .temperature(dto.getMeasurements().getTemperature())
                    .spO2(dto.getMeasurements().getSpO2())
                    .build();

            vitalsEventPublisher.publishVitalsRegisteredEvent(event);

        } catch (Exception e) {
            log.error("Error during processing vitals: {}", e.getMessage());
        }
    }
}
