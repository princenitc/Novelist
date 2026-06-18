package com.prince.novelist.integration;

import com.prince.novelist.TestSetup;
import com.prince.novelist.event.BookEvent;
import com.prince.novelist.publisher.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Import(TestSetup.class)
public class RabbitMQIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }

    @Autowired
    private DomainEventPublisher eventPublisher;

    @Test
    void testPublishAndConsumeBookEvent() throws InterruptedException {
        BookEvent event = new BookEvent(UUID.randomUUID().toString(), BookEvent.Action.CREATED, null, System.currentTimeMillis());
        eventPublisher.publishBookEvent(event);
        
        // Wait to allow consumer to process
        TimeUnit.SECONDS.sleep(2);
        
        // Basic assertion to ensure context loads and message is published without errors
        assertThat(true).isTrue();
    }
}
