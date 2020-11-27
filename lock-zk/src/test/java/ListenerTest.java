import cn.swx.lock.zk.utils.MyZkSerializer;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher;

import java.util.List;

public class ListenerTest {

    private static final String IP_PORT = "127.0.0.1:2181";
    private static final String LOCK_NODE_LISTENER = "/listener";


    private static ZkClient zkClient = new ZkClient(IP_PORT);


    public static void main(String[] args) throws InterruptedException {

        zkClient.setZkSerializer(new MyZkSerializer());

        // 节点数据变化监听器 (不监听子节点)
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String path, Object o) throws Exception {
                // 1.当前节点的数据改变时触发 2.新增当前节点也触发
                System.out.println(path + ":----handleDataChange-----:" + o);
            }

            @Override
            public void handleDataDeleted(String path) throws Exception {
                // 当前节点被删除时触发
                System.out.println(path + ":----handleDataDeleted-----:");
            }
        };

        // 子节点监听器
        IZkChildListener listener2 = new IZkChildListener() {
            @Override
            public void handleChildChange(String path, List<String> list) throws Exception {
                // 1. 当前节点的有子节点 增加/删除时触发   2. 新增当前节点时也触发
                System.out.println(path + ":----handleChildChange------:" + list);
            }
        };

        // zk服务端连接状态监控
        IZkStateListener listener3 = new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                System.out.println("----handleStateChanged------:" + state);
            }

            @Override
            public void handleNewSession() throws Exception {
                System.out.println("----handleNewSession------");
            }

            @Override
            public void handleSessionEstablishmentError(Throwable throwable) throws Exception {
                System.out.println("----handleSessionEstablishmentError------");
            }
        };

        zkClient.subscribeDataChanges(LOCK_NODE_LISTENER, listener);
        zkClient.subscribeChildChanges(LOCK_NODE_LISTENER, listener2);
        zkClient.subscribeStateChanges(listener3);


        Thread.sleep(1000000000);
    }

}


