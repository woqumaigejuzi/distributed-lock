import org.I0Itec.zkclient.ZkClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ZKLock implements Lock {

    private static final String IP_PORT = "127.0.0.1:2181";
    private static final String LOCK_NODE_ROOT = "/LOCK";
    private static final String LOCK_NODE = LOCK_NODE_ROOT + "/VN";


    private volatile String currentPath;
    private volatile String beforePath;

    private ZkClient zkClient = new ZkClient(IP_PORT);


    public ZKLock() {
        if (zkClient.exists(LOCK_NODE)) {
            zkClient.createPersistent(LOCK_NODE);
        }
    }


    @Override
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
        return false;
    }

    @Override
    public void unlock() {

    }

    @Override
    public Condition newCondition() {
        return null;
    }

}
