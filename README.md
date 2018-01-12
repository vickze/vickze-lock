# Java实现Redis、Zookeeper分布式锁

    //Lock zookeeperLock = new ZookeeperLock("127.0.0.1:2181", 10000, "lock", "test");
    Lock lock = new RedisLock(shardedJedisPool, "lock", "test");

    //加锁，未获得锁抛出异常
    lock.lock();
    ...
    lock.unLock();

    //尝试加锁，未获得锁返回false
    if (lock.tryLock()) {
      ...
      lock.unLock();
    }

    //尝试加锁，最多等待10秒
    if (lock.tryLock(10, TimeUnit.SECONDS)) {
      ...
      lock.unLock();
    }
