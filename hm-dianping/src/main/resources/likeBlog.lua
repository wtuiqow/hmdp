-- KEYS[1]: blog:liked:{blogId}          set of userIds
-- KEYS[2]: blog:liked:count:{blogId}    like count
-- ARGV[1]: userId

if redis.call('sismember', KEYS[1], ARGV[1]) == 1 then
    redis.call('srem', KEYS[1], ARGV[1])
    redis.call('decr', KEYS[2])
    return 1
else
    redis.call('sadd', KEYS[1], ARGV[1])
    redis.call('incr', KEYS[2])
    return 2
end

