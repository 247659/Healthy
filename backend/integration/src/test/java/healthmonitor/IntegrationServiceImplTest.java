package healthmonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthmonitor.dto.VitalSignsDto;
import healthmonitor.publisher.BatchPublisher;
import healthmonitor.publisher.VitalsPublisher;
import healthmonitor.service.IntegrationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BatchPublisher batchPublisher;

    @Mock
    private VitalsPublisher vitalsPublisher;

    @InjectMocks
    private IntegrationServiceImpl integrationService;

    @Test
    void receiveVitals_ShouldPublishSingleVitals() {
        // given
        VitalSignsDto dto = new VitalSignsDto();
        dto.setPatientId("pat-123");

        // when
        integrationService.receiveVitals(dto);

        // then
        verify(vitalsPublisher, times(1)).publicVitals(dto);
        verifyNoInteractions(batchPublisher);
    }

    @Test
    void processBatchMeasurements_ShouldSplitIntoBatches_WhenLinesExceedBatchSize() throws Exception {
        // given
        String validJson = "{\"patientId\":\"test\"}\n";
        StringBuilder sb = new StringBuilder();
        // Generujemy 2005 linii (rozmiar batcha to domyślnie 2000)
        for (int i = 0; i < 2005; i++) {
            sb.append(validJson);
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "vitals.json", "application/json", sb.toString().getBytes(StandardCharsets.UTF_8)
        );

        VitalSignsDto mockDto = new VitalSignsDto();
        when(objectMapper.readValue(anyString(), eq(VitalSignsDto.class))).thenReturn(mockDto);

        // when
        integrationService.processBatchMeasurements(file);

        // then
        // Oczekujemy, że publisher zostanie wywołany dokładnie 2 razy
        ArgumentCaptor<List<VitalSignsDto>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(batchPublisher, times(2)).publishBatch(batchCaptor.capture());

        List<List<VitalSignsDto>> capturedBatches = batchCaptor.getAllValues();
        assertEquals(2, capturedBatches.size());

        // Pierwsza paczka powinna mieć równo 2000 elementów (limit BATCH_SIZE)
        assertEquals(2000, capturedBatches.get(0).size());

        // Druga paczka powinna zawierać pozostałe 5 elementów
        assertEquals(5, capturedBatches.get(1).size());
    }

    @Test
    void processBatchMeasurements_ShouldSkipEmptyLines() throws Exception {
        // given
        // Plik zawiera dwie poprawne linie i kilka pustych (w tym ze spacjami)
        String content = "{\"patientId\":\"test1\"}\n\n   \n\t\n{\"patientId\":\"test2\"}\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "vitals.json", "application/json", content.getBytes(StandardCharsets.UTF_8)
        );

        when(objectMapper.readValue(anyString(), eq(VitalSignsDto.class))).thenReturn(new VitalSignsDto());

        // when
        integrationService.processBatchMeasurements(file);

        // then
        ArgumentCaptor<List<VitalSignsDto>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(batchPublisher, times(1)).publishBatch(batchCaptor.capture());

        // Puste linie powinny zostać zignorowane (continue), więc wynik to lista z 2 elementami
        List<VitalSignsDto> publishedBatch = batchCaptor.getValue();
        assertEquals(2, publishedBatch.size());

        // Weryfikujemy, że ObjectMapper był wywołany tylko 2 razy (dla linii zawierających tekst)
        verify(objectMapper, times(2)).readValue(anyString(), eq(VitalSignsDto.class));
    }

    @Test
    void processBatchMeasurements_ShouldThrowRuntimeException_OnIoException() throws Exception {
        // given
        MockMultipartFile mockFile = mock(MockMultipartFile.class);
        // Symulacja błędu odczytu strumienia wejściowego z pliku
        when(mockFile.getInputStream()).thenThrow(new IOException("Simulated IO Exception"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> integrationService.processBatchMeasurements(mockFile));

        assertEquals("Błąd podczas przetwarzania pliku wsadowego", exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());

        // W przypadku błędu odczytu nic nie powinno zostać opublikowane
        verifyNoInteractions(batchPublisher);
    }

    @Test
    void processBatchMeasurements_ShouldThrowRuntimeException_OnJsonParsingError() throws Exception {
        // given
        String invalidJsonLine = "To nie jest poprawny JSON";
        MockMultipartFile file = new MockMultipartFile(
                "file", "vitals.json", "text/plain", invalidJsonLine.getBytes(StandardCharsets.UTF_8)
        );

        // Symulacja rzucenia wyjątku przez ObjectMapper przy próbie parsowania
        when(objectMapper.readValue(anyString(), eq(VitalSignsDto.class)))
                .thenThrow(new RuntimeException("JSON Parse error"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> integrationService.processBatchMeasurements(file));

        assertEquals("Błąd podczas przetwarzania pliku wsadowego", exception.getMessage());
        verifyNoInteractions(batchPublisher);
    }
}
