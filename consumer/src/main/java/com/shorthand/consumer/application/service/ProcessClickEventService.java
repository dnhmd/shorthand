package com.shorthand.consumer.application.service;

import com.shorthand.common.event.LinkClickEvent;
import com.shorthand.consumer.domain.model.ClickEvent;
import com.shorthand.consumer.domain.port.inbound.ProcessClickEventUseCase;
import com.shorthand.consumer.domain.port.outbound.ClickEventRepository;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

public class ProcessClickEventService implements ProcessClickEventUseCase {

    private final UserAgentAnalyzer userAgentAnalyzer;
    private final ClickEventRepository clickEventRepository;

    public ProcessClickEventService(UserAgentAnalyzer userAgentAnalyzer, ClickEventRepository clickEventRepository) {
        this.userAgentAnalyzer = userAgentAnalyzer;
        this.clickEventRepository = clickEventRepository;
    }

    @Override
    public void processClickEvent(LinkClickEvent linkClickEvent) {
        UserAgent userAgent = userAgentAnalyzer.parse(linkClickEvent.userAgent());

        ClickEvent clickEvent = new ClickEvent(
                linkClickEvent.code(),
                linkClickEvent.ipAddress(),
                null,
                userAgent.getValue("DeviceName"),
                userAgent.getValue("OperatingSystemNameVersion"),
                userAgent.getValue("AgentNameVersion"),
                linkClickEvent.userAgent(),
                linkClickEvent.clickedAt()
        );
        clickEventRepository.save(clickEvent);
    }
}
