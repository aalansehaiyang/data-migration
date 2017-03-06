package com.data.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.data.service.BusinessControlManager;
import com.data.utils.DataUtils;
import com.data.vo.DataContextParam;
import com.data.vo.ProcessResult;

/**
 * @author onlyone
 */
@RestController
public class TaskController {

    private final Logger           logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private BusinessControlManager businessControlManager;

    /**
     * <pre>
     * 任务启动
     * url:http://localhost:8091/task/start?bizType=test
     * </pre>
     */
    @RequestMapping(value = "/task/start")
    public String startTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            if (DataUtils.switchOpen == true) {
                return "任务已启动，无需重复启动！";
            } else {
                DataUtils.switchOpen = true;
                DataUtils.resetRecordCount();
            }

            DataContextParam dataContextParam = new DataContextParam();
            String bizType = request.getParameter("bizType");
            dataContextParam.setBizType(bizType);
            businessControlManager.doHandle(dataContextParam);
        } catch (Exception e) {
            logger.error("[TaskController.startTask] error!", e);
            return "任务启动失败";
        }
        return "任务启动成功";
    }

    /**
     * <pre>
     * 系统参数调整
     * url:http://localhost:8091/task/adjust?consumerCurrentQueueSizeLimit=17&maxThreadSize=12&coreThreadSize=12
     * </pre>
     */
    @RequestMapping(value = "/task/adjust")
    public Object paramAdjust(HttpServletRequest request, HttpServletResponse response) throws Exception {
        DataUtils.setConsumerCurrentQueueSizeLimit(Integer.valueOf(request.getParameter("consumerCurrentQueueSizeLimit")));

        int coreThreadSize = Integer.valueOf(request.getParameter("coreThreadSize"));
        int maxThreadSize = Integer.valueOf(request.getParameter("maxThreadSize"));
        // 注意：核心线程数不能大于最大线程数，否则线程会不断创建、销毁，浪费系统资源
        if (coreThreadSize > maxThreadSize) {
            coreThreadSize = maxThreadSize;
        }
        DataUtils.setCustomerCorePoolSize(coreThreadSize);
        DataUtils.setCustomerMaxThreadSize(maxThreadSize);
        return "系统参数调整成功";

    }

    /**
     * <pre>
     * 任务处理进度
     * url:http://localhost:8091/task/process
     * </pre>
     */
    @RequestMapping(value = "/task/process")
    public ProcessResult processResult(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ProcessResult processResult = new ProcessResult();
        // 业务信息
        processResult.setSourceDataSize(DataUtils.sourceDataSize.longValue());
        processResult.setSuccessCount(DataUtils.handleSuccessSize.longValue());
        processResult.setFailCount(DataUtils.handleFailSize.longValue());

        // 系统信息
        processResult.setCustomerMaxPoolSize(DataUtils.consumerExecutor.getMaxPoolSize());
        processResult.setCustomerCorePoolSize(DataUtils.consumerExecutor.getCorePoolSize());
        processResult.setCustomerActiveCount(DataUtils.consumerExecutor.getActiveCount());
        processResult.setCustomerCurrentQueueSize(DataUtils.consumerExecutor.getCurrentQueueSize());
        processResult.setBlockQueueSize(DataUtils.taskQueue.size());
        processResult.setConsumerCurrentQueueSizeLimit(DataUtils.consumerCurrentQueueSizeLimit);

        return processResult;
    }

}
