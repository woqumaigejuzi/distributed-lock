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



#### 4. 基于Redis实现

###### 4.1 通过jedis 的 setnx 和 getset 实现



###### 4.2 通过jedis高版本的原子命令

###### 4.3 通过redission



