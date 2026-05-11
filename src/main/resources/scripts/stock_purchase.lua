  local stock = tonumber(redis.call('GET', KEYS[1]))
                   local purchase_reserved = tonumber(redis.call('EXISTS', KEYS[2]))
                   local purchase_done = tonumber(redis.call('EXISTS', KEYS[3]))
                   local qty = tonumber(ARGV[1])

                   if purchase_reserved == 1 or purchase_done == 1 then
                      return -4
                   end

                   if qty == nil or qty <= 0 then
                      return -3
                   end

                   if stock == nil then
                      return -2
                   end

                   if stock < qty then
                      return -1
                   end

                   redis.call('DECRBY',KEYS[1],qty)
                   local ttl = redis.call('TTL', KEYS[1])
                   if ttl > 0 then
                      redis.call('SET', KEYS[2], "1", 'EX', ttl)
                   else
                      redis.call('SET', KEYS[2], "1")
                   end
                   return 1