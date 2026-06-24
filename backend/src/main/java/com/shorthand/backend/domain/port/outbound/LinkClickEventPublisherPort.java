package com.shorthand.backend.domain.port.outbound;

import com.shorthand.common.event.LinkClickEvent;

public interface LinkClickEventPublisherPort {
    void publishMessage(LinkClickEvent event);
}
