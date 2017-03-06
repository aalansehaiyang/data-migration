

#### 一、背景

之前在做一些老系统改造事情，需要将老库的数据清洗并迁移到新库里，由于数据量较大，每次开发都要重写一遍多线程，另外由于多线程开发有很多细节点需要注意，每次都要特别小心，一不小心，就可能搞出个bug。

所以在想是否可以平台化，将源数据的读取以及多线程的消费逻辑抽取出来，每次需求，只需关注具体的业务实现。

#### 二、实理原理

* 生产&消费模式

* 生产端持续不断的获取源数据，放入阻塞队列中，blockQueue设置了长度限制，如果消费端不能及时提取内容，会阻塞生产端，无法继续往里存入数据，直到有空闲空间

```
生产端结束标志：最后一次获取的数据为null，表示已经没有新的源数据
```

* 消费端会从blockQueue 提取源数据，来进行消费，当然大部分耗时逻辑是在具体处理细节中，取数据本身不会花太长时间，所以消费端也采用单线程制，取到源数据后，会扔给一个线程池执行器ThreadPoolTaskExecutor，由它来控制具体任务的执行。ThreadPoolTaskExecutor也有一个任务队列，为了防止队列过长，撑爆内存，所以会有上限limit控制，如果小于limit，消费线程才会往ThreadPoolTaskExecutor提交任务





#### 三、使用说明

项目为springboot工程，启动时直接运行com.data.Main

* 启动任务

http://localhost:8091/task/start?bizType=test_data

* 实时查看任务的运行状况

http://localhost:8091/task/process

```
{
  "customerMaxPoolSize": 30,
  "customerCorePoolSize": 30,
  "customerActiveCount": 0,
  "customerCurrentQueueSize": 0,
  "consumerCurrentQueueSizeLimit": 100,
  "blockQueueSize": 5,
  "sourceDataSize": 55558,
  "successCount": 55552,
  "failCount": 0
}
```

```
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

```

* 任务运行过程中，动态调整线程数及消费任务队列长度

http://localhost:8091/task/adjust?consumerCurrentQueueSizeLimit=100&maxThreadSize=30&coreThreadSize=30

* 如何接入新业务需求

可以参考com.data.service.impl.TestDataServiceImpl，实现相应的接口。

另外需要将实现类配置到xml文件的com.data.service.DataServiceDispatch的map映射里。

启动只要指定bizType为配置的key即可


#### 四、优势

```
a）  启动开关，避免重复启动
b）  约定胜于配置，采用队列缓存数据，多线程消费数据。开发只需要按规范实现数据获取接口及消费接口即可，无非关注线程处理细节
c）  增加业务类型，用于区分不同业务功能
d）  接口支持泛型，数据模型由开发者自己定义，保证灵活
e）  增加线程控制模块，任务运行过程中，可以随时动态调整核心线程数、最大线程数、任务队列上限
f）  任务进度实时查看。当前线程信息（正在执行任务的线程数、当前待处理任务数、中转队列大小等），源数据进度，成功记录条数，失败记录条数
g）支持数据源区间段读取，多台机器部署，提高处理速度
h） springboot工程，部署非常方便
```