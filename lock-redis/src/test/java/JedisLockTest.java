import lombok.NoArgsConstructor;

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
        for (int i = 0; i < 2; i++) {
            pool.submit(new Worker(jedisLock));
        }

    }

    static class Worker implements Runnable {
        static AtomicInteger gen = new AtomicInteger();
        private Integer workerId = gen.incrementAndGet();
        JedisLock jedisLock;

        public Worker(JedisLock jedisLock) {
            this.jedisLock = jedisLock;
        }

        private void go(JedisLock jedisLock) {
            boolean locked = false;
            String seq = UUID.randomUUID().toString();
            try {
                System.out.println(workerId + "尝试获得锁,序号：" + seq);
                locked = jedisLock.tryLock(seq, 10 * 1000, TimeUnit.MILLISECONDS);
                if (locked) {
                    System.out.println(workerId + "获得业务锁" + keyLock + "业务开始");
                    Thread.sleep(12 * 1000);
                    System.out.println(workerId + "业务结束");
                } else {
                    System.out.println(workerId + "获取锁" + keyLock + "失败");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (locked) {
                    boolean unlock = jedisLock.unlock(seq);
                    if (unlock) {
                        System.out.println(workerId + "释放锁" + keyLock + "成功");
                    } else {
                        System.out.println(workerId + "释放锁" + keyLock + "失败");
                    }
                }
            }
        }

        @Override
        public void run() {
            go(jedisLock);
        }
    }

}


