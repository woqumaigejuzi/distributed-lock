import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class JedisLock implements Lock {

    private static final String IP = "127.0.0.1";
    private static final int PORT = 6379;


    private Jedis jedis;
    private String lockName;
    protected int INTERNAL_LOCK_LEASE_TIME = 5;
    /**
     * NX:如果不存在就设置这个key XX:如果存在就设置这个key
     * EX:单位为秒，PX:单位为毫秒
     */
    private SetParams params = SetParams.setParams().nx().ex(INTERNAL_LOCK_LEASE_TIME);


    public JedisLock(String lockName) {
        this.lockName = lockName;
        this.jedis = new JedisPool(IP, PORT).getResource();
    }

    public void lock() {

    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (true) {
            String set = jedis.set(lockName, "", params);
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

    @Override
    public void unlock() {

    }

    @Override
    public Condition newCondition() {
        return null;
    }


}
