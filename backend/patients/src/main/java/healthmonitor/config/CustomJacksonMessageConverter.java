package healthmonitor.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import java.io.IOException;

public class CustomJacksonMessageConverter implements MessageConverter {

    private final ObjectMapper objectMapper;

    public CustomJacksonMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties)
            throws MessageConversionException {

        try {
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);

            byte[] json = objectMapper.writeValueAsBytes(object);

            return new Message(json, messageProperties);

        } catch (JsonProcessingException e) {
            throw new MessageConversionException("Error serializing message to JSON", e);
        }
    }

    @Override
    public Object fromMessage(Message message)
            throws MessageConversionException {

        try {
            byte[] body = message.getBody();

            return objectMapper.readValue(body, Object.class);

        } catch (IOException e) {
            throw new MessageConversionException("Error deserializing message from JSON", e);
        }
    }
}
