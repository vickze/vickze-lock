package io.vickze.lock;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;

/**
 * @author vick.zeng
 * @email zengyukang@hey900.com
 * @date 2018-01-08 16:44
 */
public class ZookeeperLockTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int stock = 500;

    private final int threads = 510;

    private volatile int i = 0;

    private volatile int j = 0;

    private volatile int k = 0;

    private volatile int l = 0;

    private CountDownLatch countDownLatch = new CountDownLatch(threads);

    @Test
    public void zookeeperLockTest() throws InterruptedException {
        // 多线程测试
        for (int n = 0; n < threads; n++) {
            new Thread(() -> {
                Lock zookeeperLock = new ZookeeperLock("127.0.0.1:2181", 3000, "lock", "test");

                long startTime = System.currentTimeMillis();
                try {
                    if (zookeeperLock.tryLock()) {
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
                    zookeeperLock.unlock();
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
