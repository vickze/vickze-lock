package io.vickze.lock;

import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author vick.zeng
 * @email zyk@yk95.top
 * @date 2018-01-08 17:52
 */
public class RedissonLockTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private RedissonClient redisson;

    private int stock = 500;

    private final int threads = 510;

    private int i = 0;

    private int j = 0;

    private int k = 0;

    private int l = 0;

    private CountDownLatch countDownLatch = new CountDownLatch(threads);

    private CyclicBarrier cyclicBarrier = new CyclicBarrier(threads);

    @Before
    public void before() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379");

        redisson = Redisson.create(config);
    }

    @Test
    public void redissonLockTest() throws InterruptedException {
        // 多线程测试
        for (int n = 0; n < threads; n++) {
            new Thread(() -> {
                RLock lock = redisson.getLock("lock:test");
                long startTime = System.currentTimeMillis();
                try {
                    //等到全部线程准备好才开始执行，模拟并发
                    cyclicBarrier.await();
                    //尝试加锁，最多等待10秒，上锁以后30秒自动解锁
                    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
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
                    logger.debug("花费：{}ms", System.currentTimeMillis() - startTime);
                    lock.unlock();
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
