local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Retrieve current bucket state
local data = redis.call('HMGET', key, 'tokens', 'last_updated')
local tokens = tonumber(data[1])
local last_updated = tonumber(data[2])

-- Initialize if key does not exist
if not tokens then
    tokens = capacity
    last_updated = now
else
    -- Replenish tokens based on elapsed time
    local elapsed = math.max(0, now - last_updated)
    tokens = math.min(capacity, tokens + (elapsed * refill_rate))
end

-- Check if enough tokens are available
if tokens >= requested then
    tokens = tokens - requested
    redis.call('HSET', key, 'tokens', tokens, 'last_updated', now)
    redis.call('EXPIRE', key, math.ceil(capacity / refill_rate)) -- Auto-clean old keys
    return 1 -- Allowed
else
    redis.call('HSET', key, 'tokens', tokens, 'last_updated', now)
    return 0 -- Denied
end