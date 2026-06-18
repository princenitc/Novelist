package com.prince.novelist.consumer;

import com.prince.novelist.config.MessageQueueConfig;
import com.prince.novelist.event.BookEvent;
import com.prince.novelist.event.RatingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    @RabbitListener(queues = MessageQueueConfig.BOOK_QUEUE)
    public void consumeBookEvent(BookEvent event) {
        log.info("Received BookEvent: action={}, bookId={}", event.getAction(), 
            (event.getBook() != null ? event.getBook().getBookId() : "null"));
        // Future Phase 3 (RAG Integration): Generate embeddings, update vector store, etc.
    }

    @RabbitListener(queues = MessageQueueConfig.RATING_QUEUE)
    public void consumeRatingEvent(RatingEvent event) {
        log.info("Received RatingEvent: userId={}, bookId={}, rating={}", 
            event.getUserId(), event.getBookId(), event.getRating());
        // Future Phase 4: Generate recommendations based on new ratings.
    }
}
