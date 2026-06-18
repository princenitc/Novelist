package com.prince.novelist.publisher;

import com.prince.novelist.config.MessageQueueConfig;
import com.prince.novelist.event.BookEvent;
import com.prince.novelist.event.RatingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);
    
    private final RabbitTemplate rabbitTemplate;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishBookEvent(BookEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == 0) {
            event.setTimestamp(System.currentTimeMillis());
        }
        log.info("Publishing BookEvent: {} for bookId: {}", event.getAction(), event.getBook() != null ? event.getBook().getBookId() : "null");
        rabbitTemplate.convertAndSend(MessageQueueConfig.EXCHANGE, "book." + event.getAction().name().toLowerCase(), event);
    }

    public void publishRatingEvent(RatingEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == 0) {
            event.setTimestamp(System.currentTimeMillis());
        }
        log.info("Publishing RatingEvent for bookId: {}, userId: {}", event.getBookId(), event.getUserId());
        rabbitTemplate.convertAndSend(MessageQueueConfig.EXCHANGE, "rating.added", event);
    }
}
