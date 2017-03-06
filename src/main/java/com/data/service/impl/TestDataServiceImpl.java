package com.data.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.data.service.DataService;
import com.data.utils.DataUtils;

/**
 * 测试demo
 * 
 * @author onlyone
 */
public class TestDataServiceImpl implements DataService<String> {

    private final Logger logger        = LoggerFactory.getLogger(TestDataServiceImpl.class);

    private Long         providerCount = 100000L;

    @Override
    public String querySourceData() {
        if (providerCount > 0) {
            String result = "number:" + providerCount;
            providerCount--;
            DataUtils.sourceDataSize.addAndGet(1);
            return result;
        }

        return null;
    }

    @Override
    public void migrationData(String object) {

        logger.error("comsumer====" + object);
        DataUtils.handleSuccessSize.incrementAndGet();

    }

    @Override
    public void resetSign() {

    }

}
