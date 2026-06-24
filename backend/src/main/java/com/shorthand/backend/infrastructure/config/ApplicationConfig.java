package com.shorthand.backend.infrastructure.config;

import com.shorthand.backend.application.service.CreateLinkService;
import com.shorthand.backend.application.service.RedirectLinkService;
import com.shorthand.backend.domain.port.inbound.CreateLinkUseCase;
import com.shorthand.backend.domain.port.inbound.RedirectLinkUseCase;
import com.shorthand.backend.domain.port.outbound.LinkCachePort;
import com.shorthand.backend.domain.port.outbound.LinkClickEventPublisherPort;
import com.shorthand.backend.domain.port.outbound.LinkIdentifierPort;
import com.shorthand.backend.domain.port.outbound.LinkRepository;
import com.shorthand.backend.infrastructure.adapter.outbound.generator.Base62Encoder;
import com.shorthand.backend.infrastructure.adapter.outbound.generator.LinkIdentifierAdapter;
import com.shorthand.backend.infrastructure.adapter.outbound.generator.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    private final ShorthandProperties shorthandProperties;

    public ApplicationConfig(ShorthandProperties shorthandProperties) {
        this.shorthandProperties = shorthandProperties;
    }

    @Bean
    public CreateLinkUseCase createLinkUseCase(LinkRepository linkRepository, LinkIdentifierPort linkIdentifierPort) {
        return new CreateLinkService(linkRepository, linkIdentifierPort, shorthandProperties.link().defaultExpiryDays());
    }

    @Bean
    public RedirectLinkUseCase redirectLinkUseCase(LinkCachePort linkCachePort, LinkRepository linkRepository, LinkClickEventPublisherPort linkClickEventPublisherPort) {
        return new RedirectLinkService(linkCachePort, linkRepository, linkClickEventPublisherPort);
    }

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(shorthandProperties.snowflake().machineId());
    }

    @Bean
    public Base62Encoder base62Encoder() {
        return new Base62Encoder();
    }

    @Bean
    public LinkIdentifierPort linkIdentifierPort(SnowflakeIdGenerator snowflakeIdGenerator, Base62Encoder base62Encoder) {
        return new LinkIdentifierAdapter(snowflakeIdGenerator, base62Encoder);
    }
}
