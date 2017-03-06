package com.data.service;

/**
 * 数据服务接口（某业务数据迁移时只需按标准实现具体逻辑）
 * 
 * @author onlyone
 */
public interface DataService<T> {

    /**
     * <pre>
     * 数据来源
     * <p>如果返回结果为null，表示生产任务结束
     * 
     * </pre>
     */
    public T querySourceData();

    /**
     * <pre>
     * 重置数据源起始标记
     * 在任务跑完会后会执行，便于下次重新启动任务
     * </pre>
     */
    public void resetSign();

    /**
     * <pre>
     * 封装对源数据的处理逻辑
     * </pre>
     */
    public void migrationData(T object);

}
