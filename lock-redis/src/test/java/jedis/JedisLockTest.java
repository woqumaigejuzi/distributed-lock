package jedis;

import utils.jedis.JedisLock;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JedisLockTest {
    static ThreadPoolExecutor pool = new ThreadPoolExecutor(
            10, 20, 10 * 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10)
    );
    static String keyLock = "lock";

    public static void main(String[] args) throws InterruptedException {
        JedisLock jedisLock = new JedisLock(keyLock);
        JedisLock jedisLock2 = new JedisLock(keyLock);
        List<JedisLock> jedisLocks = Arrays.asList(jedisLock, jedisLock2);
        // 表示线程 1、3 属于一个应用，共享锁1    2、4属于一个应用，共享锁2
        for (int i = 0; i < 4; i++) {
            pool.submit(new Worker(jedisLocks.get(i % jedisLocks.size())));
        }
        pool.shutdown();
    }

    static class Worker implements Runnable {
        static AtomicInteger gen = new AtomicInteger();
        private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        private Integer workerId = gen.incrementAndGet();
        JedisLock jedisLock;

        public Worker(JedisLock jedisLock) {
            this.jedisLock = jedisLock;
        }

        @Override
        public void run() {
            boolean locked = false;
            String seq = UUID.randomUUID().toString();
            try {
                System.out.println(sdf.format(new Date()) + "  " + workerId + "尝试获得锁,序号：" + seq);
                locked = jedisLock.tryLock(seq, 10 * 1000, TimeUnit.MILLISECONDS);
                if (locked) {
                    System.out.println(sdf.format(new Date()) + "  " + workerId + "获得业务锁" + keyLock + "业务开始");
                    Thread.sleep(6 * 1000);
                    System.out.println(sdf.format(new Date()) + "  " + workerId + "业务结束");
                } else {
                    System.out.println(sdf.format(new Date()) + "  " + workerId + "获取锁" + keyLock + "失败");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (locked) {
                    boolean unlock = jedisLock.unlock(seq);
                    if (unlock) {
                        System.out.println(sdf.format(new Date()) + "  " + workerId + "释放锁" + keyLock + "成功");
                    } else {
                        System.out.println(sdf.format(new Date()) + "  " + workerId + "释放锁" + keyLock + "失败");
                    }
                }
            }
        }
    }

}


