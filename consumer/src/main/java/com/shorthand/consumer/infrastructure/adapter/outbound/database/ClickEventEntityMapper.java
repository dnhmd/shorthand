package com.shorthand.consumer.infrastructure.adapter.outbound.database;

import com.shorthand.consumer.domain.model.ClickEvent;
import org.springframework.stereotype.Component;

@Component
public class ClickEventEntityMapper {

    public ClickEventEntity toEntity(ClickEvent clickEvent) {
        ClickEventEntity clickEventEntity = new ClickEventEntity();
        clickEventEntity.setLinkCode(clickEvent.linkCode());
        clickEventEntity.setIpAddress(clickEvent.ipAddress());
        clickEventEntity.setCountry(clickEvent.country());
        clickEventEntity.setDevice(clickEvent.device());
        clickEventEntity.setOs(clickEvent.os());
        clickEventEntity.setBrowser(clickEvent.browser());
        clickEventEntity.setUserAgent(clickEvent.userAgent());
        clickEventEntity.setClickedAt(clickEvent.clickedAt());

        return clickEventEntity;
    }
}
