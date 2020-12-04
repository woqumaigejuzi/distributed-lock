package redission;

import org.redisson.api.RLock;
import utils.redission.MyRedissonLock;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RedissionTest {
    static ThreadPoolExecutor pool = new ThreadPoolExecutor(
            10, 20, 10 * 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10)
    );
    static String keyLock = "lock";

    public static void main(String[] args) {
        RLock rLock1 = new MyRedissonLock(keyLock).getRLock();
        RLock rLock2 = new MyRedissonLock(keyLock).getRLock();
        List<RLock> rLocks = Arrays.asList(rLock1, rLock2);

        for (int i = 0; i < 4; i++) {
            pool.submit(new Worker(rLocks.get(i % rLocks.size())));
        }
        pool.shutdown();
    }

    static class Worker implements Runnable {
        static AtomicInteger gen = new AtomicInteger();
        private Integer workerId = gen.incrementAndGet();
        private RLock lock;
        private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        public Worker(RLock lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            boolean b = false;
            try {
                System.out.println(sdf.format(new Date()) + "  " + workerId + "尝试获得锁");
                b = lock.tryLock(10 * 1000, TimeUnit.MILLISECONDS);
                if (b) {
                    System.out.println(sdf.format(new Date()) + "  " + workerId + "获得业务锁" + keyLock + "业务开始");
                    Thread.sleep(6 * 1000);
                    System.out.println(sdf.format(new Date()) + "  " + workerId + "业务结束");
                } else {
                    System.out.println(sdf.format(new Date()) + "  " + workerId + "获取锁" + keyLock + "失败");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (b) {
                    lock.unlock();
                }
            }
        }
    }

}
