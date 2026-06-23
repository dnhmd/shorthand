package com.shorthand.backend.infrastructure.adapter.outbound.generator;

public class SnowflakeIdGenerator {

    private final long machineId;

    private static final long epoch = 1767225600000L;

    private static final long sequenceBits = 12L;
    private static final long machineIdShift = sequenceBits;
    private static final long timestampLeftShift = sequenceBits + machineIdShift;
    private static final long sequenceMask = ~(-1L << sequenceBits);

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long machineId) {
        this.machineId = machineId;
    }

    public synchronized long nextId() {
        long currentTimestamp = timeGen();
        if (currentTimestamp < lastTimestamp) throw new IllegalStateException("Clock moved backwards. Rejecting ID generation.");
        if (lastTimestamp == currentTimestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) currentTimestamp = waitForNextMillis(lastTimestamp);
        } else {
            sequence = 0L;
        }
        lastTimestamp = currentTimestamp;
        return ((currentTimestamp - epoch) << timestampLeftShift) |
                (machineId << machineIdShift) |
                sequence;
    }

    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) timestamp = timeGen();
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
