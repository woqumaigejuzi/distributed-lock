package utils.redission;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class MyRedissonLock {

    private RLock myLock;

    public MyRedissonLock(String lockName) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        RedissonClient redisson = Redisson.create(config);
        myLock = redisson.getLock(lockName);
    }

    public RLock getRLock() {
        return myLock;
    }


}
