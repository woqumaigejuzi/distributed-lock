package jedis;

import redis.clients.jedis.Jedis;

public class JedisLockOriginal {

    public boolean lock(Jedis jedis, String lockName, Integer expire) {
        //返回是否设置成功
        //setNx加锁
        long now = System.currentTimeMillis();
        boolean result = jedis.setnx(lockName, String.valueOf(now + expire * 1000)) == 1;
        if (!result) {
            //防止死锁的容错
            String timestamp = jedis.get(lockName);
            if (timestamp != null && Long.parseLong(timestamp) < now) {
                //不通过del方法来删除锁。而是通过同步的getSet
                String oldValue = jedis.getSet(lockName, String.valueOf(now + expire));
                if (oldValue != null && oldValue.equals(timestamp)) {
                    result = true;
                    jedis.expire(lockName, expire);
                }
            }
        }
        if (result) {
            jedis.expire(lockName, expire);
        }
        return result;
    }

    public boolean unlock(Jedis jedis, String lockName) {
        jedis.del(lockName);
        return true;
    }


}
