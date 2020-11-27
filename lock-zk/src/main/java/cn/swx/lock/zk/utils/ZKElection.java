package cn.swx.lock.zk.utils;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.UUID;


public class ZKElection {

    private static final String IP_PORT = "127.0.0.1:2181";
    private static final String LOCK_NODE_ELECTION = "/election";
    private static final String LOCK_NODE_ELECTION_WORK = LOCK_NODE_ELECTION + "/work";


    private ZkClient zkClient = new ZkClient(IP_PORT);
    private String id = UUID.randomUUID().toString();
    IZkDataListener listener;

    public ZKElection() {
        if (!zkClient.exists(LOCK_NODE_ELECTION)) {
            zkClient.createPersistent(LOCK_NODE_ELECTION);
        }
        listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {
                System.out.println(o + " 为新的主节点");
            }

            @Override
            public void handleDataDeleted(String s) throws Exception {
                System.out.println("检测到原节点下线");

            }
        };
    }

    public boolean acquireMaster() {


        zkClient.subscribeDataChanges(LOCK_NODE_ELECTION_WORK, listener);
        if (!zkClient.exists(LOCK_NODE_ELECTION_WORK)) {
            try {
                zkClient.createEphemeral(LOCK_NODE_ELECTION_WORK, id);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return false;
            }
            return true;
        }
        return false;
    }


    public static void main(String[] args) throws InterruptedException {
        ZKElection zkElection = new ZKElection();
        zkElection.acquireMaster();
        Thread.sleep(100000000);
    }


}
