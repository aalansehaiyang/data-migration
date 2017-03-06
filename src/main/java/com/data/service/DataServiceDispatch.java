package com.data.service;

import java.util.Map;

/**
 * 数据服务适配器
 * 
 * @author onlyone
 */
public class DataServiceDispatch {

    private Map<String, DataService> dataServiceMap;

    public void setDataServiceMap(Map<String, DataService> dataServiceMap) {
        this.dataServiceMap = dataServiceMap;
    }

    // 根据业务类型查询具体的DataService
    public DataService getDateService(String bizType) {
        return dataServiceMap.get(bizType);
    }

}
