-- KEYS[1]: blog:liked:{blogId}          zset of userIds, score = like time
-- KEYS[2]: blog:liked:count:{blogId}    like count
-- ARGV[1]: userId
-- ARGV[2]: like time

if redis.call('zscore', KEYS[1], ARGV[1]) then
    redis.call('zrem', KEYS[1], ARGV[1])
    redis.call('decr', KEYS[2])
    return 1
else
    redis.call('zadd', KEYS[1], ARGV[2], ARGV[1])
    redis.call('incr', KEYS[2])
    return 2
end
