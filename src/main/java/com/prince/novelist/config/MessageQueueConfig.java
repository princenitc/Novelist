package com.prince.novelist.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageQueueConfig {

    public static final String EXCHANGE = "novelist.domain.exchange";
    public static final String BOOK_QUEUE = "book.events.queue";
    public static final String RATING_QUEUE = "rating.events.queue";
    
    public static final String DLX = "novelist.dlx";
    public static final String DLQ = "novelist.dlq";

    public static final String BOOK_ROUTING_KEY = "book.#";
    public static final String RATING_ROUTING_KEY = "rating.#";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange domainExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Queue bookQueue() {
        return QueueBuilder.durable(BOOK_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", BOOK_QUEUE)
                .build();
    }

    @Bean
    public Queue ratingQueue() {
        return QueueBuilder.durable(RATING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", RATING_QUEUE)
                .build();
    }

    @Bean
    public Binding bookBinding(Queue bookQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(bookQueue).to(domainExchange).with(BOOK_ROUTING_KEY);
    }

    @Bean
    public Binding ratingBinding(Queue ratingQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(ratingQueue).to(domainExchange).with(RATING_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBookBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(BOOK_QUEUE);
    }

    @Bean
    public Binding deadLetterRatingBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(RATING_QUEUE);
    }
}
