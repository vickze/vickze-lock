package io.vickze.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * @author vick.zeng
 * @email zyk@yk95.top
 * @date 2018-01-08 16:45
 */
public class CuratorLockTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int stock = 500;

    private final int threads = 1000;

    private int i = 0;

    private int j = 0;

    private int k = 0;

    private volatile int l = 0;

    private CountDownLatch countDownLatch = new CountDownLatch(threads);

    private CyclicBarrier cyclicBarrier = new CyclicBarrier(threads);

    private CuratorFramework client;

    private CuratorFrameworkFactory.Builder builder;

    @Before
    public void before() {

        client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", new ExponentialBackoffRetry(1000, 3));

        builder = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
                .retryPolicy(new ExponentialBackoffRetry(1000, 3));
        // etc. etc.
    }


    @Test
    public void curatorLockTest() throws InterruptedException {
        // 多线程测试
        for (int n = 0; n < threads; n++) {
            new Thread(() -> {
                CuratorFramework client = builder.build();
                client.start();
                InterProcessMutex lock = new InterProcessMutex(client, "/lock/test");
                long startTime = System.currentTimeMillis();
                try {
                    //等到全部线程准备好才开始执行，模拟并发
                    cyclicBarrier.await();
                    //尝试加锁，最多等待10秒
                    if (lock.acquire(10, TimeUnit.SECONDS)) {
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
                } catch (Exception e) {
                    incrementL();
                    logger.error(e.getMessage(), e);
                } finally {
                    try {
                        lock.release();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    logger.debug("花费：{}ms", System.currentTimeMillis() - startTime);
                    client.close();
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

    private void incrementI() {
        i++;
    }

    private void incrementJ() {
        j++;
    }

    private synchronized void incrementK() {
        k++;
    }

    private synchronized void incrementL() {
        l++;
    }
}
