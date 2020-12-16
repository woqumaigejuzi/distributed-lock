package utils.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class JedisLock {

    private static final String IP = "127.0.0.1";
    private static final int PORT = 6379;


    private Jedis jedis;
    private String lockName;
    protected int INTERNAL_LOCK_LEASE_TIME = 30;
    /**
     * NX:如果不存在就设置这个key XX:如果存在就设置这个key
     * EX:单位为秒，PX:单位为毫秒
     */
    private SetParams params = SetParams.setParams().nx().ex(INTERNAL_LOCK_LEASE_TIME);


    public JedisLock(String lockName) {
        this.lockName = lockName;
        this.jedis = new JedisPool(IP, PORT).getResource();
    }

    public boolean lock(String id) {
        return false;
    }


    public synchronized boolean tryLock(String randomValue, long time, TimeUnit unit) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (true) {
            String set = jedis.set(lockName, randomValue, params);
            // SET命令返回OK ，则证明获取锁成功
            if ("OK".equalsIgnoreCase(set)) {
                return true;
            }
            long deta = System.currentTimeMillis() - start;
            long timeout = unit.toMillis(time);
            if (deta > timeout) {
                return false;
            }
            Thread.sleep(200);
        }
    }

    /**
     * 但是匹配 value 和删除 key 不是一个原子操作，Redis 也没有提供类似于delifequals这样的指令.
     * 这就需要使用 Lua 脚本来处理了，因为 Lua 脚本可以保证连续多个指令的原子性执行。
     *
     * @param randomValue
     * @return
     */
    public boolean unlock(String randomValue) {
        String script =
                "if redis.call('get',KEYS[1]) == ARGV[1] then" +
                        "   return redis.call('del',KEYS[1]) " +
                        "else" +
                        "   return 0 " +
                        "end";
        try {
            String result = jedis.eval(script, Collections.singletonList(lockName), Collections.singletonList(randomValue)).toString();
            return "1".equals(result);
        } finally {
            jedis.close();
        }
    }


}
