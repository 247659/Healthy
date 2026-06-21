package healthmonitor.vitals.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import healthmonitor.vitals.config.RabbitMQConfig;
import healthmonitor.vitals.dto.VitalSignsDto;
import healthmonitor.vitals.dto.VitalThresholdDto;
import healthmonitor.vitals.event.VitalThresholdEvent;
import healthmonitor.vitals.mapper.VitalThresholdMapper;
import healthmonitor.vitals.model.VitalThreshold;
import healthmonitor.vitals.repository.VitalThresholdRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class VitalsServiceImpl implements VitalsService {

    private final VitalThresholdMapper vitalThresholdMapper;
    private final VitalThresholdRepository vitalThresholdRepository;
    private final InfluxDBClient influxDBClient;
    private final WriteApi writeApi;

    public VitalsServiceImpl(VitalThresholdMapper vitalThresholdMapper,
                             VitalThresholdRepository vitalThresholdRepository,
                             InfluxDBClient influxDBClient) {
        this.vitalThresholdMapper = vitalThresholdMapper;
        this.vitalThresholdRepository = vitalThresholdRepository;
        this.influxDBClient = influxDBClient;
        this.writeApi = influxDBClient.makeWriteApi();
    }

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
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processAndSaveVitals(VitalSignsDto dto) {
        log.info("Processing and saving vitals for patient: {}", dto.getPatientId());
        try {
            Point point = saveToDatabase(dto);
            influxDBClient.getWriteApiBlocking().writePoint(bucket, organization, point);

        } catch (Exception e) {
            log.error("Error during processing vitals: {}", e.getMessage());
        }
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.THRESHOLD_QUEUE)
    public void saveThreshold(VitalThresholdEvent event) {
        VitalThreshold vitalThreshold = new VitalThreshold();
        vitalThreshold.setPatientId(event.patientId());
        vitalThresholdRepository.save(vitalThreshold);
    }

    @Override
    public VitalThresholdDto update(String patientId, VitalThresholdDto request) {
        VitalThreshold vitalThreshold = vitalThresholdMapper.toEntity(request);
        vitalThreshold.setPatientId(patientId);
        VitalThreshold savedVitalThreshold = vitalThresholdRepository.save(vitalThreshold);
        return vitalThresholdMapper.toDto(savedVitalThreshold);
    }

    @Override
    public VitalThresholdDto getByPatientId(String patientId) {
        return vitalThresholdRepository.findById(patientId)
                .map(vitalThresholdMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient's vital threshold not found"));
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.BATCH_QUEUE_NAME)
    public void processAndSaveVitalsBatch(List<VitalSignsDto> batch) {
        log.info("Odebrano paczkę {} pomiarów do zapisu masowego.", batch.size());
        try {
            for (VitalSignsDto dto : batch) {
                Point point = saveToDatabase(dto);
                writeApi.writePoint(bucket, organization, point);
            }

            log.info("Paczka została przekazana do asynchronicznego zapisu InfluxDB.");
        } catch (Exception e) {
            log.error("Błąd podczas zapisywania paczki batch do InfluxDB: {}", e.getMessage());
        }
    }

    private Point saveToDatabase(VitalSignsDto dto) {
        Instant timestamp = Instant.parse(dto.getTimestamp());

        return Point.measurement("vitals")
                .addTag("patient_id", dto.getPatientId())
                .addField("heart_rate", dto.getMeasurements().getHeartRate())
                .addField("systolic_bp", dto.getMeasurements().getBloodPressure().getSystolic())
                .addField("diastolic_bp", dto.getMeasurements().getBloodPressure().getDiastolic())
                .addField("temperature", dto.getMeasurements().getTemperature())
                .addField("spO2", dto.getMeasurements().getSpO2())
                .time(timestamp, WritePrecision.NS);
    }
}
