package com.data.vo;

import lombok.Data;

/**
 * 数据处理进度
 * 
 * @author onlyone
 */
@Data
public class ProcessResult {

    // -------------系统信息---------
    // 最大线程数
    private Integer customerMaxPoolSize;

    // 核心线程数
    private Integer customerCorePoolSize;

    // 正在执行任务的线程数
    private Integer customerActiveCount;

    // 当前待处理的任务数
    private Integer customerCurrentQueueSize;

    // 消费端任务队列大小上限
    private Integer consumerCurrentQueueSizeLimit;

    // 中转阻塞队列大小
    private Integer blockQueueSize;

    // -------------业务信息---------
    // 源数据大小
    private Long    sourceDataSize;
    // 成功条数
    private Long    successCount;
    // 失败条数
    private Long    failCount;

}
