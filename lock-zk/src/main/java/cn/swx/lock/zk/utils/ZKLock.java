package cn.swx.lock.zk.utils;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ZKLock implements Lock {

    private static final String IP_PORT = "127.0.0.1:2181";
    private static final String LOCK_NODE_ROOT = "/lock";
    private CountDownLatch cdl;


    private String LOCK_NODE;
    private volatile String currentPath;
    private volatile String beforePath;

    private ZkClient zkClient = new ZkClient(IP_PORT);

    public ZKLock(String businessLock) {
        LOCK_NODE = LOCK_NODE_ROOT + "/" + businessLock;
        if (!zkClient.exists(LOCK_NODE)) {
            zkClient.createPersistent(LOCK_NODE);
        }
    }

    @Override
    public synchronized void lock() {
        if (tryLock()) {
            System.out.println(Thread.currentThread().getName() + "获得锁：" + LOCK_NODE);
        } else {
            // 等待锁的释放
            waitForLock();
            // 再次尝试获得锁
            lock();
        }
    }

    private void waitForLock() {
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {
                System.out.println("--监听到数据改变事件--" + s);
            }

            @Override
            public void handleDataDeleted(String s) throws Exception {
                System.out.println(Thread.currentThread().getName() + "--监听到节点删除事件--" + s);
                cdl.countDown();
            }
        };
        // 监听
        this.zkClient.subscribeDataChanges(beforePath, listener);
        if (zkClient.exists(beforePath)) {
            try {
                cdl = new CountDownLatch(1);
                cdl.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 释放监听
        this.zkClient.subscribeDataChanges(beforePath, listener);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        if (StringUtils.isEmpty(currentPath)) {
            currentPath = zkClient.createEphemeralSequential(LOCK_NODE + "/", "lock");
        }
        List<String> children = zkClient.getChildren(LOCK_NODE);
        Collections.sort(children);
        if (currentPath.equals(LOCK_NODE + "/" + children.get(0))) {
            // 当前就是最小节点,表示拿到锁
            return true;
        } else {
            // 当前不是最小节点，设置前面一个节点的路径
            String seq = currentPath.substring(LOCK_NODE.length() + 1);
            int pos = Collections.binarySearch(children, seq);
            beforePath = LOCK_NODE + "/" + children.get(pos - 1);
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        System.out.println(Thread.currentThread().getName() + "释放锁：" + LOCK_NODE);
        zkClient.delete(currentPath);
    }

    @Override
    public Condition newCondition() {
        return null;
    }

}
