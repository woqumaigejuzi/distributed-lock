package jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisTest {
    static Jedis jedis = new JedisPool("172.171.15.232", 6379).getResource();
    static String keyName = "name";
    static String keyLock = "lock";


    public static void main(String[] args) {
        System.out.println(jedis);
    }




}
