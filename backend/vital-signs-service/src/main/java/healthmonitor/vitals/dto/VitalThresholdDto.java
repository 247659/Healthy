package healthmonitor.vitals.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record VitalThresholdDto(

        @NotNull(message = "Ciśnienie rozkurczowe jest wymagane")
        @Min(value = 30, message = "Ciśnienie rozkurczowe nie może być niższe niż 30 mmHg")
        @Max(value = 150, message = "Ciśnienie rozkurczowe nie może przekraczać 150 mmHg")
        Integer diastolicBp,

        @NotNull(message = "Tętno jest wymagane")
        @Min(value = 30, message = "Progu tętna nie można ustawić poniżej 30 bpm")
        @Max(value = 250, message = "Progu tętna nie można ustawić powyżej 250 bpm")
        Integer heartRate,

        @NotNull(message = "Saturacja jest wymagana")
        @Min(value = 50, message = "Limit saturacji nie może być ustawiony poniżej 50%")
        @Max(value = 100, message = "Saturacja nie może przekraczać 100%")
        Integer spO2,

        @NotNull(message = "Ciśnienie skurczowe jest wymagane")
        @Min(value = 50, message = "Ciśnienie skurczowe nie może być niższe niż 50 mmHg")
        @Max(value = 300, message = "Ciśnienie skurczowe nie może przekraczać 300 mmHg")
        Integer systolicBp,

        @NotNull(message = "Temperatura jest wymagana")
        @Digits(integer = 2, fraction = 1, message = "Format temperatury: max 2 cyfry całkowite i 1 po przecinku")
        @Min(value = 30, message = "Temperatura nie może być niższa niż 30.0°C")
        @Max(value = 45, message = "Temperatura nie może przekraczać 45.0°C")
        BigDecimal temperature
) {
}