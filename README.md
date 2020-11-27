# Distributed-lock

#### 1. 分布式锁概述

###### 1.1 为何需要分布式锁

- a. **避免不同节点重复相同的工作**
- b. **避免破坏数据的正确性**

###### 1.2 锁的本质

​	**同一时间，只允许一个操作**

###### 1.3 常见的实现方式

- a. 基于数据库实现
  - 使用mysql自带的悲观锁`for update`关键字
  - 自己实现悲观锁/乐观锁
- b. 基于Zookeeper有序节点
  - `create -e -s /lock/seq/ data`
- c. 基于Redis
  - jedis
  - redission





#### 2. 基于数据库



#### 3. 基于Zookeeper



#### 4. 基于Redis

###### 4.1 通过jedis 的 setnx 和 getset 实现

###### 4.2 通过jedis高版本的原子命令

###### 4.3 通过redission



