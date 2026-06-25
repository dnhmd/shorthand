package com.shorthand.consumer.domain.port.outbound;

import com.shorthand.consumer.domain.model.ClickEvent;

public interface ClickEventRepository {
    void save(ClickEvent clickEvent);
}
