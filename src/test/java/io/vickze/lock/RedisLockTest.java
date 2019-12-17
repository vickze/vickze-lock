package io.vickze.lock;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

/**
 * @author vick.zeng
 * @email zyk@yk95.top
 * @date 2018-01-03 18:01
 */
public class RedisLockTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private StringRedisTemplate jedisStringTemplate;

    private StringRedisTemplate lettuceStringTemplate;

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
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName("127.0.0.1");
        redisStandaloneConfiguration.setPort(6379);

        //使用RedisLock测试了100、200、300、400、500的线程并发，在RedisTemplate使用Jedis会比使用原生Jedis慢很多，使用Lettuce相对而言就要快很多，但依旧比原生的Jedis要慢一些
        //个人看法在SpringBoot项目上使用RedisTemplate会好一些，现在SpringBoot2.0默认也是使用Lettuce的
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder().build();
        jedisClientConfiguration.getPoolConfig().get().setMaxTotal(1000);
        jedisClientConfiguration.getPoolConfig().get().setMinIdle(500);
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);
        jedisConnectionFactory.afterPropertiesSet();

        jedisStringTemplate = new StringRedisTemplate(jedisConnectionFactory);


        LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder().build();
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, lettuceClientConfiguration);
        lettuceConnectionFactory.afterPropertiesSet();

        lettuceStringTemplate = new StringRedisTemplate(lettuceConnectionFactory);
    }
    
    @Test
    public void redisLockJedisTest() throws InterruptedException {
        // 多线程测试
        for (int n = 0; n < threads; n++) {
            new Thread(() -> {
                Lock redisLock = new RedisLock(jedisStringTemplate, "lock", "test");
                long startTime = System.currentTimeMillis();
                try {
                    //等到全部线程准备好才开始执行，模拟并发
                    cyclicBarrier.await();
                    //尝试加锁，最多等待10秒，默认最多30秒后解锁
                    if (redisLock.tryLock(10, TimeUnit.SECONDS)) {
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

    @Test
    public void redisLockTestLettuce() throws InterruptedException {
        // 多线程测试
        for (int n = 0; n < threads; n++) {
            new Thread(() -> {
                Lock redisLock = new RedisLock(lettuceStringTemplate, "lock", "test");
                long startTime = System.currentTimeMillis();
                try {
                    //等到全部线程准备好才开始执行，模拟并发
                    cyclicBarrier.await();
                    //尝试加锁，最多等待10秒，默认最多30秒后解锁
                    if (redisLock.tryLock(10, TimeUnit.SECONDS)) {
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
