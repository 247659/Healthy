package healthmonitor.notifications;

import com.fasterxml.jackson.databind.JsonMappingException;
import healthmonitor.notifications.converter.StringListConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StringListConverterTest {

    private StringListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringListConverter();
    }

    @Test
    void convertToDatabaseColumn_ShouldReturnJsonString_WhenListIsNotEmpty() {
        // given
        List<String> detailsList = List.of("Wysokie tętno", "Niska saturacja");

        // when
        // Tutaj realizowana jest ścieżka z objectMapper.writeValueAsString(list)
        String jsonResult = converter.convertToDatabaseColumn(detailsList);

        // then
        assertEquals("[\"Wysokie tętno\",\"Niska saturacja\"]", jsonResult);
    }

    @Test
    void convertToDatabaseColumn_ShouldReturnEmptyJsonArray_WhenListIsNull() {
        // given
        List<String> nullList = null;

        // when
        String jsonResult = converter.convertToDatabaseColumn(nullList);

        // then
        assertEquals("[]", jsonResult);
    }

    @Test
    void convertToDatabaseColumn_ShouldReturnEmptyJsonArray_WhenListIsEmpty() {
        // given
        List<String> emptyList = List.of();

        // when
        String jsonResult = converter.convertToDatabaseColumn(emptyList);

        // then
        assertEquals("[]", jsonResult);
    }

    // Dodatkowo test w drugą stronę dla kompletności pokrycia kodu
    @Test
    void convertToEntityAttribute_ShouldReturnList_WhenJsonIsProvided() {
        // given
        String jsonInfo = "[\"Skok ciśnienia\",\"Spadek wagi\"]";

        // when
        List<String> resultList = converter.convertToEntityAttribute(jsonInfo);

        // then
        assertEquals(2, resultList.size());
        assertEquals("Skok ciśnienia", resultList.get(0));
        assertEquals("Spadek wagi", resultList.get(1));
    }

    @Test
    void convertToEntityAttribute_ShouldThrowRuntimeException_WhenJsonIsInvalid() {
        // given
        // Przekazujemy absolutnie niepoprawny format JSON (np. brak klamer, zła składnia)
        String invalidJson = "To nie jest poprawny JSON [}";

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> converter.convertToEntityAttribute(invalidJson));

        assertEquals("Błąd podczas odczytu JSONa do listy", exception.getMessage());
        // Sprawdzamy, czy w środku znajduje się oryginalny wyjątek z Jacksona
        assertInstanceOf(IOException.class, exception.getCause());
    }
}