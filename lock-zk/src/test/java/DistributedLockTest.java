import cn.swx.lock.zk.utils.ZKLock;

public class DistributedLockTest {

    private static final int num = 5;
    private static final String LOCK_VN = "vn";

    /**
     * 模拟 num 个节点竞争 VN 锁
     */
    public static void main(String[] args) {
        for (int i = 0; i < num; i++) {
            new Thread(() -> {
                ZKLock zkLock = new ZKLock(LOCK_VN);
                try {
                    zkLock.lock();
                    // 模拟业务处理
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    zkLock.unlock();
                }
            }).start();
        }
    }


}
