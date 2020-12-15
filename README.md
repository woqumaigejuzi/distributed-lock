# 分布式锁

#### 1. 分布式锁概述

###### 1.1 为何需要分布式锁

- a. **避免不同节点重复相同的工作**
- b. **避免破坏数据的正确性**

###### 1.2 锁的本质

​	**同一时间，对指定的对象，只允许一个操作**

###### 1.3 常见的实现方式

- a. 基于数据库实现
  - 乐观锁，基于version
  - 悲观锁，基于数据库级别的`for update`关键字
- b. 基于Zookeeper实现
  - 使用临时有序节点  `create -e -s /lock/seq/ data`
- c. 基于Redis实现
  - jedis，基于setnx,expire
  - redission





#### 2. 基于数据库实现

######  2.1 一个简单的实现

​	创建一张锁表，然后通过操作该表中的数据来实现。当我们想要获得锁的时候，就可以在该表中增加一条记录，想要释放锁的时候就删除这条记录。

*例：*

​	创建用于锁的表lock：

```sql
CREATE TABLE `database_lock` (
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`resource` int NOT NULL COMMENT '锁定的资源',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uiq_idx_resource` (`resource`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库分布式锁表';
```

​	**上锁**，可以插入一条数据。如果有多个请求同时提交到数据库的话，数据库可以保证只有一个操作可以成功，其它的会报错。我们就可以认为操作成功的那个请求获得了锁。

```sql
INSERT INTO database_lock(resource, description) VALUES (1, 'lock');
```

​	**解锁**，可以删除这条数据：

```sql
DELETE FROM database_lock WHERE resource=1;
```

​	

###### 2.2 乐观锁

​	系统认为数据的更新在大多数情况下是不会产生冲突的，只在数据库更新操作提交的时候才对数据作冲突检测。如果检测的结果出现了与预期数据不一致的情况，则返回失败信息

​	基于version实现，即为数据增加一个版本标识，在基于数据库表的版本解决方案中，一般是通过为数据库表添加一个 “version”字段来实现读取出数据时，将此版本号一同读出，之后更新时，对此版本号加1。在更新过程中，会对版本号进行比较，如果是一致的，没有发生改变，则会成功执行本次操作；如果版本号不一致，则会更新失败。

*例*

​	创建用于乐观锁的表，并预置数据：

```sql
CREATE TABLE `optimistic_lock` (
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`resource` int NOT NULL COMMENT '锁定的资源',
	`version` int NOT NULL COMMENT '版本信息',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uiq_idx_resource` (`resource`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库分布式锁表';

# 一般在使用乐观锁会确保表中有相应的数据
INSERT INTO optimistic_lock(resource, version) VALUES('lock', 1);
```

​	step1:  获取资源

```sql
SELECT resource, version FROM optimistic_lock WHERE resource = 'lock';
```

​	step2：执行业务逻辑，记录此时的版本 version=oldVersion

​	step3:   执行更新操作。如果改记录更新成功，则认为业务正常；否则表示在此其它，有其它线程获得过该把锁，需要，step2中的业务执行无效，需要从step1再次尝试。

```sql
UPDATE optimistic_lock SET resource = 'lock', version = version + 1 WHERE id = 1 AND version = oldVersion
```



###### 2.3 悲观锁

​	与乐观锁相反，总是假设最坏的情况，它认为数据的更新在大多数情况下是会产生冲突的。

​	基于数据库级别的`for update`关键字。即在查询语句后面增加FOR UPDATE，数据库会在查询过程中给数据库表增加悲观锁，也称排他锁。当某条记录被加上悲观锁之后，其它线程也就无法再改行上增加悲观锁。

*例*

​	step0：在使用悲观锁时，我们必须关闭数据库的自动提交属性

```sql
SET AUTOCOMMIT = 0;
```

​	step1: 明确地指定主键，获取锁

```sql
SELECT * FROM database_lock WHERE id = 1 FOR UPDATE;
```

​	step2: 执行业务逻辑

​	step3：释放锁

```
commit;
```

如果线程B在线程A释放锁之前执行step1,那么它会被阻塞，直至线程A释放锁之后才能继续。如果线程A长时间未释放锁，那么线程B会报错，参考如下（lock wait time可以通过innodb_lock_wait_timeout来进行配置）：



参考：[基于MySQL实现的分布式锁](https://blog.csdn.net/u013474436/article/details/104924782/)





#### 3. 基于Zookeeper实现

###### 3.1 Zookeeper 概述

​	Zookeeper 是一个开源的分布式服务协调组件，主要是用来解决分布式应用中遇到的一些数据管理问题如：`统一命名服务`、`状态同步服务`、`集群管理`、`分布式应用配置项的管理`等。

- 结构特征：

  - 类似Linux文件系统一样的数据结构。每一个节点对应一个Znode节点，每一个Znode节点都可以存储1MB的数据。
  
  ```bash
  [zk: localhost:2181(CONNECTED) 0] ls /
  [army, zookeeper]
  [zk: localhost:2181(CONNECTED) 1] ls /army
  [land, air, sea]
  ```
  
  ![](./pic/zk-arch.png)
  
  
  
- 节点Znode特征：

  - 包含节点数据，修改访问时间，操作事务Id，ACL控制权限等。

  ```bash
  [zk: localhost:2181(CONNECTED) 2] get /army
  100
  cZxid = 0x6
  ctime = Tue Feb 01 03:14:43 CST 2005
  mZxid = 0x6
  mtime = Tue Feb 01 03:14:43 CST 2005
  pZxid = 0xa
  cversion = 3
  dataVersion = 0
  aclVersion = 0
  ephemeralOwner = 0x0
  dataLength = 3
  numChildren = 3
  ```

  - 节点可以根据生命周期和类型分为4中节点。

    - 生命周期：当客户端结束会话的时候，是否清理掉该节点。(参数 -e)

      >持久性的节点(ephemeralOwner = 0x0)，客户端会话结束的时候，节点依然存在；
      >
      >非持久性的节点，客户端会话结束的时候，节点随之被清理。
    >
      >​     create -e  /lock data

    - 类型：是否顺序编号。（参数 -s）
    
      >正常节点：sea，land，air
      >
      >顺序编号节点: dog000000001， dog000000002 ...
      >
      >​	create  -s  /dog  data

- 监听机制：

  ​	任何session（session1, session2）都可以对自己感兴趣的znode进行监听，让znode被修改过时，session1和session2 都会受到znode的变更实践通知。

  常见的的事件监听有：

  - 节点数据变化监听：

    ```java
    // 节点数据变化监听器 (不监听子节点)
    IZkDataListener listener = new IZkDataListener() {
        @Override
        public void handleDataChange(String path, Object o) throws Exception {
        // 1.当前节点的数据改变时触发 2.新增当前节点也触发
    }
    
        @Override
        public void handleDataDeleted(String path) throws Exception {
        // 当前节点被删除时触发
        }
    };
    ```

    

  - 子节点监听：

    ```java
    IZkChildListener listener2 = new IZkChildListener() {
      @Override
      public void handleChildChange(String path, List<String> list) throws Exception {
        // 1. 当前节点的有子节点 增加/删除时触发   2. 新增当前节点时也触发
      }
    };
    ```

    

  - 服务连接状态监听：

    ```java
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
    ```

    

###### 3.2 Zookeeper 分布式锁

基于**临时有序节**点的特征和事件状态监听机制，实现基于zk分布式锁的大致步骤如下：

- a. 当前client向server写入临时有序节点

  ```java
  // 当前节点 
  private volatile String currentPath;
  // 写入当前client的临时有序节点
  currentPath = zkClient.createEphemeralSequential(LOCK_NODE + "/", "lock");
  ```

- b. 获取对应锁下的所有节点：

  ```java
  // 获取LOCK_NODE 下所有的节点，并升序排序
  List<String> children = zkClient.getChildren(LOCK_NODE);
  Collections.sort(children);
  ```
  
- c.  如果当前client写入节点`node000000000N`就是目前列表中的最小节点，则代表获取到了锁；否则表示没有获取到锁。

  ```java
  //当前节点的前面一个节点
  private volatile String beforePath;
  if (currentPath.equals(LOCK_NODE + "/" + children.get(0))) {
  	// 当前就是最小节点,表示拿到锁
  } else {
  	// 当前不是最小节点，表示没有拿到锁，这是需要设置前面一个节点的路径
      String seq = currentPath.substring(LOCK_NODE.length() + 1);
      int pos = Collections.binarySearch(children, seq);
      beforePath = LOCK_NODE + "/" + children.get(pos - 1);
  }
  ```

- d.

  -  在c步骤中，如果获取到锁，则可以进行相应的业务处理，完成之后，可直接删除当前节点（表示释放锁）

    ```java
    zkClient.delete(currentPath);
    ```

  - 在c步骤中，如果没有获取到锁，就监听比它小的节点`node000000000(N-1)`的删除事件（`handleDataDeleted`），如果触发，就从b步骤重新开始。

    ```java
    IZkDataListener listener = new IZkDataListener() {
        @Override
        public void handleDataChange(String s, Object o) throws Exception {}
        @Override
        public void handleDataDeleted(String s) throws Exception {
            // 回到步骤b
        };
    }
    // 监听
    this.zkClient.subscribeDataChanges(beforePath, listener);
    ```

链式监听：

![](./pic/zk-es.png)



事件通知：
![](./pic/zk-lock.png)





#### 4. 基于Redis实现

###### 4.1 通过jedis 的 setnx 和 getset 实现



###### 4.2 通过jedis高版本的原子命令

###### 4.3 通过redission



