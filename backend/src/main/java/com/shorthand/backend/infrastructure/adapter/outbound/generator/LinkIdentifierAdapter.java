package com.shorthand.backend.infrastructure.adapter.outbound.generator;

import com.shorthand.backend.domain.port.outbound.LinkIdentifierPort;

public class LinkIdentifierAdapter implements LinkIdentifierPort {

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final Base62Encoder base62Encoder;

    public LinkIdentifierAdapter(SnowflakeIdGenerator snowflakeIdGenerator, Base62Encoder base62Encoder) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.base62Encoder = base62Encoder;
    }

    @Override
    public Long generateSnowflakeId() {
        return snowflakeIdGenerator.nextId();
    }

    @Override
    public String generateCode() {
        Long snowflakeId = generateSnowflakeId();
        return base62Encoder.encode(snowflakeId);
    }
}
