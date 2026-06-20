package healthmonitor.notifications.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Błąd podczas konwersji listy na JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String jsonInfo) {
        if (jsonInfo == null || jsonInfo.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(jsonInfo, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas odczytu JSONa do listy", e);
        }
    }
}