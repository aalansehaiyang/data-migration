package com.data.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DataUtils {

    // 开关
    public static boolean                switchOpen                    = false;

    // 源数据记录数
    public static AtomicInteger          sourceDataSize                = new AtomicInteger(0);

    // 已处理成功的记录条数
    public static AtomicInteger          handleSuccessSize             = new AtomicInteger(0);

    // 已处理失败的记录条数
    public static AtomicInteger          handleFailSize                = new AtomicInteger(0);

    // 任务队列
    public static BlockingQueue<Object>  taskQueue                     = new ArrayBlockingQueue<Object>(5);

    // 消费端线程执行器
    public static ThreadPoolTaskExecutor consumerExecutor              = null;

    // 消费端线程执行器队列长度上限
    public static int                    consumerCurrentQueueSizeLimit = 100;

    public static void setConsumerExecutor(ThreadPoolTaskExecutor consumerExecutor) {
        DataUtils.consumerExecutor = consumerExecutor;
        DataUtils.consumerExecutor.setMaxPoolSize(30);
        DataUtils.consumerExecutor.setCorePoolSize(30);
    }

    public static void setConsumerCurrentQueueSizeLimit(int consumerCurrentQueueSizeLimit) {
        DataUtils.consumerCurrentQueueSizeLimit = consumerCurrentQueueSizeLimit;
    }

    public static void setCustomerMaxThreadSize(int maxThreadSize) {
        DataUtils.consumerExecutor.setMaxPoolSize(maxThreadSize);
    }

    public static void setCustomerCorePoolSize(int coreThreadSize) {
        DataUtils.consumerExecutor.setCorePoolSize(coreThreadSize);
    }

    public static void resetRecordCount() {
        sourceDataSize = new AtomicInteger(0);
        handleSuccessSize = new AtomicInteger(0);
        handleFailSize = new AtomicInteger(0);

    }

}
