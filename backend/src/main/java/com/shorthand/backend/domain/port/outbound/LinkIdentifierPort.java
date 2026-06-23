package com.shorthand.backend.domain.port.outbound;

public interface LinkIdentifierPort {
    Long generateSnowflakeId();
    String generateCode();
}
