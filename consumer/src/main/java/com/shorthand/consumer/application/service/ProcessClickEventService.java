package com.shorthand.consumer.application.service;

import com.shorthand.common.event.LinkClickEvent;
import com.shorthand.consumer.domain.model.ClickEvent;
import com.shorthand.consumer.domain.port.inbound.ProcessClickEventUseCase;
import com.shorthand.consumer.domain.port.outbound.ClickEventRepository;
import com.shorthand.consumer.infrastructure.adapter.outbound.geoip.GeoIpAdapter;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessClickEventService implements ProcessClickEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessClickEventService.class);

    private final UserAgentAnalyzer userAgentAnalyzer;
    private final GeoIpAdapter geoIpAdapter;
    private final ClickEventRepository clickEventRepository;

    public ProcessClickEventService(UserAgentAnalyzer userAgentAnalyzer, GeoIpAdapter geoIpAdapter, ClickEventRepository clickEventRepository) {
        this.userAgentAnalyzer = userAgentAnalyzer;
        this.geoIpAdapter = geoIpAdapter;
        this.clickEventRepository = clickEventRepository;
    }

    @Override
    public void processClickEvent(LinkClickEvent linkClickEvent) {
        UserAgent userAgent = userAgentAnalyzer.parse(linkClickEvent.userAgent());

        ClickEvent clickEvent = new ClickEvent(
                linkClickEvent.code(),
                linkClickEvent.ipAddress(),
                geoIpAdapter.resolveCountry(linkClickEvent.ipAddress()),
                userAgent.getValue("DeviceName"),
                userAgent.getValue("OperatingSystemNameVersion"),
                userAgent.getValue("AgentNameVersion"),
                linkClickEvent.userAgent(),
                linkClickEvent.clickedAt()
        );

        log.debug("Click Event | Code: {} | Enriched & Processed", clickEvent.linkCode());
        clickEventRepository.save(clickEvent);
        log.debug("Click Event | Code: {} | Saved", clickEvent.linkCode());
    }
}
