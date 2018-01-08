package io.vickze.lock;

import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import io.vickze.lock.RedisLock;
import io.vickze.lock.ZookeeperLock;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

/**
 * @author vick.zeng
 * @email zengyukang@hey900.com
 * @date 2018-01-03 18:01
 */
public class RedisLockTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ShardedJedisPool shardedJedisPool;

    private int stock = 500;

    private final int threads = 510;

    private volatile int i = 0;

    private volatile int j = 0;

    private volatile int k = 0;

    private volatile int l = 0;

    private CountDownLatch countDownLatch = new CountDownLatch(threads);

    @Before
    public void before() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        List<JedisShardInfo> jedisShardInfoList = new ArrayList<>();
        String[] hostList = {"127.0.0.1"};

        for (String host : hostList) {
            JedisShardInfo jedisShardInfo = new JedisShardInfo(host);
            //jedisShardInfo.setPassword(password);
            jedisShardInfoList.add(jedisShardInfo);
        }
        shardedJedisPool = new ShardedJedisPool(jedisPoolConfig, jedisShardInfoList);
    }

    @Test
    public void redisLockTest() throws InterruptedException {
        // 多线程测试
        for (int n = 0; n < threads; n++) {
            new Thread(() -> {
                Lock redisLock = new RedisLock(shardedJedisPool, "lock", "test");

                long startTime = System.currentTimeMillis();
                try {
                    if (redisLock.tryLock()) {
                        if (stock > 0) {
                            stock--;
                            incrementI();
                            logger.debug("剩余库存:{}", stock);
                        } else {
                            incrementJ();
                        }
                    } else {
                        incrementK();
                    }
                    logger.debug("花费：{}ms", System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    incrementL();
                    e.printStackTrace();
                } finally {
                    redisLock.unlock();
                    countDownLatch.countDown();
                }
            }).start();
        }

        // 主线程休眠，不然主线程结束子线程不会执行
        countDownLatch.await();

        logger.debug("已减库存 {}", i);
        logger.debug("没有更多库存了 {}", j);
        logger.debug("未能拿到锁 {}", k);
        logger.debug("获取锁异常 {}", l);

        if (i + j + k + l == threads) {
            logger.debug("成功锁住代码块");
        } else {
            logger.error("未能锁住代码块");
        }
    }

    private synchronized void incrementI() {
        i++;
    }

    private synchronized void incrementJ() {
        j++;
    }

    private synchronized void incrementK() {
        k++;
    }

    private synchronized void incrementL() {
        l++;
    }
}
