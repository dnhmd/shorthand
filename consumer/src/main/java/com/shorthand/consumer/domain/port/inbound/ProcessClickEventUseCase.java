package com.shorthand.consumer.domain.port.inbound;

import com.shorthand.common.event.LinkClickEvent;

public interface ProcessClickEventUseCase {
    void processClickEvent(LinkClickEvent linkClickEvent);
}
