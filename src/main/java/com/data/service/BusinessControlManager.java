package com.data.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.data.utils.DataUtils;
import com.data.vo.DataContextParam;

/**
 * 核心业务控制器
 * 
 * @author only0one
 */
public class BusinessControlManager {

    private final Logger        logger = LoggerFactory.getLogger(BusinessControlManager.class);

    @Autowired
    private DataServiceDispatch dataServiceDispatch;

    public void doHandle(DataContextParam dataContextParam) {

        if (DataUtils.switchOpen == false) {
            return;
        }
        // 生产端
        new Thread(new Runnable() {

            public void run() {

                Object object = null;
                do {
                    // if (DataUtils.consumerExecutor.getCurrentQueueSize() > DataUtils.consumerCurrentQueueSizeLimit) {
                    // try {
                    // Thread.sleep(100);
                    // continue;
                    // } catch (InterruptedException e) {
                    // }
                    // }
                    object = dataServiceDispatch.getDateService(dataContextParam.getBizType()).querySourceData();
                    if (object != null) {
                        try {
                            DataUtils.taskQueue.put(object);
                        } catch (Exception e) {
                            logger.error("set queue error!", e);
                        }
                    }
                    // 任务结束，启动开关关闭
                    if (object == null) {
                        dataServiceDispatch.getDateService(dataContextParam.getBizType()).resetSign();
                        DataUtils.switchOpen = false;
                    }
                } while (object != null);

            }
        }).start();

        // 消费端
        new Thread(new Runnable() {

            @Override
            public void run() {
                Object object = null;
                while (true) {
                    try {
                        if (DataUtils.consumerExecutor.getCurrentQueueSize() > DataUtils.consumerCurrentQueueSizeLimit) {
                            try {
                                Thread.sleep(100);
                                continue;
                            } catch (InterruptedException e) {
                            }
                        }

                        object = DataUtils.taskQueue.take();

                    } catch (Exception e) {
                        logger.error("take queue error!", e);
                    }
                    DataUtils.consumerExecutor.execute(new ConsumerTask(object, dataContextParam));
                }

            }
        }).start();
    }

    // 消费任务
    private class ConsumerTask implements Runnable {

        private Object           object           = null;

        private DataContextParam dataContextParam = null;

        public ConsumerTask(Object object, DataContextParam dataContextParam){
            this.object = object;
            this.dataContextParam = dataContextParam;
        }

        @Override
        public void run() {
            dataServiceDispatch.getDateService(dataContextParam.getBizType()).migrationData(object);
        }
    }

}
