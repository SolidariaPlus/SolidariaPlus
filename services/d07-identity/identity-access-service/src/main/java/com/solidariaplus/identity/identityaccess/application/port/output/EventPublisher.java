package com.solidariaplus.identity.identityaccess.application.port.output;

import com.solidariaplus.identity.identityaccess.domain.event.DomainEvent;

public interface EventPublisher {
    void publish(DomainEvent event);
}
