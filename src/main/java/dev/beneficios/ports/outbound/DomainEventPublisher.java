package dev.beneficios.ports.outbound;

import dev.beneficios.domain.model.shared.DomainEvent;
import java.util.List;

public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}
