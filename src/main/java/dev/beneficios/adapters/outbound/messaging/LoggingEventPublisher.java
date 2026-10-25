package dev.beneficios.adapters.outbound.messaging;

import dev.beneficios.ports.outbound.DomainEventPublisher;
import dev.beneficios.domain.model.shared.DomainEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(event -> log.info("Domain event: {}", event));
    }
}
