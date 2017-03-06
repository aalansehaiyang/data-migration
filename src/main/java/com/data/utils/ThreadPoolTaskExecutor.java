package com.data.utils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

public class ThreadPoolTaskExecutor extends CustomizableThreadFactory implements ExecutorService, SchedulingTaskExecutor, Executor, BeanNameAware, InitializingBean, DisposableBean {

    protected final Logger           logger                           = LoggerFactory.getLogger(getClass());

    private final Object             poolSizeMonitor                  = new Object();

    private int                      corePoolSize                     = 1;

    private int                      maxPoolSize                      = Integer.MAX_VALUE;

    private int                      keepAliveSeconds                 = 60;

    private boolean                  allowCoreThreadTimeOut           = false;

    private int                      queueCapacity                    = Integer.MAX_VALUE;

    private ThreadFactory            threadFactory                    = this;

    // 替换为调用方执行的handler
    private RejectedExecutionHandler rejectedExecutionHandler         = new ThreadPoolExecutor.CallerRunsPolicy();

    private boolean                  waitForTasksToCompleteOnShutdown = false;

    private boolean                  threadNamePrefixSet              = false;

    private String                   beanName;

    private ThreadPoolExecutor       threadPoolExecutor;

    /**
     * Set the ThreadPoolExecutor's core pool size. Default is 1.
     * <p>
     * <b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setCorePoolSize(int corePoolSize) {
        synchronized (this.poolSizeMonitor) {
            this.corePoolSize = corePoolSize;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setCorePoolSize(corePoolSize);
            }
        }
    }

    /**
     * Return the ThreadPoolExecutor's core pool size.
     */
    public int getCorePoolSize() {
        synchronized (this.poolSizeMonitor) {
            return this.corePoolSize;
        }
    }

    /**
     * Set the ThreadPoolExecutor's maximum pool size. Default is <code>Integer.MAX_VALUE</code>.
     * <p>
     * <b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setMaxPoolSize(int maxPoolSize) {
        synchronized (this.poolSizeMonitor) {
            this.maxPoolSize = maxPoolSize;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
            }
        }
    }

    /**
     * Return the ThreadPoolExecutor's maximum pool size.
     */
    public int getMaxPoolSize() {
        synchronized (this.poolSizeMonitor) {
            return this.maxPoolSize;
        }
    }

    /**
     * Set the ThreadPoolExecutor's keep-alive seconds. Default is 60.
     * <p>
     * <b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setKeepAliveSeconds(int keepAliveSeconds) {
        synchronized (this.poolSizeMonitor) {
            this.keepAliveSeconds = keepAliveSeconds;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Return the ThreadPoolExecutor's keep-alive seconds.
     */
    public int getKeepAliveSeconds() {
        synchronized (this.poolSizeMonitor) {
            return this.keepAliveSeconds;
        }
    }

    /**
     * Specify whether to allow core threads to time out. This enables dynamic growing and shrinking even in combination
     * with a non-zero queue (since the max pool size will only grow once the queue is full).
     * <p>
     * Default is "false". Note that this feature is only available on Java 6 or above. On Java 5, consider switching to
     * the backport-concurrent version of ThreadPoolTaskExecutor which also supports this feature.
     * 
     * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
     */
    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    /**
     * Set the capacity for the ThreadPoolExecutor's BlockingQueue. Default is <code>Integer.MAX_VALUE</code>.
     * <p>
     * Any positive value will lead to a LinkedBlockingQueue instance; any other value will lead to a SynchronousQueue
     * instance.
     * 
     * @see java.util.concurrent.LinkedBlockingQueue
     * @see java.util.concurrent.SynchronousQueue
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * Set the ThreadFactory to use for the ThreadPoolExecutor's thread pool.
     * <p>
     * Default is this executor itself (i.e. the factory that this executor inherits from). See
     * {@link org.springframework.util.CustomizableThreadCreator}'s javadoc for available bean properties.
     * 
     * @see #setThreadPriority
     * @see #setDaemon
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = (threadFactory != null ? threadFactory : this);
    }

    /**
     * Set the RejectedExecutionHandler to use for the ThreadPoolExecutor. Default is the ThreadPoolExecutor's default
     * abort policy.
     * 
     * @see java.util.concurrent.ThreadPoolExecutor.AbortPolicy
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = (rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Set whether to wait for scheduled tasks to complete on shutdown.
     * <p>
     * Default is "false". Switch this to "true" if you prefer fully completed tasks at the expense of a longer shutdown
     * phase.
     * 
     * @see java.util.concurrent.ThreadPoolExecutor#shutdown()
     * @see java.util.concurrent.ThreadPoolExecutor#shutdownNow()
     */
    public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
    }

    @Override
    public void setThreadNamePrefix(String threadNamePrefix) {
        super.setThreadNamePrefix(threadNamePrefix);
        this.threadNamePrefixSet = true;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    /**
     * Calls <code>initialize()</code> after the container applied all property values.
     * 
     * @see #initialize()
     */
    @Override
    public void afterPropertiesSet() {
        initialize();
    }

    /**
     * Creates the BlockingQueue and the ThreadPoolExecutor.
     * 
     * @see #createQueue
     */
    @SuppressWarnings("unchecked")
    public void initialize() {
        if (logger.isInfoEnabled()) {
            logger.info("Initializing ThreadPoolExecutor" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
        }
        if (!this.threadNamePrefixSet && this.beanName != null) {
            setThreadNamePrefix(this.beanName + "-");
        }
        BlockingQueue queue = createQueue(this.queueCapacity);
        this.threadPoolExecutor = new ThreadPoolExecutor(this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds,
                                                         TimeUnit.SECONDS, queue, this.threadFactory,
                                                         this.rejectedExecutionHandler);
        if (this.allowCoreThreadTimeOut) {
            this.threadPoolExecutor.allowCoreThreadTimeOut(true);
        }
    }

    /**
     * Create the BlockingQueue to use for the ThreadPoolExecutor.
     * <p>
     * A LinkedBlockingQueue instance will be created for a positive capacity value; a SynchronousQueue else.
     * 
     * @param queueCapacity the specified queue capacity
     * @return the BlockingQueue instance
     * @see java.util.concurrent.LinkedBlockingQueue
     * @see java.util.concurrent.SynchronousQueue
     */
    @SuppressWarnings("unchecked")
    protected BlockingQueue createQueue(int queueCapacity) {
        if (queueCapacity > 0) {
            return new LinkedBlockingQueue(queueCapacity);
        } else {
            return new SynchronousQueue();
        }
    }

    /**
     * Return the underlying ThreadPoolExecutor for native access.
     * 
     * @return the underlying ThreadPoolExecutor (never <code>null</code>)
     * @throws IllegalStateException if the ThreadPoolTaskExecutor hasn't been initialized yet
     */
    public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
        Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
        return this.threadPoolExecutor;
    }

    /**
     * Implementation of both the JDK 1.5 Executor interface and the Spring TaskExecutor interface, delegating to the
     * ThreadPoolExecutor instance.
     * 
     * @see java.util.concurrent.Executor#execute(Runnable)
     * @see org.springframework.core.task.TaskExecutor#execute(Runnable)
     */
    @Override
    public void execute(Runnable task) {
        Executor executor = getThreadPoolExecutor();
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    /**
     * This task executor prefers short-lived work units.
     */
    @Override
    public boolean prefersShortLivedTasks() {
        return true;
    }

    /**
     * Return the current pool size.
     * 
     * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
     */
    public int getPoolSize() {
        return getThreadPoolExecutor().getPoolSize();
    }

    /**
     * Return the number of currently active threads.
     * 
     * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
     */
    public int getActiveCount() {
        return getThreadPoolExecutor().getActiveCount();
    }

    /**
     * Return this ThreadPool queue capacity
     */
    public int getQueueSize() {
        return queueCapacity;
    }

    /**
     * Return this ThreadPool queue current capacity
     */
    public int getCurrentQueueSize() {
        return getThreadPoolExecutor().getQueue().size();
    }

    /**
     * Calls <code>shutdown</code> when the BeanFactory destroys the task executor instance.
     * 
     * @see #shutdown()
     */
    @Override
    public void destroy() {
        shutdown();
    }

    /**
     * Perform a shutdown on the ThreadPoolExecutor.
     * 
     * @see java.util.concurrent.ThreadPoolExecutor#shutdown()
     */
    public void shutdown() {
        if (logger.isInfoEnabled()) {
            logger.info("Shutting down ThreadPoolExecutor" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
        }
        if (this.waitForTasksToCompleteOnShutdown) {
            this.threadPoolExecutor.shutdown();
        } else {
            this.threadPoolExecutor.shutdownNow();
        }
    }

    // ------------------下面的方法均是转掉用ThreadPoolExecutor-----------------------------//
    @Override
    public List<Runnable> shutdownNow() {
        return getThreadPoolExecutor().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return getThreadPoolExecutor().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return getThreadPoolExecutor().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return getThreadPoolExecutor().awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return getThreadPoolExecutor().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return getThreadPoolExecutor().submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return getThreadPoolExecutor().submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getThreadPoolExecutor().invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                                                                                                              throws InterruptedException {
        return getThreadPoolExecutor().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getThreadPoolExecutor().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                                                                                                throws InterruptedException,
                                                                                                ExecutionException,
                                                                                                TimeoutException {
        return getThreadPoolExecutor().invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        // TODO Auto-generated method stub

    }

}
