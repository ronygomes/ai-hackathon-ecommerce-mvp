package com.ecommerce.core.domain;

import java.util.List;

public interface IDomainEvent {
    String getEventId();

    long getTimestamp();
}
